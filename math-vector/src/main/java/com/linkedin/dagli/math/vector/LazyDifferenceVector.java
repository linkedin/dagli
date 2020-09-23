package com.linkedin.dagli.math.vector;

import com.linkedin.dagli.math.number.PrimitiveNumberTypes;


/**
 * Lazily subtracts one vector from another.  The subtracted elements are generated on demand, and changes to the
 * original vectors will affect this one.
 */
class LazyDifferenceVector extends LazyUnionVector {
  private static final long serialVersionUID = 1;

  /**
   * Default constructor for the benefit of Kryo serialization.  Results in an invalid instance (Kryo will fill in the
   * fields with deserialized values after instantiation).
   */
  private LazyDifferenceVector() {
    super(null, null);
  }

  /**
   * Creates an instance that will lazily subtract the provides vectors.
   *
   * @param minuend the vector from which to subtract
   * @param subtrahend the vector that will be subtracted
   */
  public LazyDifferenceVector(Vector minuend, Vector subtrahend) {
    super(minuend, subtrahend);
  }

  @Override
  public Class<? extends Number> valueType() {
    return PrimitiveNumberTypes.sumType(getFirstVector().valueType(), getSecondVector().valueType());
  }

  @Override
  protected double compute(double elementValue1, double elementValue2) {
    return elementValue1 - elementValue2;
  }
}
