package com.linkedin.dagli.math.vector;

import com.linkedin.dagli.math.number.PrimitiveNumberTypes;


/**
 * Lazily multiplies all the elements of a vector by a scalar value.  Changes to the original vector affect the elements
 * of this one.
 */
class LazyScalarProductVector extends LazyTransformedValueVector {
  private static final long serialVersionUID = 1;

  private final double _factor;

  /**
   * Default constructor for the benefit of Kryo serialization.  Results in an invalid instance (Kryo will fill in the
   * fields with deserialized values after instantiation).
   */
  private LazyScalarProductVector() {
    this(null, 0);
  }

  @Override
  public Class<? extends Number> valueType() {
    return PrimitiveNumberTypes.productType(PrimitiveNumberTypes.smallestTypeForValue(_factor), getWrappedVector().valueType());
  }

  /**
   * Creates a vector that lazily multiplies a wrapped vector by a scalar factor.
   *
   * @param vector the vector whose elements will be multiplied
   * @param factor the factor with which to multiply the elements
   */
  public LazyScalarProductVector(Vector vector, double factor) {
    super(vector);
    _factor = factor;
  }

  @Override
  protected double compute(long index, double value) {
    return value * _factor;
  }
}
