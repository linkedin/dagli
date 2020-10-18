package com.linkedin.dagli.util.collection;

import com.linkedin.dagli.util.function.Function1;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;


/**
 * A lazy map that will generate (and cache) the values for a given key on-the-fly as needed using a key-to-value
 * function.  Different values than those provided by the key-to-value function may also be explicitly set.
 *
 * This class is serializable so long as its key-to-value function is serializable.  No attempt is made by this class to
 * vet that the function is <i>safely-serializable</i> or even serializable; this can be done by the client by invoking
 * {@link Function1#safelySerializable(Function1.Serializable)}.
 *
 * The key-to-value function's result is cached so, unless a key is removed and then re-added to the map later, the
 * function will be called at most once for that key.  No guarantee is made regarding <i>when</i> that call will occur,
 * however.
 *
 * @param <K> the type of key in the map
 * @param <V> the type of values in the map
 */
public class LazyMap<K, V> extends AbstractMap<K, V> implements Serializable {
  private static final long serialVersionUID = 1;
  private static final int DEFAULT_CAPACITY = 8;

  private static class CacheEntry<V> implements Serializable {
    private static final long serialVersionUID = 1;

    boolean _isSet = false;
    V _value = null;

    <K> V getValue(K key, Function<? super K, ? extends V> keyToValueFunction) {
      if (!_isSet) {
        _isSet = true;
        _value = keyToValueFunction.apply(key);
      }
      return _value;
    }

    <K> V setValue(K key, V value, Function<? super K, ? extends V> keyToValueFunction) {
      V oldValue = getValue(key, keyToValueFunction);
      _value = value;
      return oldValue;
    }
  }

  private final HashMap<K, CacheEntry<V>> _map;
  private final Function<? super K, ? extends V> _keyToValueFunction;

  /**
   * Creates a new lazy map with a default initial capacity.
   *
   * @param keyToValueFunction the function that will lazily calculate the value for each key
   */
  public LazyMap(Function<? super K, ? extends V> keyToValueFunction) {
    this(DEFAULT_CAPACITY, keyToValueFunction);
  }

  /**
   * Adds a mapping from the key to a lazily-computed-in-the-future value if no mapping for the key presently exists.
   *
   * @param key the key to add
   * @return true if a mapping was added to the map or false if the key was already present
   */
  public boolean putIfAbsent(K key) {
    boolean[] result = new boolean[1];
    _map.computeIfAbsent(key, k -> {
      result[0] = true;
      return new CacheEntry<>();
    });
    return result[0];
  }


  /**
   * Creates a new lazy map with an initial set of keys that will (when the value for each key is read) be mapped to
   * their values using the specified {@code keyToValueFunction}.
   *
   * @param keyToValueFunction the function that will lazily calculate the value for each key
   * @param keys the keys to be added to this map; duplicate keys will be ignored
   */
  public LazyMap(Iterable<? extends K> keys, Function<? super K, ? extends V> keyToValueFunction) {
    this(Math.toIntExact(Iterables.size64(keys)), keyToValueFunction);

    // add all keys
    for (K key : keys) {
      putIfAbsent(key);
    }
  }

  /**
   * Creates a new lazy map with the specified initial capacity.
   *
   * @param keyToValueFunction the function that will lazily calculate the value for each key
   * @param initialCapacity the initial capacity of the map
   */
  public LazyMap(int initialCapacity, Function<? super K, ? extends V> keyToValueFunction) {
    _map = new HashMap<>(initialCapacity);
    _keyToValueFunction = keyToValueFunction;
  }

  private class EntrySet extends AbstractSet<Entry<K, V>> {
    @Override
    public Iterator<Entry<K, V>> iterator() {
      return _map.entrySet().stream().map(entry -> (Entry<K, V>) new Entry<K, V>() {
        @Override
        public K getKey() {
          return entry.getKey();
        }

        @Override
        public V getValue() {
          return entry.getValue().getValue(entry.getKey(), _keyToValueFunction);
        }

        @Override
        public V setValue(V value) {
          return entry.getValue().setValue(entry.getKey(), value, _keyToValueFunction);
        }
      }).iterator();
    }

    @Override
    public int size() {
      return _map.size();
    }

    @Override
    public boolean contains(Object o) {
      return _map.containsKey(o);
    }

    @Override
    public boolean remove(Object o) {
      return _map.remove(o) != null;
    }

    private class EntryIterator implements Iterator<Entry<K, V>> {
      @Override
      public boolean hasNext() {
        return false;
      }

      @Override
      public Entry<K, V> next() {
        return null;
      }
    }
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return new EntrySet();
  }

  @Override
  public boolean containsKey(Object key) {
    return _map.containsKey(key);
  }

  @Override
  @SuppressWarnings("unchecked")
  public V get(Object key) {
    CacheEntry<V> entry = _map.get(key);
    if (entry == null) {
      return null;
    }
    return entry.getValue((K) key, _keyToValueFunction);
  }

  /**
   * Gets the mapped value for a given key or, if the key is not present in the map, invokes the provided function to
   * calculate the default value.  This is similar to {@link #computeIfAbsent(Object, Function)} except that the
   * resulting default value is <strong>not</strong> entered into the map (and can be null).
   *
   * @param key the key whose value should be returned
   * @param defaultComputationFunction a function that accepts a key and returns a default value to us eif the key is
   *                                   not present in the map
   * @return the value mapped to the key or, if the key is not present, the default value returned by the provided
   *         function
   */
  public V getOrComputeDefault(K key, Function<? super K, ? extends V> defaultComputationFunction) {
    CacheEntry<V> entry = _map.get(key);
    return entry == null ? defaultComputationFunction.apply(key) : entry.getValue(key, _keyToValueFunction);
  }

  @Override
  @SuppressWarnings("unchecked")
  public V remove(Object key) {
    CacheEntry<V> entry =  _map.remove(key);
    if (entry == null) {
      return null;
    }
    return entry.getValue((K) key, _keyToValueFunction);
  }

  @Override
  public Set<K> keySet() {
    return _map.keySet();
  }

  @Override
  public V put(K key, V value) {
    return _map.computeIfAbsent(key, k -> new CacheEntry<>()).setValue(key, value, _keyToValueFunction);
  }
}
