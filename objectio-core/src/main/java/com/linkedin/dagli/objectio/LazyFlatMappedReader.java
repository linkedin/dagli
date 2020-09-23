package com.linkedin.dagli.objectio;

import com.linkedin.dagli.util.collection.Iterables;
import java.util.Collections;
import java.util.function.Function;


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
class LazyFlatMappedReader<T> implements ObjectReader<T> {
  private ObjectReader<?> _wrapped;
  private Function<Object, Iterable<T>> _mapper;
  private long _size = -1;

  /**
   * Creates a new instance.
   *
   * @param wrapped the reader to be wrapped, whose elements will be flat-mapped
   * @param mapper the mapper that will flat-map the original elements from the wrapped reader
   * @param <S> the type of element contained in the wrapped reader
   */
  public <S> LazyFlatMappedReader(ObjectReader<S> wrapped, Function<S, Iterable<? extends T>> mapper) {
    _wrapped = wrapped;
    _mapper = (Function) mapper; // correctness is enforced by the parameter typing on this constructor
  }

  @Override
  public long size64() {
    if (_size < 0) {
      long[] size = new long[1];
      _wrapped.forEach(i -> size[0] += Iterables.size64(_mapper.apply(i)));
      _size = size[0];
    }

    return _size;
  }

  /**
   * An {@link ObjectIterator} that flat-maps the elements of a wrapped iterator.
   */
  private class Iterator implements ObjectIterator<T> {
    private ObjectIterator<?> _wrappedIterator = _wrapped.iterator();
    private java.util.Iterator<T> _cachedIterator = Collections.emptyIterator();

    /**
     * Ensures that _cachedIterator references an iterator that can return the next() value.
     */
    private void ensureNext() {
      while (!_cachedIterator.hasNext()) {
        _cachedIterator = _mapper.apply(_wrappedIterator.next()).iterator();
      }
    }

    @Override
    public boolean hasNext() {
      while (!_cachedIterator.hasNext()) {
        if (!_wrappedIterator.hasNext()) {
          return false;
        }
        _cachedIterator = _mapper.apply(_wrappedIterator.next()).iterator();
      }
      return true;
    }

    @Override
    public T next() {
      ensureNext();
      return _cachedIterator.next();
    }

    @Override
    public void close() {
      if (_wrappedIterator != null) {
        _wrappedIterator.close();
      }
      _wrappedIterator = null;
    }
  }

  @Override
  public ObjectIterator<T> iterator() {
    return new Iterator();
  }

  @Override
  public void close() {
    if (_wrapped != null) {
      _wrapped.close();
    }
    _wrapped = null;
    _mapper = null;
  }
}
