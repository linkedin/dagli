package com.linkedin.dagli.math.vector;

import com.linkedin.dagli.math.number.PrimitiveNumberTypes;
import com.linkedin.dagli.util.invariant.Arguments;
import java.util.Arrays;
import java.util.NoSuchElementException;


/**
 * A {@link DenseVector} that lazily concatenates other dense vectors, allocating each a segment of its "index space" to
 * accommodate their entries.  The actual elements of the concatenated vector will not be materialized in memory and
 * will instead be tied to the underlying vectors.  Changes to these vectors will be reflected in their concatenated
 * vector.
 */
public class LazyConcatenatedDenseVector extends AbstractVector implements DenseVector {
  private final DenseVector[] _denseVectors;
  private final long[] _cumulativeVectorCapacities;

  /**
   * Private no-args constructor for Kryo
   */
  private LazyConcatenatedDenseVector() {
    _denseVectors = null;
    _cumulativeVectorCapacities = null;
  }

  /**
   * Creates a new instance that will (lazily) concatenate the provided vectors.
   *
   * The "index space"" allocated for each constituent vector in the concatenated vector will be determined by the
   * capacities of each provided vector.  If the vector later grows to include non-zero elements with indices equal or
   * greater than the capacity at the time this instance was created, these elements will be omitted from the
   * concatenated vector.
   *
   * @param vectors the vectors to concatenate
   */
  public LazyConcatenatedDenseVector(DenseVector... vectors) {
    this(vectors, Arrays.stream(vectors).mapToLong(DenseVector::capacity).toArray());
  }

  /**
   * Creates a new instance that will (lazily) concatenate the provided vectors.
   *
   * The indices allocated for each constituent vector in the concatenated vector are determined by the
   * capacities specified by <code>vectorCapacities</code>.  If any non-zero elements in these vectors have indices
   * equal or greater than these provided capacities, such elements will be omitted from the concatenated vector.
   *
   * @param vectors the vectors to concatenate (at least one)
   * @param vectorCapacities a parallel array to <code>vectors</code> that specifies the capacity that will be assumed
   *                         for each of these vectors (and thus how much of the "index space" is allocated to them).
   *                         These do not need to match the true capacities of each vector.
   */
  public LazyConcatenatedDenseVector(DenseVector[] vectors, long[] vectorCapacities) {
    this(vectors.clone(), vectorCapacities.clone(), true);
  }

  /**
   * Creates a new instance that will (lazily) concatenate the provided vectors.
   *
   * The new instance takes ownership of the provided arrays and may modify them.  They should not be subsequently used
   * by the client.
   *
   * The indices allocated for each constituent vector in the concatenated vector are determined by the
   * capacities specified by <code>vectorCapacities</code>.  If any non-zero elements in these vectors have indices
   * equal or greater than these provided capacities, such elements will be omitted from the concatenated vector.
   *
   * @param vectors the vectors to concatenate (at least one)
   * @param vectorCapacities a parallel array to <code>vectors</code> that specifies the capacity that will be assumed
   *                         for each of these vectors (and thus how much of the "index space" is allocated to them).
   *                         These do not need to match the true capacities of each vector.
   * @param dummyParameter serves no purpose other than to disambiguate this constructor
   */
  private LazyConcatenatedDenseVector(DenseVector[] vectors, long[] vectorCapacities, boolean dummyParameter) {
    Arguments.check(vectors.length > 0, "At least one vector must be provided to concatenate");
    Arguments.equals(vectors.length, vectorCapacities.length,
        () -> "Provided dense vector array and vector capacity array are not the same length");
    _denseVectors = vectors;
    _cumulativeVectorCapacities = vectorCapacities;

    for (int i = 1; i < _cumulativeVectorCapacities.length; i++) {
      _cumulativeVectorCapacities[i] += _cumulativeVectorCapacities[i - 1];
    }
  }

