package com.linkedin.dagli.math.vector;

import com.linkedin.dagli.math.number.PrimitiveNumberTypes;


/**
 * Lazily element-wise multiplies this vector by another vector (Hadamard product).  Changes to the original vectors
 * will affect this product.
 */
class LazyProductVector extends LazyIntersectionVector {
  private static final long serialVersionUID = 1;

  /**
   * Default constructor for the benefit of Kryo serialization.  Results in an invalid instance (Kryo will fill in the
   * fields with deserialized values after instantiation).
   */
  private LazyProductVector() {
    this(null, null);
  }

  @Override
  public Class<? extends Number> valueType() {
    return PrimitiveNumberTypes.productType(getFirstVector().valueType(), getSecondVector().valueType());
  }

  /**
   * Create a vector that is the logical, lazily-computed Hadamard product of the provided vectors.
   *
   * @param first the first vector to multiply
   * @param second the second vector to multiply
   */
  public LazyProductVector(Vector first, Vector second) {
    super(first, second);
  }

  @Override
  protected double compute(double elementValue1, double elementValue2) {
    return elementValue1 * elementValue2;
  }
}

