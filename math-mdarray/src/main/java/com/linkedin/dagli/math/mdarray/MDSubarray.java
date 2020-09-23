package com.linkedin.dagli.math.mdarray;

import com.linkedin.dagli.util.invariant.Arguments;
import java.util.Arrays;


/**
 * An MDArray that provides a "view" on a portion of another array.  Changes in the underlying array affect the subarray
 * view.
 */
final class MDSubarray extends AbstractMDArray {
  private static final long serialVersionUID = 1;

  private final MDArray _mdArray;

  private final long[] _subarrayIndices;
  private final long _subarrayOffset;

  private final long _elementCount;

  /**
   * Creates a new subarray.
   *
   * @param mdArray the original array viewed by this subarray
   * @param subarrayIndices the subarray's indices in the original array
   */
  MDSubarray(MDArray mdArray, long[] subarrayIndices) {
    super(Arrays.copyOfRange(mdArray.shape(), subarrayIndices.length, mdArray.shape().length));
    _elementCount = MDArrays.elementCount(_shape);
    _mdArray = mdArray;
    _subarrayIndices = subarrayIndices.clone();
    _subarrayOffset =
        MDArrays.indicesToOffset(Arrays.copyOf(subarrayIndices, mdArray.shape().length), mdArray.shape());
  }

  @Override
  public Class<? extends Number> valueType() {
    return _mdArray.valueType();
  }

  @Override
  public MDArray subarrayAt(long... indices) {
    return new MDSubarray(_mdArray, MDArrays.concatenate(_subarrayIndices, indices));
  }

  @Override
  public double getAsDouble(long... indices) {
    MDArrays.checkValidIndices(indices, _shape);

    // we could alternatives construct a full long[] index for our underlying array, but--assuming the underlying array
    // has implemented getAsDouble(offset)--this avoids array creation
    return _mdArray.getAsDoubleUnsafe(_subarrayOffset + MDArrays.indicesToOffset(indices, _shape));
  }

  @Override
  public double getAsDouble(long offset) {
    Arguments.indexInRange(offset, 0, _elementCount, () -> "Offset " + offset + " does not fall within the subarray");
    return getAsDoubleUnsafe(offset);
  }

  @Override
  public double getAsDoubleUnsafe(long offset) {
    return _mdArray.getAsDoubleUnsafe(_subarrayOffset + offset);
  }

  @Override
  public long getAsLong(long... indices) {
    MDArrays.checkValidIndices(indices, _shape);

    // we could alternatives construct a full long[] index for our underlying array, but--assuming the underlying array
    // has implemented getAsDouble(offset)--this avoids array creation
    return _mdArray.getAsLongUnsafe(_subarrayOffset + MDArrays.indicesToOffset(indices, _shape));
  }

  @Override
  public long getAsLong(long offset) {
    Arguments.indexInRange(offset, 0, _elementCount, () -> "Offset " + offset + " does not fall within the subarray");
    return getAsLongUnsafe(offset);
  }

  @Override
  public long getAsLongUnsafe(long offset) {
    return _mdArray.getAsLongUnsafe(_subarrayOffset + offset);
  }
}
