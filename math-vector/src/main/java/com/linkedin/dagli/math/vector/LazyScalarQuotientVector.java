package com.linkedin.dagli.math.vector;

/**
 * Lazily divides all the elements of a vector by a scalar value.  Changes to the original vector affect the elements
 * of this one.
 */
class LazyScalarQuotientVector extends LazyTransformedValueVector {
  private static final long serialVersionUID = 1;

  private final double _divisor;

  /**
   * Default constructor for the benefit of Kryo serialization.  Results in an invalid instance (Kryo will fill in the
   * fields with deserialized values after instantiation).
   */
  private LazyScalarQuotientVector() {
    this(null, 0);
  }

  @Override
  public Class<? extends Number> valueType() {
    return double.class; // in the future, we could potentially return a smaller type depending on the value of _divisor
  }

  /**
   * Creates a vector that lazily divides a wrapped vector by a scalar divisor.
   *
   * @param vector the vector whose elements will be divided
   * @param divisor the divisor with which to divide the elements
   */
  public LazyScalarQuotientVector(Vector vector, double divisor) {
    super(vector);
    _divisor = divisor;
  }

  @Override
  protected double compute(long index, double value) {
    return value / _divisor;
  }
}
