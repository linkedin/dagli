package com.linkedin.dagli.objectio;

import com.linkedin.dagli.util.array.ArraysEx;
import java.util.Objects;
import java.util.function.Predicate;


/**
 * A ObjectReader that represents a collection of values derived from the values of another ObjectReader, as lazily
 * transformed by a given mapping function.  The transformed values are not saved; rather, values are re-transformed
 * every time they are accessed.
 *
 * Note that the Iterables (and derivative Iterators) returned from the mapping function are assumed to be "lightweight"
 * and not require closing.
 *
 * @param <T> the type of the (mapped) elements in this ObjectReader
 */
class LazyFilteredReader<T> implements ObjectReader<T> {
  private ObjectReader<T> _wrapped;
  private Predicate<T> _inclusionTest;
  private long _size = -1; // the cached size of this reader, or -1 if the size is not yet known

  /**
   * Creates a new, lazy filtered iterable (no copies of the data will be made).  Only elements for which inclusionTest
   * returns true will be kept.
   *
   * @param wrapped the source iterable to be filtered
   * @param inclusionTest predictate that returns true for each element to be kept in the resultant filtered iterable
   */
  public LazyFilteredReader(ObjectReader<T> wrapped, Predicate<T> inclusionTest) {
    _wrapped = wrapped;
    _inclusionTest = inclusionTest;
  }

  @Override
  public long size64() {
    if (_size < 0) {
      _size = _wrapped.stream().filter(_inclusionTest).count();
    }

    return _size;
  }

  /**
   * An {@link ObjectIterator} that iterates over a wrapped iterator, ignoring any elements that do not satisfy a given
   * predicate.
   *
   * @param <T> the type of element to be iterated
   */
  public static class Iterator<T> implements ObjectIterator<T> {
    private ObjectIterator<T> _wrapped;
    private T _next = null;
    private boolean _cachedNext = false;
    private Predicate<T> _predicate;

    /**
     * Creates a new instance.
     *
     * @param wrapped the wrapped iterator whose elements should be filtered
     * @param predicate elements for which the predicate returns true will be iterated over by this instance; those for
     *                  whom the predicate returns false will be ignored
     */
    public Iterator(ObjectIterator<T> wrapped, Predicate<T> predicate) {
      _wrapped = Objects.requireNonNull(wrapped);
      _predicate = Objects.requireNonNull(predicate);
    }

    @SuppressWarnings("unchecked") // safe for destination to masquerade as T[]
    private int next(ObjectIteratorNext<T> nextMethod, Object[] destination, int offset, int count) {
      // return our cache if needed
      if (count > 0 && _cachedNext) {
        _cachedNext = false;
        destination[offset] = _next;
        return 1;
      }

      while (true) { // keep trying until we can return at least one thing, if things are indeed available
        int fetched = nextMethod.next(_wrapped, destination, offset, count);
        if (fetched <= 0) {
          return fetched;
        }

        int filtered = ArraysEx.filterInPlace((T[]) destination, offset, offset + fetched, _predicate);
        if (filtered > 0) {
          return filtered;
        }
        // we were able to get elements from the underlying iterator, but they were all filtered out--try fetching more
      }
    }

    @Override
    public int tryNextAvailable(Object[] destination, int offset, int count) {
      return next(ObjectIterator::tryNextAvailable, destination, offset, count);
    }

    @Override
    public int nextAvailable(Object[] destination, int offset, int count) {
      return next(ObjectIterator::nextAvailable, destination, offset, count);
    }

    /**
     * Makes sure that _next references a valid next element
     */
    private void ensureNext() {
      while (!_cachedNext) {
        _next = _wrapped.next();
        _cachedNext = _predicate.test(_next);
      }
    }

    @Override
    public boolean hasNext() {
      while (!_cachedNext && _wrapped.hasNext()) {
        _next = _wrapped.next();
        _cachedNext = _predicate.test(_next);
      }
      return _cachedNext;
    }

    @Override
    public T next() {
      ensureNext();
      _cachedNext = false;
      return _next;
    }

    @Override
    public void close() {
      if (_wrapped != null) {
        _wrapped.close();
      }
      _wrapped = null;
      _next = null;
      _predicate = null;
      _cachedNext = false;
    }
  }

  @Override
  public ObjectIterator<T> iterator() {
    return new Iterator<>(_wrapped.iterator(), _inclusionTest);
  }

  @Override
  public void close() {
    if (_wrapped != null) {
      _wrapped.close();
    }
    _wrapped = null;
    _inclusionTest = null;
  }
}
