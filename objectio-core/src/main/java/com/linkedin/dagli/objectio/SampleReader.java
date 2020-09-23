package com.linkedin.dagli.objectio;

import com.linkedin.dagli.util.array.ArraysEx;
import java.util.Random;


/**
 * Wraps a ObjectReader and provides a random sample of its elements.
 *
 * This works by mapping every element to a real number in the range [0, 1.0) and then checking if it falls within
 * the "segment" (a subrange which lies within [0, 1.0)).  If an element is in the subrange, it is included in the set
 * of elements contained in this ObjectReader; if it is not, it is ignored.
 *
 * @param <T> the type of element read by this reader
 */
class SampleReader<T> implements ObjectReader<T> {
  private final ObjectReader<T> _wrapped;
  private final SampleSegment _segment;

  private long _size = -1; // the cached size of this reader, or -1 if the size has not yet been calculated

  /**
   * Creates a new instance.
   *
   * @param reader the reader whose elements are to be sampled
   * @param segment the segment that defines which elements are to be sampled from the wrapped reader
   */
  public SampleReader(ObjectReader<T> reader, SampleSegment segment) {
    _wrapped = reader;
    _segment = segment;
  }

  @Override
  public long size64() {
    if (_size < 0) {
      long sampledSize = 0;

      Random r = new Random(_segment.getSeed());
      long originalSize = _wrapped.size64();

      for (long i = 0; i < originalSize; i++) {
        if (sample(r)) {
          sampledSize++;
        }
      }

      _size = sampledSize;
    }

    return _size;
  }

  /**
   * Checks whether the next element should be sampled.
   *
   * @param r a {@link Random} that represents the current state of the sampler.
   * @return true or false
   */
  private boolean sample(Random r) {
    return _segment.contains(r.nextDouble());
  }

  /**
   * An iterator that samples the elements of another, wrapped iterator.
   *
   * @param <T> the type of element iterated by the iterator
   */
  private static class Iterator<T> implements ObjectIterator<T> {
    private final SampleReader<T> _owner;
    private final ObjectIterator<T> _wrapped;
    private final Random _rand;

    private boolean _presampled = false; // is _wrapped's next element already known to be included in the sample?

    /**
     * Creates a new instance.
     *
     * @param owner the {@link SampleReader} instance being iterated
     */
    public Iterator(SampleReader<T> owner) {
      _owner = owner;
      _wrapped = _owner._wrapped.iterator();
      _rand = new Random(_owner._segment.getSeed());
    }

    @Override
    public boolean hasNext() {
      if (_presampled) {
        // _wrapped's next element is available and will is known to be in our sample
        return true;
      }

      while (_wrapped.hasNext()) {
        if (_owner.sample(_rand)) {
          _presampled = true; // we know _wrapped's next element is in our sample
          return true;
        }

        _wrapped.next(); // not sampled, skip
      }

      return false;
    }

    @Override
    public T next() {
      if (_presampled) {
        // we know _wrapped's next element is in our sample
        _presampled = false; // but the next element might not be, so clear the presampled flag
        return _wrapped.next();
      }

      while (!_owner.sample(_rand)) {
        _wrapped.next();
      }

      return _wrapped.next();
    }

    private int next(ObjectIteratorNext<T> nextMethod, Object[] destination, int offset, int count) {
      while (true) {
        int actual = nextMethod.next(_wrapped, destination, offset, count);
        if (actual <= 0) {
          return actual;
        }

        if (_presampled) {
          // the first element we retrieved *must* be included in the resulting sample
          _presampled = false;
          return ArraysEx.filterInPlace(destination, offset + 1, offset + actual, v -> _owner.sample(_rand)) + 1;
        }

        int sampledCount = ArraysEx.filterInPlace(destination, offset, offset + actual, v -> _owner.sample(_rand));
        if (sampledCount > 0) {
          return sampledCount;
        }
      }
    }

    @Override
    public int nextAvailable(Object[] destination, int offset, int count) {
      return next(ObjectIterator::nextAvailable, destination, offset, count);
    }

    @Override
    public int tryNextAvailable(Object[] destination, int offset, int count) {
      return next(ObjectIterator::tryNextAvailable, destination, offset, count);
    }

    @Override
    public void close() {
      _wrapped.close();
    }
  }

  @Override
  public ObjectIterator<T> iterator() {
    return new Iterator<>(this);
  }

  @Override
  public void close() {
    _wrapped.close();
  }
}
