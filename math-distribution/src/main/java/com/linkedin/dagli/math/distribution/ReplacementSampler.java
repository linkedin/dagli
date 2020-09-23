package com.linkedin.dagli.math.distribution;

import java.io.Serializable;
import java.util.Optional;


/**
 * Interface for samplers that draw a single value with replacement from a non-empty distribution.
 */
public interface ReplacementSampler<T> extends Serializable {
  /**
   * Samples a label from the discrete distribution with replacement using a provided [0, 1) random value.
   *
   * The probability of a label  being selected is proportional to the probability it is assigned in the distribution;
   * the probabilities in the distribution do not need to sum to 1.
   *
   * Although the {@code standardUniformRandom} argument is expected to be drawn from [0, 1), a value of 1.0 will be
   * treated as if it were a value arbitrarily close to 1.0 (i.e. 0.999999....); consequently, values drawn from [0, 1]
   * may also be used without issue.
   *
   * @param standardUniformRandom a [0, 1) random value drawn uniformly at random by your favorite random number
   *                              generator
   * @return an {@link Optional} containing the sampled label, or nothing (empty) if the distribution has no labels with
   *         non-zero probability
   */
  Optional<T> sample(double standardUniformRandom);
}
