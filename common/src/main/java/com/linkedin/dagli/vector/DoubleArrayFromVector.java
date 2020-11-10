package com.linkedin.dagli.vector;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.object.Max;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer2;
import com.linkedin.dagli.util.invariant.Arguments;


/**
 * Transforms {@link Vector}s to double[] arrays containing a dense representation of the vector's elements (offset 0
 * in the array corresponds with the element at index 0 in the vector, offset 1 corresponds with index 1, etc., and
 * negative indices cannot be represented).
 *
 */
@ValueEquality
public class DoubleArrayFromVector extends AbstractPreparedTransformer2<Vector, Long, double[], DoubleArrayFromVector> {
  private static final long serialVersionUID = 1;

  private boolean _consistentArrayLength = false;
  private boolean _ignoreOutOfBoundsIndices = false;
  private int _minArrayLength = 0;
  private int _maxArrayLength = Integer.MAX_VALUE;

  public DoubleArrayFromVector withInput(Producer<? extends Vector> vectorInput) {
    return clone(c -> {
      c._input1 = vectorInput;
      c.configureLengthInput();
    });
  }

  private void configureLengthInput() {
    MaxNonZeroVectorElementIndex maxNonZeroElementIndex = new MaxNonZeroVectorElementIndex().withInput(_input1);
    _input2 = _consistentArrayLength ? new Max<Long>().withInput(maxNonZeroElementIndex) : maxNonZeroElementIndex;
  }

  /**
   * Creates a copy of this instance whose results will have a consistent length.
   *
   * <strong>The transformer created and returned by this method will have "hidden" preparable ancestors and your DAG
   * will also be preparable.</strong>
   *
   * "Consistent length" means that all result arrays produced by this transformer will have the same length, regardless
   * of the contents of the input vector.  This fixed length chosen will be as large as needed to represent all elements
   * (with non-negative indices) of the vectors seen during preparation, subject to the constraints of the minimum and
   * maximum array length configured for this instance.
   *
   * If vectors with higher-index elements are later encountered by the prepared transformer, those elements whose index
   * exceeds the fixed length chosen during preparation will be ignored.
   *
   * By default, result array lengths will not be consistent, meaning that the resulting array sizes will vary within
   * the configured minimum and maximum result array lengths to accommodate the actual elements of the input vectors.
   *
   * @return a copy of this instance that will produce arrays of uniform length
   */
  public DoubleArrayFromVector withConsistentArrayLength() {
    return clone(c -> {
      c._consistentArrayLength = true;
      c.configureLengthInput();
    });
  }

  /**
   * Creates a copy of this instance that will ignore non-zero elements in the input vectors whose indices are either
   * negative or greater or equal to the maximum array length (these cannot be included in the resulting array).
   *
   * By default, an {@link IndexOutOfBoundsException} is thrown when such an "out of bounds" element index is
   * encountered, to avoid a potential silent loss of information.
   *
   * @return a copy of this instance that will ignore vector elements with out-of-bounds indices
   */
  public DoubleArrayFromVector withIgnoredOutOfBoundsIndices() {
    return clone(c -> c._ignoreOutOfBoundsIndices = true);
  }

  /**
   * Creates a copy of this instance that will produce arrays with a size no less than that specified, even when this
   * size is longer than that needed to represent all the non-zero elements of the vector.
   *
   * By default the minimum array length is 0.
   *
   * @param minArrayLength the minimum length of all arrays produced by the returned instance
   * @return a copy of this instance that will use the specified minimum array length
   */
  public DoubleArrayFromVector withMinimumArrayLength(int minArrayLength) {
    return clone(c -> c._minArrayLength = minArrayLength);
  }

  /**
   * Creates a copy of this instance that will produce arrays with a size no greater than that specified, even when this
   * size is less than that needed to represent all the non-zero elements of the vector.  Elements with indices beyond
   * this limit will result in an exception if {@link #withIgnoredOutOfBoundsIndices()} is not used.
   *
   * By default the maximum array length is {@link Integer#MAX_VALUE}.
   *
   * @param maxArrayLength the maximum length of all arrays produced by the returned instance
   * @return a copy of this instance that will use the specified maximum array length
   */
  public DoubleArrayFromVector withMaximumArrayLength(int maxArrayLength) {
    return clone(c -> c._maxArrayLength = maxArrayLength);
  }

  @Override
  public double[] apply(Vector vector, Long maxNonZeroElementIndex) {
    if (!_ignoreOutOfBoundsIndices) {
      Arguments.check(vector.minNonZeroElementIndex().orElse(0) >= 0,
          "Input vector is not allowed to have non-zero elements with negative indices");

      if (!_consistentArrayLength) { // if consistent length is requested, elements at too-high indices are ignored
        Arguments.check(maxNonZeroElementIndex < _maxArrayLength,
            () -> "Vector has non-zero element at index " + maxNonZeroElementIndex
                + " which exceeds the configured maximum length of " + _maxArrayLength);
      }
    }

    int vectorLength = (int) Math.max(Math.min(maxNonZeroElementIndex + 1, _maxArrayLength), _minArrayLength);
    double[] result = new double[vectorLength];
    vector.copyTo(result);
    return result;
  }
}
