package com.linkedin.dagli.math.vector;

/**
 * Lazily clips a vector's values; the clipping range must include 0 (to be consistent with the semantics of a vector--
 * otherwise there would be an infinite number of non-zero elements.)
 */
class LazyClippedVector extends LazyTransformedValueVector {
  private static final long serialVersionUID = 1;

  private final double _min;
  private final double _max;

  /**
   * Default constructor for the benefit of Kryo serialization.  Results in an invalid instance (Kryo will fill in the
   * fields with deserialized values after instantiation).
   */
  private LazyClippedVector() {
    this(null, 0, 0);
  }

  /**
   * Creates a new lazy clipping vector that lazily transforms the values of a wrapped vector
   *
   * @param wrapped the vector whose values will be transformed
   * @param min the minimum value for each clipped element; must be <= 0
   * @param max the maximum value for each clipped element; must be >= 0
   */
  public LazyClippedVector(Vector wrapped, double min, double max) {
    super(wrapped);

    if (min > 0) {
      throw new IllegalArgumentException("Minimum clipping value cannot be > 0");
    } else if (max < 0) {
      throw new IllegalArgumentException("Maximum clipping value cannot be < 0");
    }

    _min = min;
    _max = max;
  }

  @Override
  protected double compute(long index, double value) {
    if (value > _max) {
      return _max;
    } else if (value < _min) {
      return _min;
    } else {
      return value; // note that value may be NaN
    }
  }

  @Override
  public Class<? extends Number> valueType() {
    return getWrappedVector().valueType();
  }
}
