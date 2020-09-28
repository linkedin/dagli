package com.linkedin.dagli.util.collection;

import com.linkedin.dagli.util.function.LongFunction1;
import it.unimi.dsi.fastutil.BigArrays;
import it.unimi.dsi.fastutil.Size64;
import it.unimi.dsi.fastutil.objects.ObjectBigArrays;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;


/**
 * An implementation of a Map that supports a virtually unlimited number of elements (other implementations are limited
 * to 2^31 elements or fewer).  A 64-bit hash function for the keys must be provided.
 */
public class BigHashMap<K, V> implements Map<K, V>, Size64, Serializable {
  private static final long serialVersionUID = 1;
  private static final long DEFAULT_CAPACITY = 16;
  private static final double DEFAULT_LOAD_FACTOR = 0.75;

  /*
   * This class is implemented as a BigArray of LinkedHashEntries (which comprise single-linked lists).  The obvious
   * alternative, an open addressing (e.g. linear probing) strategy, would certainly be faster if Java supported C-style
   * structs, but there is reason to assume that a linked strategy may be more performant in this particular context.
   * Regardless, actually benchmarking this is left to future work, and the asymptotic expected time for puts and gets
   * (of course) remains constant in either case.
   */

  /**
   * An entry in the hash table that also doubles as a {@link Map.Entry} implementation.  Entries in a given "bucket"
   * are stored in a singly-linked list.
   *
   * BigHashMap does not serialize these entries directly and thus they do not need to be (and are not) serializable.
   *
   * @param <K> the type of the key
   * @param <V> the type of the value
   */
  private static class LinkedHashEntry<K, V> implements Entry<K, V> {
    private final K _key;
    private final long _hash;

    private V _value;
    private LinkedHashEntry<K, V> _next; // null if this is the last entry in the table

    private LinkedHashEntry(K key, V value, long hash) {
      _key = key;
      _value = value;
      _hash = hash;
    }

    @Override
    public K getKey() {
      return _key;
    }

    @Override
    public V getValue() {
      return _value;
    }

    @Override
    public V setValue(V value) {
      V res = _value;
      _value = value;
      return res;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Entry)) {
        return false;
      }

