package com.linkedin.dagli.objectio;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Random;


/**
 * Partially shuffles a wrapped reader by maintaining a K-sized buffer of elements in memory.  Once the first K elements
 * of the wrapped reader are in the buffer, the shuffled reader iterates elements via the following procedure:
 * (1) Get the next element from the wrapped reader
 * (2) Assign it a random location in the buffer
 * (3) Return (as the next shuffled element) the element currently in that location
 *
 * Once the last element from the underlying reader is read, the elements remaining in the buffer are returned in random
 * order.
 *
 * It's important to note that this is *not* a true, uniform/fair shuffle.  E.g. the first K elements returned are
 * guaranteed to be among the first 2*K elements in the wrapped reader.  It is more akin to minibatch shuffling, albeit
 * with greater randomization: the last element returned can in principle be any element, but is much more likely to be
 * an element near the end of the original reader.
 *
 * The advantage over a true shuffle (e.g. Fisher-Yates) is that the memory requirement is O(K) rather than O(n), where
 * n is the number of elements in the wrapped reader.  If K >= n this class performs a true shuffle.  For machine
 * learning purposes a partial shuffle can achieve much of the benefit of a full shuffle without a potentially high
 * shuffling cost, depending on the original "state" of the data.
 *
 * @param <T> the type of element read by the reader
 */
class LazyShuffledBufferReader<T> implements ObjectReader<T> {
  private final ObjectReader<T> _wrapped;
  private final long _seed;
  private final int _bufferSize;

  /**
   * Creates a new shuffled reader that will partially shuffle the provided, wrapped reader.
   * The shuffled order is deterministically dependent the the provided seed.
   *
   * Note that the buffer is not allocated when this class is created.  Instead, each iterator will have its own buffer.
   * This class itself is "lazy", meaning that it stores no data is instead backed by the wrapped reader.  Consequently,
   * its memory cost is trivial.
   *
   * @param wrapped the reader to (partially) shuffle
   * @param seed the random seed to use
   * @param bufferSize the size of the buffer; a larger buffer makes the shuffle more "thorough" at the expense of
   *                   greater memory usage.
   */
  public LazyShuffledBufferReader(ObjectReader<T> wrapped, long seed, int bufferSize) {
    _wrapped = wrapped;
    _seed = seed;
    _bufferSize = (int) Math.min(bufferSize, _wrapped.size64());
  }

  @Override
  public long size64() {
    return _wrapped.size64();
  }

  /**
   * Partially shuffles a wrapped iterator by maintaining a K-sized buffer of elements in memory.  Once the first K
   * elements of the wrapped iterator are in the buffer, the shuffled iterator iterates elements via the following
   * procedure:
   * (1) Get the next element from the wrapped iterator
   * (2) Assign it a random location in the buffer
   * (3) Return (as the next shuffled element) the element currently in that location
   *
   * Once the last element from the underlying iterator is read, the elements remaining in the buffer are returned in
   * random order.
   *
   * It's important to note that this is *not* a true, uniform/fair shuffle.  E.g. the first K elements returned are
   * guaranteed to be among the first 2*K elements in the wrapped reader.  It is more akin to minibatch shuffling,
   * albeit with greater randomization: the last element returned can in principle be any element, but is much more
   * likely to be an element near the end of the original iterator.
   *
   * The advantage over a true shuffle (e.g. Fisher-Yates) is that the memory requirement is O(K) rather than O(n),
   * where n is the number of elements in the wrapped iterator.  If K >= n this class performs a true shuffle.  For
   * machine learning purposes a partial shuffle can achieve much of the benefit of a full shuffle without a potentially
   * high shuffling cost, depending on the original "state" of the data.
   *
   * @param <T> the type of object iterated by this iterator
   */
  public static class Iterator<T> implements ObjectIterator<T> {
    private final ObjectIterator<T> _wrapped;
    private final T[] _buffer; // real type is Object[], masquerading as T[] for convenience
    private final Random _random;
    private int _bufferedCount = 0;

    /**
     * Creates a new instance.
     *
     * @param wrapped the iterator whose elements should be shuffled
     * @param seed a seed used to randomize the shuffling
     * @param bufferSize the size of the buffer used for shuffling.  Larger buffers allow for better approximation of
     *                   a true, uniform shuffle, where every ordering of the elements is equally likely.
     */
    public Iterator(ObjectIterator<T> wrapped, long seed, int bufferSize) {
      _wrapped = wrapped;
      _buffer = (T[]) new Object[bufferSize]; // masquerade as T[] for convenience
      _random = new Random(seed);
    }

    @Override
    public boolean hasNext() {
      return _wrapped.hasNext() || _bufferedCount > 0;
    }

    @Override
    public T next() {
      if (_wrapped.hasNext()) {
        if (_bufferedCount == 0) {
          _bufferedCount = _wrapped.next(_buffer, 0, _buffer.length); // _buffer is really of type Object[]
          return next();
        }

        int nextIndex = _random.nextInt(_buffer.length);
        T res = _buffer[nextIndex];
        _buffer[nextIndex] = _wrapped.next();
        return res;
      }

      // wrapped has no more elements
      if (_bufferedCount > 0) {
        int nextIndex = _random.nextInt(_bufferedCount);
        _bufferedCount--;
        T res = _buffer[nextIndex];
        _buffer[nextIndex] = _buffer[_bufferedCount];
        _buffer[_bufferedCount] = null;
        return res;
      }

      throw new NoSuchElementException();
    }

    @Override
    public void close() {
      _wrapped.close();
      Arrays.fill(_buffer, 0, _bufferedCount, null);
    }
  }

  @Override
  public ObjectIterator<T> iterator() {
    return new Iterator<>(_wrapped.iterator(), _seed, _bufferSize);
  }

  @Override
  public void close() {
    _wrapped.close();
  }
}
