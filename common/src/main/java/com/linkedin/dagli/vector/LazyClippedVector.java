package com.linkedin.dagli.vector;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import com.linkedin.dagli.util.invariant.Arguments;


/**
 * Produces a lazily-clipped vector (see {@link Vector#lazyClip(double, double)}) from the input vector.  The clipping
 * range must include 0.
 */
@ValueEquality
public class LazyClippedVector extends AbstractPreparedTransformer1WithInput<Vector, Vector, LazyClippedVector> {
  private static final long serialVersionUID = 1;

  private double _min = Double.NEGATIVE_INFINITY;
  private double _max = Double.POSITIVE_INFINITY;

  /**
   * Returns a copy of this instance that will clip with the specified minimum value.
   *
   * The default minimum is {@link Double#NEGATIVE_INFINITY}.
   *
   * @param min the minimum value (must be <= 0); values in the input vector less than this value will be clipped to be
   *            this minimum
   * @return a copy of this instance that will clip with the specified minimum value
   */
  public LazyClippedVector withMinimumValue(double min) {
    Arguments.check(min <= 0, "Clipping minimum must be <= 0");
    return clone(c -> c._min = min);
  }

  /**
   * Returns a copy of this instance that will clip with the specified maximum value.
   *
   * The default maximum is {@link Double#POSITIVE_INFINITY}.
   *
   * @param max the maximum value (must be >= 0); values in the input vector greater than this value will be clipped to
   *            be this maximum
   * @return a copy of this instance that will clip with the specified maximum value
   */
  public LazyClippedVector withMaximumValue(double max) {
    Arguments.check(max >= 0, "Clipping maximum must be >= 0");
    return clone(c -> c._max = max);
  }

  @Override
  public Vector apply(Vector vec) {
    return vec.lazyClip(_min, _max);
  }
}
