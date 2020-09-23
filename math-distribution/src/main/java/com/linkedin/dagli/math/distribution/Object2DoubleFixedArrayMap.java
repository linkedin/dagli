package com.linkedin.dagli.math.distribution;

import it.unimi.dsi.fastutil.doubles.DoubleCollection;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.Map;
import java.util.NoSuchElementException;


/**
 * This is a map of objects to doubles that stores its elements in arrays.  We use this class (rather than
 * {@link Object2DoubleArrayMap}) for improved efficiency.
 *
 * Unfortunately, the implementation of {@link Object2DoubleArrayMap} has an oversight that prevents setting the value
 * of elements as they're iterated (despite this being simple to accomplish).  It also does not expose the underlying
 * arrays used to store the element, which makes sense in part because, when items are added, the backing arrays may
 * change.
 *
 * This class is backed by {@link Object2DoubleArrayMap}, but we use containment, not inheritance; we do this to ensure
 * that no operation that might increase or decrease the size of the map is allowed (even if
 * {@link Object2DoubleArrayMap} changes in the future) and thus our array pointers and those held within the
 * {@link Object2DoubleArrayMap} remain the same.
 */
class Object2DoubleFixedArrayMap<T> implements Object2DoubleMap<T> {

  private Object2DoubleArrayMap<T> _arrayMap;

  // need our own pointers to these, since they are private in Object2DoubleArrayMap
  private T[] _keys;
  private double[] _values;

  /**
   * Creates a new instance with the provided keys and values.  The new map takes ownership of the provided arrays,
   * and these should not be further modified by the caller.
   *
   * @param keys the keys for the map
   * @param values a parallel array of values for the map, with the value at each position correspond to the key at the
   *               same array offset in the keys array
   */
  public Object2DoubleFixedArrayMap(T[] keys, double[] values) {
    _keys = keys;
    _values = values;
    _arrayMap = new Object2DoubleArrayMap<>(_keys, _values);
  }

  /**
   * Gets the underlying array of keys.  This array should not be modified.
   *
   * @return the underlying array of keys
   */
  public T[] getKeyArray() {
    return _keys;
  }

  /**
   * Gets the underlying array of values.  This array should not be modified.
   *
   * @return the underlying array of values
   */
  public double[] getValueArray() {
    return _values;
  }

  @Override
  public int size() {
    return _arrayMap.size();
  }

  @Override
  public boolean isEmpty() {
    return _arrayMap.isEmpty();
  }

  @Override
  public double getDouble(Object key) {
    return _arrayMap.getDouble(key);
  }

  @Override
  public void defaultReturnValue(double rv) {
    _arrayMap.defaultReturnValue(rv);
  }

  @Override
  public double defaultReturnValue() {
    return _arrayMap.defaultReturnValue();
  }

  @Override
  public ObjectSet<Entry<T>> object2DoubleEntrySet() {
    return new EntrySet();
  }

  @Override
  public ObjectSet<T> keySet() {
    return _arrayMap.keySet();
  }

  @Override
  public DoubleCollection values() {
    return _arrayMap.values();
  }

  @Override
  public boolean containsKey(Object key) {
    return _arrayMap.containsKey(key);
  }

  @Override
  public void putAll(Map<? extends T, ? extends Double> m) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsValue(double value) {
    return _arrayMap.containsValue(value);
  }

  /**
   * An entry that knows the index of its key and value within the encapsulated map instance
   */
  private final class ReferenceEntry implements Object2DoubleMap.Entry<T> {
    private int _index;

    /**
     * Create a new entry instance that will correspond to a particular key & value
     *
     * @param index the index of the entry to represent
     */
    ReferenceEntry(int index) {
      _index = index;
    }

    @Override
    public double getDoubleValue() {
      return _values[_index];
    }

    @Override
    public double setValue(double value) {
      double oldValue = _values[_index];
      _values[_index] = value;
      return oldValue;
    }

    @Override
    public T getKey() {
      return _keys[_index];
    }
  }

  /**
   * An implementation of an EntrySet that allows modification of the values of entries, but not the deletion of
   * entries.
   *
   * This code is partly based on the implementation of Object2DoubleArrayMap::EntrySet, licensed under
   * Apache 2.0 and copyright 2007-2017 Sebastiano Vigna.
   */
  private final class EntrySet extends AbstractObjectSet<Entry<T>> implements FastEntrySet<T> {
    @Override
    public ObjectIterator<Entry<T>> iterator() {
      return new ObjectIterator<Object2DoubleMap.Entry<T>>() {
        private int _next = 0;

        @Override
        public boolean hasNext() {
          return _next < _arrayMap.size();
        }

        @Override
        public Entry<T> next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          return new ReferenceEntry(_next++);
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }

    @Override
    public ObjectIterator<Object2DoubleMap.Entry<T>> fastIterator() {
      return new ObjectIterator<Object2DoubleMap.Entry<T>>() {
        private int _next = 0;
        private final ReferenceEntry _entry = new ReferenceEntry(0);

        @Override
        public boolean hasNext() {
          return _next < _arrayMap.size();
        }

        @Override
        public Entry<T> next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          _entry._index = _next++;
          return _entry;
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }

    @Override
    public int size() {
      return _arrayMap.size();
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }
  }
}
