package com.linkedin.dagli.math.vector;

import com.linkedin.dagli.math.number.PrimitiveNumberTypes;
import com.linkedin.dagli.util.invariant.Arguments;
import com.linkedin.dagli.util.array.ArraysEx;
import java.io.Serializable;
import java.util.Arrays;
import java.util.OptionalLong;


/**
 * Memory-efficient, read-only sparse vector (with 64-bit indices) where every non-zero element has a certain, fixed
 * value (usually 1, e.g. to represent a one-hot encoding) with these elements' indices stored in sorted arrays.
 *
 * Using this class where appropriate can reduce memory use by approximately 33% compared to {@link SparseFloatArrayVector}.
 */
public class SparseIndexArrayVector extends AbstractVector implements Serializable {
  private static final long serialVersionUID = 1;

  private final long[] _indices;
  private final double _value;

  /**
   * Creates a {@link SparseIndexArrayVector} where elements with the provided indices will take the provided fixed
   * value.  Duplicate indices are ignored (e.g. if the index "5" occurs multiple times in the array, the resulting
   * vector will only have a single element with index 5).
   *
   * @param elementIndices Indices of the vector elements that will assume the provided value
   * @param elementValue The (non-zero) value for each of the specified elements
   */
  public SparseIndexArrayVector(long[] elementIndices, double elementValue) {
    this(elementIndices.clone(), elementValue, true);
  }

  /**
   * Creates a {@link SparseIndexArrayVector} where elements with the provided indices will take the provided fixed
   * value.  Duplicate indices are ignored (e.g. if the index "5" occurs multiple times in the array, the resulting
   * vector will only have a single element with index 5).
   *
   * <strong>The vector takes ownership of the passed array</strong>: it may be modified and possibly stored by the
   * vector, and should not be subsequently used by the client.
   *
   * @param elementIndices Indices of the vector elements that will assume the provided value
   * @param elementValue The (non-zero) value for each of the specified elements
   */
  public static SparseIndexArrayVector wrap(long[] elementIndices, double elementValue) {
    return new SparseIndexArrayVector(elementIndices, elementValue, true);
  }

  /**
   * Creates a {@link SparseIndexArrayVector} where elements with the provided indices will take the provided fixed
   * value.  Duplicate indices are ignored (e.g. if the index "5" occurs multiple times in the array, the resulting
   * vector will only have a single element with index 5).
   *
   * <strong>The vector takes ownership of the passed array</strong>: it may be modified and possibly stored by the
   * vector, and should not be subsequently used by the client.
   *
   * @param elementIndices Indices of the vector elements that will assume the provided value
   * @param elementValue The (non-zero) value for each of the specified elements
   * @param dummyArg dummy parameter that serves to distinguish this constructor from an otherwise-identical public
   *                 constructor
   */
  private SparseIndexArrayVector(long[] elementIndices, double elementValue, boolean dummyArg) {
    Arguments.check(elementValue != 0, "The element value for the provided indices may not be 0");

    // sort the input indices
    Arrays.sort(elementIndices);

    // remove duplicate indices
    int uniqueIndexCount = ArraysEx.deduplicateSortedArray(elementIndices);
    if (uniqueIndexCount < elementIndices.length) {
      elementIndices = Arrays.copyOf(elementIndices, uniqueIndexCount);
    }

    _indices = elementIndices;
    _value = elementValue;
  }

  /**
   * Private no-args constructor specifically for the benefit of Kryo
   */
  private SparseIndexArrayVector() {
    _indices = null; // null ensures deserialization failure is obvious
    _value = 1;
  }

  @Override
  public Class<? extends Number> valueType() {
    return PrimitiveNumberTypes.smallestTypeForValue(_value);
  }

  @Override
  public long size64() {
    return _indices.length;
  }

  private class Iterator implements VectorElementIterator {
    private int _offset = 0;

    @Override
    public <T> T mapNext(VectorElementFunction<T> mapper) {
      return mapper.apply(_indices[_offset++], _value);
    }

    @Override
    public void next(VectorElementConsumer consumer) {
      consumer.consume(_indices[_offset++], _value);
    }

    @Override
    public boolean hasNext() {
      return _offset < _indices.length;
    }
  }

  private class ReverseIterator implements VectorElementIterator {
    private int _offset = _indices.length - 1;

    @Override
    public <T> T mapNext(VectorElementFunction<T> mapper) {
      return mapper.apply(_indices[_offset--], _value);
    }

    @Override
    public void next(VectorElementConsumer consumer) {
      consumer.consume(_indices[_offset--], _value);
    }

    @Override
    public boolean hasNext() {
      return _offset >= 0;
    }
  }

  @Override
  public VectorElementIterator iterator() {
    return new Iterator();
  }

  @Override
  public VectorElementIterator reverseIterator() {
    return new ReverseIterator();
  }

  @Override
  public double get(long index) {
    return Arrays.binarySearch(_indices, index) < 0 ? 0.0 : _value;
  }

  @Override
  public OptionalLong minNonZeroElementIndex() {
    return _indices.length > 0 ? OptionalLong.of(_indices[0]) : OptionalLong.empty();
  }

  @Override
  public OptionalLong maxNonZeroElementIndex() {
    return _indices.length > 0 ? OptionalLong.of(_indices[_indices.length - 1]) : OptionalLong.empty();
  }
}