      Entry<?, ?> other = (Entry<?, ?>) o;
      return Objects.equals(_key, other.getKey()) && Objects.equals(_value, other.getValue());
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(_key) ^ Objects.hashCode(_value);
    }

    private boolean hasKey(K key, long keyHash) {
      return (_hash == keyHash && Objects.equals(_key, key));
    }
  }

  private long _size; // >= 0
  private double _loadFactor; // > 0
  private transient long _nextRehashThreshold; // _capacity * loadFactor
  private transient long _hashMask; // _capacity - 1
  private transient long _capacity; // always a power of 2
  private transient LinkedHashEntry<K, V>[][] _table; // BigArray
  private final LongFunction1.Serializable<? super K> _hashFunction; // safely-serializable
  private final Class<K> _keyClass; // required because the Map interface thinks passing Objects as putative keys is
                                    // perfectly fine

  @SuppressWarnings("unchecked")
  private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
    in.defaultReadObject();
    setCapacity(nextPowerOf2((long) (_size / _loadFactor) + 1));
    for (long i = 0; i < _size; i++) {
      K key = (K) in.readObject();
      V val = (V) in.readObject();
      addEntry(new LinkedHashEntry<>(key, val, hash(key)));
    }
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    EntryIterator iterator = new EntryIterator();
    while (iterator.hasNext()) {
      Entry<K, V> entry = iterator.next();
      out.writeObject(entry.getKey());
      out.writeObject(entry.getValue());
    }
  }

  /**
   * Hashes a key to a 64-bit (long) hash value.
   *
   * This method simply invokes the hash function stored in the {@link #_hashFunction} field, unless the key is null in
   * which case the hash is canonically 0.
   *
   * @param key the key to hash
   * @return a 64-bit hash value (which should be well-distributed)
   */
  private long hash(K key) {
    return key == null ? 0 : _hashFunction.apply(key);
  }

  /**
   * Private, no-args constructor for the benefit of Kryo.
   */
  private BigHashMap() {
    _hashFunction = null;
    _keyClass = null;
  }

  /**
   * Creates a new map with the default initial capacity (16) and the default load factor (0.75).
   *
   * The provided {@code hashFunction} <strong>must</strong> be "safely-serializable" to allow for reliable
   * deserialization of the map.  Method references and function objects are safely-serializable; lambdas are not.
   * See {@link LongFunction1.Serializable#safelySerializable()} for more information.
   *
   * The hash function will <strong>not</strong> be invoked on {@code null} keys.  These will instead be assigned the
   * canonical hash value of 0.
   *
   * @param keyClass the class of the keys to be used (i.e. {@code K.class}); required because the Map interface allows
   *                 for attempted lookups of arbitrary (non-K) keys, which the hash function obviously can't hash, so
   *                 we need to use this provided class object to vet their type
   * @param hashFunction the safely-serializable hash function to use to hash (non-null) keys
   */
  public BigHashMap(Class<K> keyClass, LongFunction1.Serializable<? super K> hashFunction) {
    this(keyClass, hashFunction, DEFAULT_CAPACITY);
  }

  /**
   * Creates a new map with the default load factor (0.75).
   *
   * The provided {@code hashFunction} <strong>must</strong> be "safely-serializable" to allow for reliable
   * deserialization of the map.  Method references and function objects are safely-serializable; lambdas are not.
   * See {@link LongFunction1.Serializable#safelySerializable()} for more information.
   *
   * The hash function will <strong>not</strong> be invoked on {@code null} keys.  These will instead be assigned the
   * canonical hash value of 0.
   *
   * @param keyClass the class of the keys to be used (i.e. {@code K.class}); required because the Map interface allows
   *                 for attempted lookups of arbitrary (non-K) keys, which the hash function obviously can't hash, so
   *                 we need to use this provided class object to vet their type
   * @param hashFunction the safely-serializable hash function to use to hash (non-null) keys
   * @param initialCapacity the initial capacity of the hash table; {@code initialCapacity * loadFactor} entries may be
   *                        added before the hash table is resized/rehashed
   */
  public BigHashMap(Class<K> keyClass, LongFunction1.Serializable<? super K> hashFunction, long initialCapacity) {
    this(keyClass, hashFunction, initialCapacity, DEFAULT_LOAD_FACTOR);
  }

  /**
   * Creates a new map.
   *
   * The provided {@code hashFunction} <strong>must</strong> be "safely-serializable" to allow for reliable
   * deserialization of the map.  Method references and function objects are safely-serializable; lambdas are not.
   * See {@link LongFunction1.Serializable#safelySerializable()} for more information.
   *
   * The hash function will <strong>not</strong> be invoked on {@code null} keys.  These will instead be assigned the
   * canonical hash value of 0.
   *
   * @param keyClass the class of the keys to be used (i.e. {@code K.class}); required because the Map interface allows
   *                 for attempted lookups of arbitrary (non-K) keys, which the hash function obviously can't hash, so
   *                 we need to use this provided class object to vet their type
   * @param hashFunction the safely-serializable hash function to use to hash (non-null) keys
   * @param initialCapacity the initial capacity of the hash table; {@code initialCapacity * loadFactor} entries may be
   *                        added before the hash table is resized/rehashed
   * @param loadFactor the load factor of the map; this determines when the hash table's size increases as it grows;
   *                   larger loadFactors reduce the memory footprint of the map (albeit with very quickly diminishing
   *                   returns past ~1.0) at the cost of making hash table operations more expensive (by a constant
   *                   factor).
   */
  public BigHashMap(Class<K> keyClass, LongFunction1.Serializable<? super K> hashFunction, long initialCapacity,
      double loadFactor) {
    if (initialCapacity <= 0) {
      throw new IllegalArgumentException("Initial capacity must be at least 1");
    }
    if (!(loadFactor > 0)) { // express condition this way to catch NaNs
      throw new IllegalArgumentException("Load factor must be > 0");
    }

    _loadFactor = loadFactor;
    _hashFunction = Objects.requireNonNull(hashFunction).safelySerializable();
    _keyClass = keyClass;

    setCapacity(nextPowerOf2(initialCapacity));
  }

  /**
   * Sets the capacity and creates a new, empty, correspondingly-sized table
   *
   * @param newCapacity the new capacity
   */
  @SuppressWarnings("unchecked")
  private void setCapacity(long newCapacity) {
    _capacity = newCapacity;
    _table = ObjectBigArrays.newBigArray(new LinkedHashEntry[0][0], _capacity);
    _nextRehashThreshold = (long) (_capacity * _loadFactor);
    _hashMask = _capacity - 1;
  }

  /**
   * Adds an entry to the hashtable.  Note that the _next field of the entry is overwritten and does not need to be null
   * prior to calling this method.
   *
   * @param entry the entry to add to the table
   */
  private void addEntry(LinkedHashEntry<K, V> entry) {
    long index = entry._hash & _hashMask;
    entry._next = BigArrays.get(_table, index);
    BigArrays.set(_table, index, entry);
  }

  private void maybeGrow() {
    if (_size > _nextRehashThreshold) {
      LinkedHashEntry<K, V>[][] oldTable = _table; // store a reference to the existing table of entries
      setCapacity(_capacity << 1); // double to get next highest power of 2

      // copy existing entries into our new table
      for (LinkedHashEntry<K, V>[] subarray : oldTable) {
        for (LinkedHashEntry<K, V> entry : subarray) {
          // walk through the linked list, re-adding all the entries
          LinkedHashEntry<K, V> curEntry = entry;
          while (curEntry != null) {
            LinkedHashEntry<K, V> nextEntry = curEntry._next;
            addEntry(curEntry);
            curEntry = nextEntry;
          }
        }
      }
    }
  }

  /**
   * Gets the lowest power of 2 that greater than or equal to a provided long value.
   *
   * @param value the positive integer whose minimal greater-or-equal power of 2 is sought
   * @return the lowest power of 2 that greater than or equal to the provided value
   */
  private static long nextPowerOf2(long value) {
    return Long.highestOneBit((value << 1) - 1);
  }

  private LinkedHashEntry<K, V> findEntry(K key) {
    return findEntry(key, hash(key));
  }

  private LinkedHashEntry<K, V> findEntry(K key, long hash) {
    long index = hash & _hashMask;
    LinkedHashEntry<K, V> entry = BigArrays.get(_table, index);
    while (entry != null) {
      if (entry.hasKey(key, hash)) {
        return entry;
      }
      entry = entry._next;
    }

    return null;
  }

  @Override
  public long size64() {
    return _size;
  }

  @Override
  @SuppressWarnings("deprecation")
  public int size() {
    return Math.toIntExact(_size);
  }

  @Override
  public boolean isEmpty() {
    return _size == 0;
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean containsKey(Object key) {
    if (!_keyClass.isInstance(key)) {
      return false;
    }
    return findEntry((K) key) != null;
  }

  @Override
  public boolean containsValue(Object value) {
    EntryIterator iterator = new EntryIterator();
    while (iterator.hasNext()) {
      if (Objects.equals(iterator.next().getValue(), value)) {
        return true;
      }
    }

    return false;
  }

  @Override
  @SuppressWarnings("unchecked")
  public V get(Object key) {
    if (!_keyClass.isInstance(key)) {
      return null;
    }

    LinkedHashEntry<K, V> entry = findEntry((K) key);
    if (entry == null) {
      return null;
    }
    return entry.getValue();
  }

  /**
   * Gets the Map entry corresponding to the given key, or null if the key has no corresponding entry in the table.
   *
   * Calling containsKey() and then get() is a common pattern when null values are possible.  This method is much faster
   * (requiring only a single lookup) and, in the current implementation, is also (slightly) faster than either of
   * those methods by themselves.
   *
   * @param key the key whose entry is sought
   * @return the Map entry corresponding to the given key, or null if the key has no corresponding entry in the table
   */
  public Map.Entry<K, V> getEntry(K key) {
    return findEntry(key);
  }

  @Override
  public V put(K key, V value) {
    long hash = hash(key);
    LinkedHashEntry<K, V> existing = findEntry(key, hash);
    if (existing != null) {
      // updating existing entry
      V result = existing._value;
      existing._value = value;
      return result;
    } else {
      // new entry
      addEntry(new LinkedHashEntry<>(key, value, hash));
      _size++;
      maybeGrow();
      return null;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public V getOrDefault(Object key, V defaultValue) {
    if (!_keyClass.isInstance(key)) {
      return defaultValue;
    }

    Entry<K, V> entry = getEntry((K) key);
    return entry == null ? defaultValue : entry.getValue();
  }

  @Override
  public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
    long hash = hash(key);
    LinkedHashEntry<K, V> existing = findEntry(key, hash);
    if (existing != null) {
      if (existing._value == null) {
        // we overwrite null values, since a null value counts as "absent"
        existing._value = mappingFunction.apply(key); // if this value is a null it's a no-op, as required by spec
        return existing._value;
      }
      return existing._value;
    } else {
      // new entry
      V value = mappingFunction.apply(key);
      if (value != null) {
        addEntry(new LinkedHashEntry<>(key, value, hash));
        _size++;
        maybeGrow();
      }
      return value;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public V remove(Object key) {
    if (!_keyClass.isInstance(key)) {
      return null;
    }

    Entry<K, V> entry = removeEntry((K) key);
    return entry == null ? null : entry.getValue();
  }

  /**
   * Removes and returns the Map entry associated with a key (if present).
   *
   * @param key the key whose entry should be removed
   * @return the removed Map entry, or null if no entry with the specified key was present
   */
  public Map.Entry<K, V> removeEntry(K key) {
    return removeEntry(key, hash(key));
  }

  private Map.Entry<K, V> removeEntry(K key, long hash) {
    long index = hash & _hashMask;
    LinkedHashEntry<K, V> prev = BigArrays.get(_table, index);
    if (prev == null) {
      return null; // no linked list exists in this bucket
    } else if (prev.hasKey(key, hash)) {
      // sought entry is the first in the list: modify the table's pointer to point to the next entry in the linked list
      BigArrays.set(_table, index, prev._next);
      _size--;
      return prev;
    }

    // walk through list to search for match
    LinkedHashEntry<K, V> cur = prev._next;
    while (cur != null) {
      if (cur.hasKey(key, hash)) {
        prev._next = cur._next; // remove current entry from linked list
        _size--;
        return cur;
      }
      prev = cur;
      cur = cur._next;
    }

    return null; // no match found
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    m.forEach(this::put);
  }

  @Override
  public void clear() {
    BigArrays.fill(_table, null);
    _size = 0;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    EntryIterator iterator = new EntryIterator();
    while (iterator.hasNext()) {
      hash += iterator.next().hashCode();
    }
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Map)) {
      return false;
    }

    Map<?, ?> other = (Map<?, ?>) obj;
    long otherSize = obj instanceof Size64 ? ((Size64) obj).size64() : other.size();
    if (_size != otherSize) {
      return false;
    }

    return entrySet().containsAll(other.entrySet());
  }

  @Override
  public KeySet keySet() {
    return new KeySet();
  }

  /**
   * A set of {@link BigHashMap} keys, backed by (and tied to) the source map.
   */
  public class KeySet extends AbstractSet<K> implements Serializable, Size64 {
    private static final long serialVersionUID = 1;

    @Override
    public long size64() {
      return BigHashMap.this._size;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int size() {
      return BigHashMap.this.size();
    }

    @Override
    public boolean isEmpty() {
      return BigHashMap.this.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
      return containsKey(o);
    }

    @Override
    public Iterator<K> iterator() {
      return new Iterator<K>() {
        final EntryIterator _wrapped = new EntryIterator();
        @Override
        public boolean hasNext() {
          return _wrapped.hasNext();
        }

        @Override
        public K next() {
          return _wrapped.next().getKey();
        }

        @Override
        public void remove() {
          _wrapped.remove();
        }
      };
    }

    @Override
    public boolean add(K key) {
      long hash = hash(key);
      if (findEntry(key, hash) != null) {
        return false;
      }
      addEntry(new LinkedHashEntry<>(key, null, hash));
      _size++;
      maybeGrow();
      return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean remove(Object o) {
      if (!_keyClass.isInstance(o)) {
        return false;
      }
      return removeEntry((K) o) != null;
    }

    @Override
    public void clear() {
      BigHashMap.this.clear();
    }
  }

  @Override
  public ValuesCollection values() {
    return new ValuesCollection();
  }

  /**
   * A collection of {@link BigHashMap} values, backed by (and tied to) the source map.
   */
  public class ValuesCollection extends AbstractCollection<V> implements Serializable, Size64 {
    private static final long serialVersionUID = 1;

    @Override
    public long size64() {
      return _size;
    }

    @Override
    public Iterator<V> iterator() {
      return new Iterator<V>() {
        final EntryIterator _wrapped = new EntryIterator();

        @Override
        public boolean hasNext() {
          return _wrapped.hasNext();
        }

        @Override
        public V next() {
          return _wrapped.next().getValue();
        }

        @Override
        public void remove() {
          _wrapped.remove();
        }
      };
    }

    @Override
    @SuppressWarnings("deprecation")
    public int size() {
      return Math.toIntExact(_size);
    }
  }

  @Override
  public EntrySet entrySet() {
    return new EntrySet();
  }

  /**
   * A set of {@link BigHashMap} entries, backed by (and tied to) the source map.
   */
  public class EntrySet extends AbstractSet<Entry<K, V>> implements Serializable, Size64 {
    private static final long serialVersionUID = 1;

    @SuppressWarnings("unchecked")
    public Entry<K, V>[][] toBigArray() {
      Entry<K, V>[][] result = ObjectBigArrays.newBigArray(new Entry[0][0], _size);
      Iterator<Entry<K, V>> iterator = iterator();
      for (Entry<K, V>[] subarray : result) {
        for (int i = 0; i < subarray.length; i++) {
          subarray[i] = iterator.next();
        }
      }
      return result;
    }

    @Override
    public boolean isEmpty() {
      return BigHashMap.this.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
      if (!(o instanceof Map.Entry)) {
        return false;
      }

      Entry<?, ?> entry = (Map.Entry<?, ?>) o;
      if (!_keyClass.isInstance(entry.getKey())) {
        return false;
      }

      @SuppressWarnings("unchecked")
      Entry<K, V> mapped = BigHashMap.this.getEntry((K) entry.getKey());

      return mapped != null && Objects.equals(mapped.getValue(), entry.getValue());
    }

    @Override
    public boolean remove(Object o) {
      if (!(o instanceof Map.Entry)) {
        return false;
      }

      Entry<?, ?> entry = (Map.Entry<?, ?>) o;
      return BigHashMap.this.remove(entry.getKey(), entry.getValue());
    }

    @Override
    public void clear() {
      BigHashMap.this.clear();
    }

    @Override
    public long size64() {
      return BigHashMap.this.size64();
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
      return new EntryIterator();
    }

    @Override
    @SuppressWarnings("deprecation")
    public int size() {
      return BigHashMap.this.size();
    }
  }

  private class EntryIterator implements Iterator<Map.Entry<K, V>> {
    int _outerIndex = 0;
    int _innerIndex = 0;
    LinkedHashEntry<K, V> _nextEntry = _table[0][0];
    LinkedHashEntry<K, V> _lastEntry = null;

    @Override
    public boolean hasNext() {
      if (_nextEntry != null) {
        return true;
      }

      for (; _outerIndex < _table.length; _outerIndex++) {
        LinkedHashEntry<K, V>[] outer = _table[_outerIndex];
        for (++_innerIndex; _innerIndex < outer.length; _innerIndex++) {
          if (outer[_innerIndex] != null) {
            _nextEntry = outer[_innerIndex];
            return true;
          }
        }
      }

      return false;
    }

    @Override
    public Entry<K, V> next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      _lastEntry = _nextEntry;
      _nextEntry = _nextEntry._next;
      return _lastEntry;
    }

    @Override
    public void remove() {
      // we could make this faster by remembering the "last last" entry, but since this would only have benefit if the
      // linked list has a size > 1 (a minority of cases if the load factor is <= 1 as it typically is) we don't bother.
      if (_lastEntry == null || removeEntry(_lastEntry._key, _lastEntry._hash) == null) {
        throw new IllegalStateException("Attempted to remove() already-removed element, or before next() was called");
      }
    }
  }
}