  /**
   * Creates a new instance of {@link LazyConcatenatedDenseVector} that will (lazily) concatenate the provided vectors.
   *
   * The new instance takes ownership of the provided arrays and may modify them.  They should not be subsequently used
   * by the client.
   *
   * The indices allocated for each constituent vector in the concatenated vector are determined by the
   * capacities specified by <code>vectorCapacities</code>.  If any non-zero elements in these vectors have indices
   * equal or greater than these provided capacities, such elements will be omitted from the concatenated vector.
   *
   * @param vectors the vectors to concatenate (at least one)
   * @param vectorCapacities a parallel array to <code>vectors</code> that specifies the capacity that will be assumed
   *                         for each of these vectors (and thus how much of the "index space" is allocated to them).
   *                         These do not need to match the true capacities of each vector.
   */
  public static LazyConcatenatedDenseVector wrap(DenseVector[] vectors, long[] vectorCapacities) {
    return new LazyConcatenatedDenseVector(vectors, vectorCapacities, true);
  }

  /**
   * Checks if this instance simply wraps a single DenseVector with a nominal capacity equal or greater than that
   * vector (which means that the logical set of elements in both the wrapper and wrapped vectors are exactly the same).
   * When this is the case, faster, simplified implementations of some methods can be used.
   *
   * @return true if this wrapper is "trivial", false otherwise
   */
  private boolean isTrivialWrapper() {
    return _denseVectors.length == 1 && _cumulativeVectorCapacities[0] >= _denseVectors[0].capacity();
  }

  @Override
  public long capacity() {
    return _cumulativeVectorCapacities[_cumulativeVectorCapacities.length - 1];
  }

  @Override
  public Class<? extends Number> valueType() {
    Class<? extends Number> type = _denseVectors[0].valueType();
    for (int i = 1; i < _denseVectors.length; i++) {
      type = PrimitiveNumberTypes.smallestCommonType(type, _denseVectors[i].valueType());
    }
    return type;
  }

  // the contract of a dense vector is that this method is O(1) relative to the number of elements; although this
  // implementation takes time O(log(_denseVectors.length)) this can be viewed as a constant factor.
  @Override
  public double get(long index) {
    if (index >= capacity()) {
      return 0;
    }
    int vectorIndex = Arrays.binarySearch(_cumulativeVectorCapacities, index);
    if (vectorIndex >= 0) {
      // exactly at a cumulative capacity in the cumulative capacities array--the corresponding vector is the next one
      vectorIndex += 1;
    } else { // vectorIndex is -("insertion point" - 1) (insertion point is the first element > index)
      vectorIndex = -vectorIndex - 1; // -vectorIndex == "insertion point" + 1, so we subtract 1 to get insertion point
    }
    if (vectorIndex == 0) {
      return _denseVectors[0].get(index);
    } else {
      return _denseVectors[vectorIndex].get(index - _cumulativeVectorCapacities[vectorIndex - 1]);
    }
  }

  private long capacityForVectorIndex(int index) {
    return index == 0 ? _cumulativeVectorCapacities[0]
        : _cumulativeVectorCapacities[index] - _cumulativeVectorCapacities[index - 1];
  }

  @Override
  public long size64() {
    long[] sum = new long[1];
    for (int i = 0; i < _denseVectors.length; i++) {
      long vecCapacity = capacityForVectorIndex(i);
      DenseVector vector = _denseVectors[i];

      // if we can cheaply prove that all the vector's non-zero elements are within the capacity we've assigned to it,
      // we can just use vector.size64() to get the vector's size
      if (vector.capacity() <= vecCapacity) {
        sum[0] += vector.size64();
      } else {
        // need to iterate through the elements
        vector.iterator().forEachRemainingUntilFalse((idx, val) -> {
          if (idx >= vecCapacity) {
            return false;
          }

          sum[0]++;
          return true;
        });
      }
    }
    return sum[0];
  }

  @Override
  public VectorElementIterator iterator() {
    if (isTrivialWrapper()) {
      return _denseVectors[0].iterator();
    }

    return new Iterator(false);
  }

  @Override
  public VectorElementIterator reverseIterator() {
    if (isTrivialWrapper()) {
      return _denseVectors[0].reverseIterator();
    }

    return new Iterator(true);
  }

