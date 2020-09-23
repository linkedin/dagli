package com.linkedin.dagli.objectio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;


/**
 * A ObjectReader that provides all the elements contained in multiple other ObjectReaders.  Elements are enumerated
 * by looking at each ObjectReader in turn and retrieving the next element.  When a particular ObjectReader's elements
 * are exhausted, it is skipped and the remaining ObjectReaders' elements continue to be returned.
 *
 * So, for example, if we had two underlying ObjectReaders, each containing these elements:
 * [1, 2, 3, 4]
 * [A, B]
 *
 * The iterated order of elements would be 1, A, 2, B, 3, 4.
 *
 * @param <T> the type of element being enumerated
 */
public class MultiplexedReader<T> implements ObjectReader<T> {
  private final ObjectReader<T>[] _objectReaders;

  /**
   * Creates a new instance that will read items from the provided ObjectReaders.
   *
   * @param objectReaders the readers from which items will be read
   */
  @SafeVarargs
  public MultiplexedReader(ObjectReader<T>... objectReaders) {
    _objectReaders = objectReaders.clone();
  }

  @Override
  public void close() {
    for (ObjectReader<T> objectReader : _objectReaders) {
      objectReader.close();
    }
  }

  @Override
  public long size64() {
    long size = 0;
    for (ObjectReader<T> objectReader : _objectReaders) {
      size += objectReader.size64();
    }
    return size;
  }

  @Override
  public ObjectIterator<T> iterator() {
    return new Iterator<>(Arrays.stream(_objectReaders).map(ObjectReader::iterator).collect(Collectors.toList()));
  }

  /**
   * A ObjectIterator that provides all the elements contained in multiple other ObjectIterator.  Elements are
   * enumerated by looking at each constituent ObjectIterator in turn and retrieving the next element.  When a
   * particular ObjectIterator's elements are exhausted, it is skipped and the remaining ObjectIterators' elements
   * continue to be returned.
   *
   * So, for example, if we had two underlying ObjectIterators, each containing these elements:
   * [1, 2, 3, 4]
   * [A, B]
   *
   * The iterated order of elements would be 1, A, 2, B, 3, 4.
   *
   * @param <T> the type of element being enumerated
   */
  public static class Iterator<T> implements ObjectIterator<T> {
    private final List<ObjectIterator<? extends T>> _objectIterators;
    private int _nextIterator = 0;

    /**
     * Creates a new instance.
     *
     * @param objectIterators the object iterators to be wrapped and whose elements will be iterated by this instance in
     *                        a round-robin fashion.
     */
    public Iterator(List<ObjectIterator<? extends T>> objectIterators) {
      _objectIterators = objectIterators;
    }

    /**
     * Creates a new instance.
     *
     * @param objectIterators the object iterators to be wrapped and whose elements will be iterated by this instance in
     *                        a round-robin fashion.
     */
    @SafeVarargs
    public Iterator(ObjectIterator<? extends T>... objectIterators) {
      this(new ArrayList<>(Arrays.asList(objectIterators)));
    }

    @Override
    public boolean hasNext() {
      // remove any empty iterators
      while (!_objectIterators.isEmpty() && !_objectIterators.get(_nextIterator).hasNext()) {
        // eagerly close the underlying iterators
        _objectIterators.get(_nextIterator).close();
        _objectIterators.remove(_nextIterator);
        if (_nextIterator == _objectIterators.size()) {
          _nextIterator = 0;
        }
      }
      return !_objectIterators.isEmpty();
    }

    @Override
    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      T res = _objectIterators.get(_nextIterator).next();
      _nextIterator = (_nextIterator + 1) % _objectIterators.size();
      return res;
    }

    @Override
    public void close() {
      for (ObjectIterator<? extends T> oi : _objectIterators) {
        oi.close();
      }
    }
  }
}
