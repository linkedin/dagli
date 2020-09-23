package com.linkedin.dagli.distribution;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.generator.RandomDouble;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import com.linkedin.dagli.math.hashing.StatelessRNG;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer2;
import java.util.NoSuchElementException;


/**
 * Samples label from {@link DiscreteDistribution}s with replacement.  The probability of each label being selected
 * will be proportional to the probability it is assigned in the distribution; the probabilities in the distributions do
 * not need to sum to 1.
 *
 * The actual pseudorandom sequence of values drawn is deterministically dependent upon the standard uniform random
 * values ([0, 1) doubles drawn uniformly at random) provided as an input (by default, a new {@link RandomDouble} is
 * used).
 *
 * By default, providing an empty (no non-zero probability events) {@link DiscreteDistribution} as input will cause a
 * {@link java.util.NoSuchElementException}.   However, {@link #withDefaultSample(Object)} may be used to provide a
 * default label that will be "sampled" if the input distribution is empty.
 */
@ValueEquality
public class SampledWithReplacement<T>
    extends AbstractPreparedTransformer2<DiscreteDistribution<? extends T>, Double, T, SampledWithReplacement<T>> {
  private static final long serialVersionUID = 1;

  private boolean _throwIfEmpty = true;
  private T _defaultLabel = null;

  /**
   * Default constructor; a distribution must be supplied by {@link #withDistribution(DiscreteDistribution)}.
   */
  public SampledWithReplacement() {
    super();
    _input2 = new RandomDouble();
  }

  /**
   * Creates an instance that will sample with replacement from the provided distribution.
   *
   * @param distribution the distribution from which to sample
   */
  public SampledWithReplacement(DiscreteDistribution<? extends T> distribution) {
    this();
    _input1 = new Constant<>(distribution);
  }

  /**
   * Creates a copy of this instance that produces the specified default label when an empty distribution is provided
   * as input.
   *
   * By default, if this method is not used to provide a default label, an exception will be thrown when an empty
   * distribution is encountered.
   *
   * @param defaultLabel the default label to be returned (may be null)
   * @return a copy of this instance with the specified default label
   */
  public SampledWithReplacement<T> withDefaultSample(T defaultLabel) {
    return clone(c -> {
      c._throwIfEmpty = false;
      c._defaultLabel = defaultLabel;
    });
  }

  /**
   * Returns a copy of this instance that will sample with replacement from the provided discrete distribution.
   *
   * @param distribution the distribution from which to sample
   * @return a copy of this instance that will sample with replacement from the provided discrete distribution
   */
  public SampledWithReplacement<T> withDistribution(DiscreteDistribution<? extends T> distribution) {
    return withDistributionInput(new Constant<>(distribution));
  }

  /**
   * Returns a copy of this instance that will sample with replacement from the discrete distributions provided by
   * the given input.
   *
   * @param distributionInput the producer providing the distribution from which to sample
   * @return a copy of this instance that will sample with replacement from the provided discrete distribution
   */
  public SampledWithReplacement<T> withDistributionInput(
      Producer<? extends DiscreteDistribution<? extends T>> distributionInput) {
    return withInput1(distributionInput);
  }

  /**
   * Returns a copy of this instance that will use the provided stateless random number generator.
   *
   * By default, random numbers will be provided by {@link RandomDouble}.
   *
   * @param rng the random number generator to use
   * @return a copy of this instance that will use the provided random number generator
   */
  public SampledWithReplacement<T> withRandomNumberGenerator(StatelessRNG rng) {
    return withRandomNumberInput(new RandomDouble().withRandomNumberGenerator(rng));
  }

  /**
   * Returns a copy of this instance that will use random numbers provided by the given input.  The provided values must
   * be [0, 1) and should be uniformly distributed.
   *
   * By default, random numbers will be provided by {@link RandomDouble}.
   *
   * @param randomNumberInput a provider a [0, 1) uniformly distributed values
   * @return a copy of this instance that will use the provided random number generator
   */
  public SampledWithReplacement<T> withRandomNumberInput(Producer<Double> randomNumberInput) {
    return withInput2(randomNumberInput);
  }

  @Override
  @SuppressWarnings("unchecked") // it's safe to treat DiscreteDistribution<? extends T> as DD<T> in this context
  public T apply(DiscreteDistribution<? extends T> distribution, Double uniformRandom) {
    return _throwIfEmpty ? distribution.sample(uniformRandom)
        .orElseThrow(() -> new NoSuchElementException("Cannot sample from an empty distribution"))
        : ((DiscreteDistribution<T>) distribution).sample(uniformRandom).orElse(_defaultLabel);
  }
}

