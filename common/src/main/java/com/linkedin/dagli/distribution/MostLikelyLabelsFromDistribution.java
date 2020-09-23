package com.linkedin.dagli.distribution;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import com.linkedin.dagli.math.distribution.LabelProbability;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Gets the most likely labels from a distribution (the labels with the highest probability), ordered by decreasing
 * probability.  Ties are broken arbitrarily.
 *
 * @param <L> the type of label in the distribution
 */
@ValueEquality
public class MostLikelyLabelsFromDistribution<L> extends
    AbstractPreparedTransformer1WithInput<DiscreteDistribution<? extends L>, List<L>, MostLikelyLabelsFromDistribution<L>> {

  private static final long serialVersionUID = 1;

  private int _limit = Integer.MAX_VALUE;

  /**
   * Creates a new instance with no initial input.
   */
  public MostLikelyLabelsFromDistribution() { }

  /**
   * Creates a new instance that will accept the provided input.
   *
   * @param input an input providing the {@link DiscreteDistribution}s whose labels are to be extracted
   */
  public MostLikelyLabelsFromDistribution(Producer<? extends DiscreteDistribution<? extends L>> input) {
    super(input);
  }

  /**
   * Gets the maximum number of labels that will be returned.  By default this is Integer.MAX_VALUE.
   *
   * @return the maximum number of labels to return
   */
  public long getLimit() {
    return _limit;
  }

  /**
   * Sets the limit on the maximum number of labels to return.  By default this is Integer.MAX_VALUE.
   *
   * @param limit the limit to enforce.
   * @return a copy of this instance with the specified limit
   */
  public MostLikelyLabelsFromDistribution<L> withLimit(int limit) {
    return clone(c -> c._limit = limit);
  }

  @Override
  public List<L> apply(DiscreteDistribution<? extends L> dd) {
    return dd.stream().map(LabelProbability::getLabel).collect(Collectors.toList());
  }
}
