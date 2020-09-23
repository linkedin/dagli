package com.linkedin.dagli.distribution;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import com.linkedin.dagli.math.distribution.LabelProbability;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;


/**
 * Gets the most likely label from a distribution (the label with the highest probability).  Ties are broken
 * arbitrarily.  By default, an exception is thrown is the distribution is empty, but you may specify an alternate
 * label be returned instead in this case.
 *
 * @param <T> the type of label in the distribution
 */
@ValueEquality
public class MostLikelyLabelFromDistribution<T>
    extends AbstractPreparedTransformer1WithInput<DiscreteDistribution<T>, T, MostLikelyLabelFromDistribution<T>> {

  private static final long serialVersionUID = 1;

  private boolean _throwIfMissing;
  private T _defaultLabel;

  /**
   * Creates an instance that throws a NoSuchElementException if an empty distribution is provided as input.
   */
  public MostLikelyLabelFromDistribution() {
    this(MissingInput.get(), true, null);
  }

  /**
   * Creates an instance with the specified distribution as input; this instance will throw a NoSuchElementException
   * if an empty distribution is later provided as input.
   */
  public MostLikelyLabelFromDistribution(Producer<? extends DiscreteDistribution<T>> distributionInput) {
    this(distributionInput, true, null);
  }

  /**
   * Creates an instance with the specified throwing behavior and default label.
   *
   * @param throwIfMissing if true, NoSuchElementException will be thrown when the transformer is applied on an
   *                       empty distribution.
   * @param defaultLabel the default label to be returned if the transformer is applied to an empty distribution.  Note
   *                     that this value is moot if throwIfMissing is true.
   */
  private MostLikelyLabelFromDistribution(Producer<? extends DiscreteDistribution<T>> distributionInput,
      boolean throwIfMissing, T defaultLabel) {
    super(distributionInput);
    _throwIfMissing = throwIfMissing;
    _defaultLabel = defaultLabel;
  }

  /**
   * Creates a copy of this instance that returns the specified defaultLabel when an empty distribution is provided
   * as input.
   *
   * @param defaultLabel the default label to be returned (may be null)
   * @return a copy of this instance with the specified default label
   */
  public MostLikelyLabelFromDistribution<T> withDefaultLabel(T defaultLabel) {
    return clone(c -> {
      c._throwIfMissing = false;
      c._defaultLabel = defaultLabel;
    });
  }

  @Override
  public T apply(DiscreteDistribution<T> valA) {
    if (_throwIfMissing) {
      return valA.max().get().getLabel();
    } else {
      return valA.max().map(LabelProbability::getLabel).orElse(_defaultLabel);
    }
  }
}
