package com.linkedin.dagli.math.distribution;

import java.util.Optional;
import java.util.stream.Stream;


/**
 * Implements a Bernoulli distribution with minimal memory footprint.  Canonically the two binary events are labeled
 * as Booleans, TRUE and FALSE.
 *
 * Note that, like all {@link DiscreteDistribution}s, {@link #stream()} and other iterators include only non-zero
 * probability events.  This means that iterating over the {@link BinaryDistribution}'s {@link LabelProbability} entries
 * may yield two entries (if {@code 0 < P(TRUE) < 1}) or just one (if P(TRUE) == 1 or P(TRUE) == 0).
 */
public class BinaryDistribution extends AbstractDiscreteDistribution<Boolean> {
  private static final long serialVersionUID = 1;

  private final double _probability;

  /**
   * Creates a new Bernoulli distribution where TRUE has the specified probability.
   *
   * @param probability the probability of TRUE
   */
  public BinaryDistribution(double probability) {
    if (probability < 0 || probability > 1) {
      throw new IllegalArgumentException("Probability must be in the range [0, 1]; provided value was " + probability);
    }

    _probability = probability;
  }

  /**
   * Private no-args constructor specifically for the benefit of Kryo
   */
  private BinaryDistribution() {
    _probability = 0;
  }

  @Override
  public double get(Boolean label) {
    return label ? _probability : 1 - _probability;
  }

  @Override
  public Optional<LabelProbability<Boolean>> max() {
    if (_probability >= 0.5) {
      return Optional.of(new LabelProbability<>(true, _probability));
    } else {
      return Optional.of(new LabelProbability<>(false, 1 - _probability));
    }
  }

  @Override
  public long size64() {
    return _probability == 1 || _probability == 0 ? 1 : 2;
  }

  @Override
  public Stream<LabelProbability<Boolean>> stream() {
    LabelProbability<Boolean> trueLP = new LabelProbability<>(true, _probability);
    LabelProbability<Boolean> falseLP = new LabelProbability<>(false, 1 - _probability);

    if (_probability == 1) { // handle special case where only one event is returned
      return Stream.of(trueLP);
    } else if (_probability == 0) { // handle special case where only one event is returned
      return Stream.of(falseLP);
    } else if (_probability >= 0.5) {
      return Stream.of(trueLP, falseLP);
    } else {
      return Stream.of(falseLP, trueLP);
    }
  }
}