  /**
   * Iterator over lazily-concatenated elements.
   *
   * This implementation uses the underlying vector's iterators, which is fast for forward iteration, but not
   * necessarily optimal for reverse iteration because elements that are beyond the assigned capacity for that vector
   * must be skipped over.  This could be avoided by using the get() method on the underlying vector to fetch the
   * required elements instead.
   */
  private class Iterator implements VectorElementIterator {
    private int _currentVectorIndex;
    private VectorElementIterator _currentIterator;
    private long _currentOffset;
    private long _currentCapacity;
    private boolean _reverse;

    private long _nextIndex = -1;
    private double _nextValue;

    Iterator(boolean reverse) {
      _reverse = reverse;
      _currentVectorIndex = reverse ? _denseVectors.length - 1 : 0;
      _currentIterator =
          reverse ? _denseVectors[_denseVectors.length - 1].reverseIterator() : _denseVectors[0].iterator();
      _currentOffset = calculateOffset();
      _currentCapacity = capacityForVectorIndex(_currentVectorIndex);
    }

    private long calculateOffset() {
      return _currentVectorIndex == 0 ? 0 : _cumulativeVectorCapacities[_currentVectorIndex - 1];
    }

    private long getAndClearNextIndex() {
      long result = _nextIndex + _currentOffset;
      _nextIndex = -1;
      return result;
    }

    @Override
    public VectorElement next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      return new VectorElement(getAndClearNextIndex(), _nextValue);
    }

    @Override
    public <T> T mapNext(VectorElementFunction<T> mapper) {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      return mapper.apply(getAndClearNextIndex(), _nextValue);
    }

    @Override
    public void next(VectorElementConsumer consumer) {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      consumer.consume(getAndClearNextIndex(), _nextValue);
    }

    private boolean nextIterator() {
      if (_reverse) {
        if (_currentVectorIndex == 0) {
          return false;
        }
        _currentIterator = _denseVectors[--_currentVectorIndex].reverseIterator();
      } else {
        if (_currentVectorIndex == _denseVectors.length - 1) {
          return false;
        }
        _currentIterator = _denseVectors[++_currentVectorIndex].iterator();
      }

      _currentOffset = calculateOffset(); // cache the current offset
      _currentCapacity = capacityForVectorIndex(_currentVectorIndex); // cache the current capacity
      return true;
    }

    private boolean cacheNext() {
      while (true) {
        while (!_currentIterator.hasNext()) { // while current iterator is exhausted...
          if (!nextIterator()) { // try to get another iterator
            _nextIndex = -1; // invalidate the cache
            return false;  // no more iterators (and thus no more elements)--return false
          }
        }

        _currentIterator.next((idx, val) -> {
          _nextIndex = idx;
          _nextValue = val;
        });

        if (_nextIndex < _currentCapacity) {
          return true; // successfully cached next element
        } else if (!_reverse) {
          // iterating forward, so we must be at an index beyond the specified capacity of the current vector
          if (!nextIterator()) { // force an immediate advance to the next vector
            _nextIndex = -1; // invalidate the cache
            return false;  // no more iterators (and thus no more elements)--return false
          }
        }
      }
    }

    @Override
    public boolean hasNext() {
      return _nextIndex >= 0 || cacheNext();
    }
  }

  @Override
  public void copyTo(float[] dest, int start, int length) {
    int copied = 0;
    for (int i = 0; i < _denseVectors.length; i++) {
      long vectorSize = i == 0 ? _cumulativeVectorCapacities[0]
          : (_cumulativeVectorCapacities[i] - _cumulativeVectorCapacities[i - 1]);

      int toCopy = Math.toIntExact(Math.min(length - copied, vectorSize));
      _denseVectors[i].copyTo(dest, start + copied, toCopy);
      copied += toCopy;

      assert copied <= length;
      if (copied == length) {
        return;
      }
    }
  }

  @Override
  public void copyTo(double[] dest, int start, int length) {
    int copied = 0;
    for (int i = 0; i < _denseVectors.length; i++) {
      long vectorSize = i == 0 ? _cumulativeVectorCapacities[0]
          : (_cumulativeVectorCapacities[i] - _cumulativeVectorCapacities[i - 1]);

      int toCopy = Math.toIntExact(Math.min(length - copied, vectorSize));
      _denseVectors[i].copyTo(dest, start + copied, toCopy);
      copied += toCopy;

      assert copied <= length;
      if (copied == length) {
        return;
      }
    }
  }
}
