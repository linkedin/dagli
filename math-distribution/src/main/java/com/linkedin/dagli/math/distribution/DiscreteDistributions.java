package com.linkedin.dagli.math.distribution;

/**
 * Helper methods for manipulating {@link DiscreteDistribution}s.
 */
public abstract class DiscreteDistributions {
  private DiscreteDistributions() { }

  /**
   * Returns the empty discrete distribution singleton.  This is a distribution containing no entries.
   *
   * @param <T> an arbitrary type for the labels the empty distribution doesn't actually have
   * @return an empty discrete distribution singleton
   */
  public static <T> DiscreteDistribution<T> empty() {
    return EmptyDiscreteDistribution.get();
  }
}
