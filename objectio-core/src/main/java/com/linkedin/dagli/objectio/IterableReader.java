package com.linkedin.dagli.objectio;

import com.linkedin.dagli.util.closeable.Closeables;
import com.linkedin.dagli.util.collection.Iterables;


/**
 * Wraps an {@link Iterable} as a {@link ObjectReader}.  Changes to the wrapped iterable will be reflected in the
 * {@link IterableReader}.
 *
 * @param <T> the type of element in the iterable
 */
public class IterableReader<T> implements ObjectReader<T> {
  private Iterable<? extends T> _iterable;
  private long _size = -1;

  /**
   * Create a new wrapper around the given collection.
   *
   * @param iterable the collection to be wrapped
   * @param size the size of the collection; this must be the number of elements actually available
   *             via iterable's iterator.
   */
  public IterableReader(Iterable<? extends T> iterable, long size) {
    _iterable = iterable;
    _size = size;
  }

  /**
   * Creates a new wrapper around the given collection.
   *
   * @param iterable the collection to be wrapped
   */
  public IterableReader(Iterable<? extends T> iterable) {
    this(iterable, -1);
  }

  @Override
  public long size64() {
    if (_size < 0) {
      _size = Iterables.size64(_iterable);
    }

    return _size;
  }

  @Override
  public ObjectIterator<T> iterator() {
    java.util.Iterator collectionIterator = _iterable.iterator();
    if (collectionIterator instanceof ObjectIterator) {
      return (ObjectIterator<T>) collectionIterator;
    }

    return new Iterator<T>(_iterable.iterator());
  }

  /**
   * Closing has no effect on the underlying collection.
   */
  @Override
  public void close() { }

  /**
   * ObjectIterator wrapper for a "normal" {@link Iterator}
   *
   * @param <T> the type of element iterated
   */
  public static class Iterator<T> implements ObjectIterator<T> {
    private final java.util.Iterator<? extends T> _iterator;

    /**
     * Creates a new instance that wraps the given iterator.  If the wrapped iterator is closeable, it *will* be closed
     * by the close() method of the wrapper.
     *
     * @param iterator the iterator to wrap
     */
    public Iterator(java.util.Iterator<? extends T> iterator) {
      _iterator = iterator;
    }

    @Override
    public boolean hasNext() {
      return _iterator.hasNext();
    }

    @Override
    public T next() {
      return _iterator.next();
    }

    @Override
    public void close() {
      Closeables.tryClose(_iterator);
    }
  }
}
