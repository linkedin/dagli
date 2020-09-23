package com.linkedin.dagli.evaluation;

import com.linkedin.dagli.annotation.struct.Optional;
import com.linkedin.dagli.annotation.struct.Struct;
import com.linkedin.dagli.annotation.struct.VirtualField;
import java.io.Serializable;


/**
 * Represents a confusion matrix for a binary problem.  This is essentially a specialization of
 * {@link MultinomialEvaluationResult}, but this @Struct does not extend that one.
 *
 * Only aggregate weights are recorded (e.g. allowing for some examples to contribute more or less to the total than
 * others).  Confusion matrices for unweighted examples correspond to each example having a weight of 1, in which case
 * the sum weight will be the same as the count.
 */
@Struct("BinaryConfusionMatrix")
class BinaryConfusionMatrixBase implements Serializable {
  private static final long serialVersionUID = 1L;

  /**
   * The sum weight of the examples with the label "true" that were labeled correctly by the classifier.
   */
  double _truePositiveWeight;

  /**
   * The sum weight of the examples with the label "false" that were incorrectly labeled as "true" by the classifier.
   */
  double _falsePositiveWeight;

  /**
   * The sum weight of the examples with the label "false" that were labeled correctly by the classifier.
   */
  double _trueNegativeWeight;

  /**
   * The sum weight of the examples with the label "true" that were incorrectly labeled as "false" by the classifier.
   */
  double _falseNegativeWeight;

  /**
   * Where applicable, the decision threshold used to determine whether the "score" outputted by the classifier was
   * a positive or negative.  If not applicable, unknown or irrelevant, the value will be {@link Double#NaN}.
   *
   * All examples with predicted scores strictly less than the decision threshold are considered to be predicted as
   * negative.  All examples with scores greater than or equal to the threshold are considered to be predicted as
   * positive.
   */
  @Optional
  double _decisionThreshold = Double.NaN;

  /**
   * @return the total weight of all examples
   */
  @VirtualField("TotalWeight")
  public double getTotalWeight() {
    return getPositiveWeight() + getNegativeWeight();
  }

  /**
   * @return the total weight of all examples where the prediction matched the true label
   */
  @VirtualField("CorrectWeight")
  public double getCorrectWeight() {
    return _truePositiveWeight + _trueNegativeWeight;
  }

  /**
   * @return the total weight of all examples that are actually positive (regardless of the classifier's prediction)
   */
  @VirtualField("PositiveWeight")
  public double getPositiveWeight() {
    return _truePositiveWeight + _falseNegativeWeight;
  }

  /**
   * @return the total weight of all examples that are actually negative (regardless of the classifier's prediction)
   */
  @VirtualField("NegativeWeight")
  public double getNegativeWeight() {
    return _trueNegativeWeight + _falsePositiveWeight;
  }

  /**
   * @return the total weight of all examples that are predicted to be positive
   */
  @VirtualField("PredictedPositiveWeight")
  public double getPredictedPositiveWeight() {
    return _truePositiveWeight + _falsePositiveWeight;
  }

  /**
   * @return the total weight of all examples that are predicted to be negative
   */
  @VirtualField("PredictedNegativeWeight")
  public double getPredictedNegativeWeight() {
    return _trueNegativeWeight + _falseNegativeWeight;
  }

  /**
   * @return the (weighted) recall, AKA the "true positive rate".  (weight of true positives) / (weight of positives)
   */
  @VirtualField("Recall")
  public double getRecall() {
    return _truePositiveWeight / getPositiveWeight();
  }

  /**
   * @return the (weighted) true negative rate.  (weight of true negatives) / (weight of negatives)
   */
  @VirtualField("TrueNegativeRate")
  public double getTrueNegativeRate() {
      return _trueNegativeWeight / getNegativeWeight();
  }

  /**
   * @return the (weighted) false positive rate.  (weight of false positives) / (weight of negatives).  Also equal to
   *         1 - weightedTrueNegativeRate.
   */
  @VirtualField("FalsePositiveRate")
  public double getFalsePositiveRate() {
    return _falsePositiveWeight / getNegativeWeight();
  }

  /**
   * Calculates the weighted precision, canonically defined as 1.0 when no examples are predicted as positive.
   *
   * The canonical value of 1.0 is relevant when constructing precision-recall graphs (where there will exist a point
   * with recall of 0.0 and precision of 1.0).
   *
   * @return the (weighted) precision.  (weight of true positives) /
   *         (weight of false positives + weight of true positives)
   */
  @VirtualField("Precision")
  public double getPrecision() {
    double predictedPositives = getPredictedPositiveWeight();
    return predictedPositives == 0 ? 1.0 : _truePositiveWeight / predictedPositives;
  }

  /**
   * @return the (weighted) accuracy.  (weight of examples the classifier predicted correctly) / (total example weight)
   */
  @VirtualField("Accuracy")
  public double getAccuracy() {
    return getCorrectWeight() / getTotalWeight();
  }

  /**
   * @return the (weighted) F1-score, the harmonic mean of recall and precision:
   *         2 * (recall * precision) / (recall + precision)
   */
  @VirtualField("F1Score")
  public double getF1Score() {
    double recall = getRecall();
    double precision = getPrecision();
    return 2 * (recall * precision) / (recall + precision);
  }

  @VirtualField("Summary")
  public String getSummary() {
    return
        (Double.isNaN(_decisionThreshold) ? "" : "Decision threshold = " + _decisionThreshold + "\n")
        + "True Positives = " + _truePositiveWeight + ", False Positives = " + _falsePositiveWeight + "\n"
        + "False Negatives = " + _falseNegativeWeight + ", True Negatives = " + _trueNegativeWeight + "\n"
        + "Precision = " + getPrecision() + " @ " + "Recall = " + getRecall() + ", F1 = " + getF1Score() + "\n"
        + "Accuracy = " + getAccuracy() + " (accuracy for baseline that predicts the mode label = "
        + (Math.max(getPositiveWeight(), getNegativeWeight()) / getTotalWeight()) + ")";
  }
}
