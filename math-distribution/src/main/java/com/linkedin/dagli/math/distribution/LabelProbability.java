package com.linkedin.dagli.math.distribution;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;


/**
 * A label with its associated probability.  Labels may be null.
 *
 * @param <L> The type of the label.
 */
public final class LabelProbability<L> implements Serializable {
  private static final long serialVersionUID = 3;

  /**
   * A comparator that compares LabelProbabilities by their probabilities *only*.  The label of each entry is not a
   * factor, and two entries with the same probability will be considered equal.
   */
  public static final Comparator<LabelProbability<?>> PROBABILITY_ORDER =
      Comparator.comparingDouble(LabelProbability::getProbability);

  private final L _label;
  private final double _probability;

  /**
   * Gets the label.  This label may be null.
   *
   * @return the label
   */
  public L getLabel() {
    return _label;
  }

  /**
   * Gets the probability of the label.
   *
   * @return the probability
   */
  public double getProbability() {
    return _probability;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof LabelProbability) {
      LabelProbability otherLabelProbability = (LabelProbability) other;
      return Objects.equals(getLabel(), otherLabelProbability.getLabel())
          && getProbability() == otherLabelProbability.getProbability();
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return hashCode(_label, _probability);
  }

  /**
   * Computes the hashCode of a given label and probability pair.  Using this method to compute the hash can be used
   * to ensure that a hash consistent with {@link LabelProbability#hashCode()} can be computed without instantiating a
   * {@link LabelProbability} object.
   *
   * @param label the label of the entry; may be null
   * @param probability the probability associated with the label
   * @param <T> the type of the label
   * @return the hash code for the label/probability pair
   */
  static <T> int hashCode(T label, double probability) {
    return Objects.hashCode(label) ^ (Double.hashCode(probability) << 1);
  }

  @Override
  public String toString() {
    return Objects.toString(getLabel()) + ": " + Double.toString(getProbability());
  }

  /**
   * Creates a new LabelProbability
   *
   * @param label the label (may be null)
   * @param probability the probability
   */
  public LabelProbability(L label, double probability) {
    _label = label;
    _probability = probability;
  }

  /**
   * Private no-args constructor specifically for the benefit of Kryo
   */
  private LabelProbability() {
    this(null, 0);
  }

  /**
   * Convenience method that transforms the label and returns the new LabelProbability
   *
   * @param labelMapper the function that performs the transformation
   * @param <R> the new label type
   * @return a new LabelProbability with the same probability and score, and transformed label
   */
  public <R> LabelProbability<R> mapLabel(Function<? super L, ? extends R> labelMapper) {
    return new LabelProbability<>(labelMapper.apply(getLabel()), getProbability());
  }

  /**
   * Convenience method that transforms the probability and returns the new LabelProbability
   *
   * @param transformer the function that transforms the probability
   * @return a new LabelProbability with the same label and score, and transformed probability
   */
  public LabelProbability<L> mapProbability(DoubleUnaryOperator transformer) {
    return new LabelProbability<>(getLabel(), transformer.applyAsDouble(getProbability()));
  }
}

