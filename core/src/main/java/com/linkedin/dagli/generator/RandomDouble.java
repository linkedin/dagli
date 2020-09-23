package com.linkedin.dagli.generator;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.hashing.DoubleXorShift;
import com.linkedin.dagli.math.hashing.StatelessRNG;
import java.util.Objects;


/**
 * Samples a [0, 1) double floating point value uniformly at (pseudo)random (the standard uniform distribution).  The
 * actual sequence of values drawn is deterministically dependent upon the random number generator and seed value,
 * allowing for repeatable experiments.  Note that the sequence of values produced will be the same when both preparing
 * the DAG and every time it is later applied; consequently, {@link RandomDouble} is typically used during preparation
 * and its value ignored within the prepared DAG.
 *
 * A (probably unique) default seed is set when the instance is created; this means that, if you create this instance
 * multiple times within otherwise identical DAGs (e.g. by running your Java program multiple times) the produced
 * random sequences will be different--use the {@link #withSeed(long)} method to ensure a consistent sequence.
 */
@ValueEquality
public class RandomDouble extends AbstractGenerator<Double, RandomDouble> {
  private static final long serialVersionUID = 1;

  private StatelessRNG _rng;

  /**
   * Creates a new instance that will use a (probably) unique seed.
   */
  public RandomDouble() {
    // create a seed by hashing this instance's UUID
    java.util.UUID handle = this.getHandle().getUUID();
    long seed = DoubleXorShift.hashWithDefaultSeed(
        handle.getMostSignificantBits() + DoubleXorShift.hashWithDefaultSeed(handle.getLeastSignificantBits()));

    _rng = new DoubleXorShift(seed);
  }

  @Override
  public void validate() {
    Objects.requireNonNull(_rng, "No random number generator has been provided");
  }

  /**
   * Returns a copy of this instance that will use the provided stateless random number generator.
   *
   * The default random number generator is {@link DoubleXorShift}.
   *
   * @param rng the random number generator to use
   * @return a copy of this instance that will use the provided random number generator
   */
  public RandomDouble withRandomNumberGenerator(StatelessRNG rng) {
    return clone(c -> c._rng = Objects.requireNonNull(rng));
  }

  /**
   * Returns a copy of this instance that will use the provided seed value for its random number generator.  Different
   * seed values will yield different pseudorandom sequences of values.
   *
   * The default seed value is 0.
   *
   * @param seed the seed value to use
   * @return a copy of this instance that will use the provided seed value
   */
  public RandomDouble withSeed(long seed) {
    return clone(c -> c._rng = c._rng.withSeed(seed));
  }

  @Override
  public Double generate(long index) {
    return _rng.hashToDouble(index);
  }
}

