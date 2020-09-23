package com.linkedin.dagli.distribution;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.distribution.BinaryDistribution;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;


/**
 * Creates a binary distribution from a single input probability (which must be in the range [0, 1]).
 */
@ValueEquality
public class BinaryDistributionFromProbability extends AbstractPreparedTransformer1WithInput<
    Number, BinaryDistribution, BinaryDistributionFromProbability> {

  private static final long serialVersionUID = 1;

  private boolean _clippedOutOfRangeProbabilities = false;

  /**
   * Returns a copy of this instance that will not <strong>not</strong> throw an {@link IllegalArgumentException}
   * exception if the input probability is outside the range [0, 1], and will rather clip the value to 1 if the input is
   * greater than 1, and 0 otherwise (note that this means that NaN will correspond to a probability of 0).
   *
   * @return a copy of this instance that will clip out-of-range "probability" inputs
   */
  public BinaryDistributionFromProbability withClippedOutOfRangeProbabilities() {
    return clone(c -> c._clippedOutOfRangeProbabilities = true);
  }

  /**
   * Returns a copy of this instance that will create a binary distribution from the probability provided by the
   * specified input.  This probability must be in the range [0, 1] (unless
   * {@link #withClippedOutOfRangeProbabilities()} is used).
   *
   * @param probabilityInput the {@link Producer} providing the probability inputs to be transformed into binary
   *                         distributions
   * @return a copy of this instance that will create distributions from the probabilities provided by the specified
   *         input
   */
  public BinaryDistributionFromProbability withInput(Producer<? extends Number> probabilityInput) {
    return super.withInput1(probabilityInput);
  }

  @Override
  public BinaryDistribution apply(Number probabilityNumber) {
    double probability = probabilityNumber.doubleValue();

    if (_clippedOutOfRangeProbabilities) {
      if (probability > 1) {
        probability = 1;
      } else if (!(probability >= 0)) { // not exactly the same as probability < 0 due to NaNs
        probability = 0;
      }
    }

    return new BinaryDistribution(probability);
  }
}

