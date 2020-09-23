package com.linkedin.dagli.distribution;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer2;


/**
 * Simple transformer that gets the probability for a provided label from a provided distribution.
 */
@ValueEquality
public class LabelProbabilityFromDistribution<T>
    extends AbstractPreparedTransformer2<T, DiscreteDistribution<? super T>, Double, LabelProbabilityFromDistribution<T>> {
  private static final long serialVersionUID = 1;

  /**
   * Returns a copy of this instance that will obtain the probability for the given, fixed label from the inputted
   * distribution.
   *
   * @param label the label whose probability should be retrieved
   * @return a copy of this instance that will obtain the probability for the given, fixed label from the inputted
   *         distribution
   */
  public LabelProbabilityFromDistribution<T> withLabel(T label) {
    return withLabelInput(new Constant<>(label));
  }

  /**
   * Returns a copy of this instance that will obtain the probability for the label provided by the given input from
   * the distribution.
   *
   * @param labelInput the input providing the label whose probability should be retrieved
   * @return a copy of this instance that will obtain the probability for the label provided by the given input from
   *         the distribution
   */
  public LabelProbabilityFromDistribution<T> withLabelInput(Producer<? extends T> labelInput) {
    return withInput1(labelInput);
  }

  /**
   * Returns a copy of this instance that will look up label probabilities from the given, fixed distribution.
   *
   * @param distribution the distribution from which the label probability will be fetched
   * @return a copy of this instance that will look up label probabilities from the given, fixed distribution
   */
  public LabelProbabilityFromDistribution<T> withDistribution(DiscreteDistribution<? super T> distribution) {
    return withDistributionInput(new Constant<>(distribution));
  }

  /**
   * Returns a copy of this instance that will look up label probabilities from the distribution provided by the given
   * input.
   *
   * @param distributionInput an input providing the distribution from which the label probability will be fetched
   * @return a copy of this instance that will look up label probabilities from the distributions provided by the given
   *         input
   */
  public LabelProbabilityFromDistribution<T> withDistributionInput(
      Producer<? extends DiscreteDistribution<? super T>> distributionInput) {
    return withInput2(distributionInput);
  }

  @Override
  public Double apply(T label, DiscreteDistribution<? super T> distribution) {
    return distribution.get(label);
  }
}
