package com.linkedin.dagli.math.mdarray;

import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.util.invariant.Arguments;


/**
 * A {@link MDArray} backed by a {@link Vector}.  Changes to the underlying vector will by reflected by this instance.
 */
public class VectorAsMDArray extends AbstractMDArray {
  private static final long serialVersionUID = 1;

  private final Vector _vector;
  private final long _elementCount;

  /**
   * Creates a new instance backed by the specified vector.  The shape can be any (valid) size; any components of the
   * {@link MDArray} with no corresponding vector element will simply be 0.
   *
   * @param vector a vector
   * @param shape the shape of the array
   */
  public VectorAsMDArray(Vector vector, long... shape) {
    super(shape.clone());
    _elementCount = MDArrays.elementCount(shape);
    _vector = vector;
  }

  @Override
  public Class<? extends Number> valueType() {
    return _vector.valueType();
  }

  @Override
  public DenseVector asVector() {
    if  (_vector instanceof DenseVector && ((DenseVector) _vector).capacity() == _elementCount) {
      // special case: our wrapped vector has a capacity corresponding to our shape
      return (DenseVector) _vector;
    }

    return super.asVector();
  }

  @Override
  public double getAsDouble(long... indices) {
    MDArrays.checkValidIndices(indices, _shape);

    return _vector.get(MDArrays.indicesToOffset(indices, _shape));
  }

  @Override
  public double getAsDouble(long offset) {
    Arguments.indexInRange(offset, 0, _elementCount, () -> "Offset must be between 0 and " + _elementCount);
    return _vector.get(offset);
  }

  @Override
  public double getAsDoubleUnsafe(long offset) {
    return _vector.get(offset);
  }

  @Override
  public long getAsLong(long... indices) {
    return (long) getAsDouble(indices); // vectors have no mechanism for retrieving long values; double is closest
  }

  @Override
  public long getAsLong(long offset) {
    return (long) getAsDouble(offset); // vectors have no mechanism for retrieving long values; double is closest
  }

  @Override
  public long getAsLongUnsafe(long offset) {
    return (long) getAsDoubleUnsafe(offset); // vectors have no mechanism for retrieving long values; double is closest
  }
}
