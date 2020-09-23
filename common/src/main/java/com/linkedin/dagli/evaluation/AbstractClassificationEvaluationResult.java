package com.linkedin.dagli.evaluation;

import com.linkedin.dagli.annotation.struct.VirtualField;
import java.io.Serializable;


/**
 * Base class providing common properties and methods for evaluation of classification results.  Some problems, like
 * ranking, may also derive from this class as they may be interpretable as classifiers.
 */
public abstract class AbstractClassificationEvaluationResult implements Serializable {
  private static final long serialVersionUID = 1;

  /**
   * The sum weight of all correct examples.
   */
  double _correctWeight = 0;

  /**
   * The sum weight of all examples.
   */
  double _totalWeight = 0;

  /**
   * The number of correct examples.
   */
  long _correctCount = 0;

  /**
   * The number of examples.
   */
  long _totalCount = 0;

  /**
   * The weighted accuracy (correctWeight / totalWeight).
   */
  @VirtualField("WeightedAccuracy")
  public double getWeightedAccuracy() {
    return _correctWeight / _totalWeight;
  }

  /**
   * The unweighted accuracy (correctCount / totalCount).
   */
  @VirtualField("UnweightedAccuracy")
  public double getUnweightedAccuracy() {
    return ((double) _correctCount) / _totalCount;
  }

  /**
   * Gets a summary of the results as a displayable string.
   *
   * @return a string summarizing the results
   */
  String getSummary() {
    return "Total example count: " + _totalCount + "\n"
        + "Total example weight: " + _totalWeight + "\n"
        + "Correct example count: " + _correctCount + "\n"
        + "Correct example weight: " + _correctWeight + "\n"
        + "Unweighted accuracy: " + getUnweightedAccuracy() + "\n"
        + "Weighted accuracy: " + getWeightedAccuracy();
  }
}
