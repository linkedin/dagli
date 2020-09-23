package com.linkedin.dagli.math.vector;

import com.linkedin.dagli.math.number.PrimitiveNumberTypes;


/**
 * Lazily sums two vectors.  The summed elements are generated on demand, and changes to the original vectors will
 * affect this one.
 */
class LazySumVector extends LazyUnionVector {
  private static final long serialVersionUID = 1;

  /**
   * Default constructor for the benefit of Kryo serialization.  Results in an invalid instance (Kryo will fill in the
   * fields with deserialized values after instantiation).
   */
  private LazySumVector() {
    super(null, null);
  }

  /**
   * Creates an instance that lazily sums the provided vectors.
   *
   * @param first the first vector to sum
   * @param second the second vector to sum
   */
  public LazySumVector(Vector first, Vector second) {
    super(first, second);
  }

  @Override
  public Class<? extends Number> valueType() {
    return PrimitiveNumberTypes.sumType(getFirstVector().valueType(), getSecondVector().valueType());
  }

  @Override
  protected double compute(double elementValue1, double elementValue2) {
    return elementValue1 + elementValue2;
  }
}
