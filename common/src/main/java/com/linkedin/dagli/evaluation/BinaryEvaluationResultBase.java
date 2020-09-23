package com.linkedin.dagli.evaluation;

import com.linkedin.dagli.annotation.struct.Struct;
import com.linkedin.dagli.annotation.struct.VirtualField;
import com.linkedin.dagli.util.closeable.Closeables;
import it.unimi.dsi.fastutil.BigList;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.ToDoubleFunction;


/**
 * Evaluation of a binary classifier where true/false (positive/negative) predictions are determined by comparing the
 * "score" (e.g. a probability in the case of logistic regression, or an arbitrary real value in the case of perceptron)
 * to a decision threshold.
 *
 * Binary evaluations implement {@link Comparable} according to their ROC AUCs.
 *
 * Note that an {@link BinaryEvaluationResult} is a relatively memory-intensive object because it contains confusion
 * matrices at all possible thresholds.  If there are a billion distinct scores, this is a high number of possible
 * thresholds and thus a high number of confusion matrices!
 */
@Struct("BinaryEvaluationResult")
class BinaryEvaluationResultBase implements Comparable<BinaryEvaluationResultBase>, Serializable {
  private static final long serialVersionUID = 1L;

  /**
   * The list, ordered by hypothetical threshold, of all confusion matrices.  The last confusion matrix will always
   * correspond to a threshold of positive infinity.
   */
  BigList<BinaryConfusionMatrix> _confusionMatrices;

  /**
   * Gets the confusion matrix corresponding to a hypothetical decision threshold.  Thresholds are defined such that
   * an example with a score less than the threshold will be considered negative, and an example with a score greater
   * than or equal to the threshold will be positive.
   *
   * @param threshold the threshold whose corresponding binary confusion matrix is sought
   * @return the confusion matrix corresponding to the proposed decision threshold
   */
  BinaryConfusionMatrix getConfusionMatrixAtThreshold(double threshold) {
    if (Double.isNaN(threshold)) {
      throw new IllegalArgumentException("Threshold cannot be NaN");
    }
    long index = getConfusionMatrixIndexAtThreshold(_confusionMatrices, threshold);

    if (index >= _confusionMatrices.size64()) {
      // if this exception is thrown, this evaluation result is invalid (and/or there's a bug in this code)
      throw new IllegalStateException(
          "Unable to find confusion matrix corresponding to threshold " + threshold + "; this should never happen");
    }

    return _confusionMatrices.get(index).withDecisionThreshold(threshold);
  }

  private static long getConfusionMatrixIndexAtThreshold(BigList<BinaryConfusionMatrix> list, double threshold) {
    if (list.size64() < 16) {
      // few items remaining--just do a linear scan
      for (long i = 0; i < list.size64(); i++) {
        if (list.get(i).getDecisionThreshold() >= threshold) {
          return i;
        }
      }
      return list.size64();
    }

    long midpoint = list.size64() >> 1;
    if (threshold < list.get(midpoint).getDecisionThreshold()) {
      return getConfusionMatrixIndexAtThreshold(list.subList(0, midpoint), threshold);
    } else {
      return midpoint + getConfusionMatrixIndexAtThreshold(list.subList(midpoint, list.size64()), threshold);
    }
  }

  /**
   * @return the AUC (area under the ROC curve) calculated using the trapezoidal rule
   */
  @VirtualField("AUC")
  public double getAUC() {
    // ROC curve has recall (true positive rate) on the y-axis, false positive rate on the x-axis.
    return calculateAUC(_confusionMatrices, BinaryConfusionMatrix::getFalsePositiveRate,
        BinaryConfusionMatrix::getRecall);
  }

  /**
   * Calculates the Average Precision, which is in principle the area under the Precision-Recall curve and, in a
   * real-world, discrete setting, approximately so.
   *
   * We calculate Average Precision as \sum_i (Recall(i) - Recall(i - 1)) * Precision(i), where the i's enumerate
   * possible thresholds in decreasing order, with the first threshold being the highest that admits at least one
   * positive example.
   *
   * @return the Average Precision
   */
  @VirtualField("AveragePrecision")
  public double getAveragePrecision() {
    double averagePrecision = 0;

    // note that our confusion matrices are listed in order of *increasing* threshold and decreasing recall; we instead
    // iterate over the matrices in reversed order (with increasing recall and decreasing thresholds)
    double lastRecall = 0;
    for (long i = _confusionMatrices.size64() - 1; i >= 0; i--) {
      BinaryConfusionMatrix cm = _confusionMatrices.get(i);
      double nextRecall = cm.getRecall();
      averagePrecision += (nextRecall - lastRecall) * cm.getPrecision();
      lastRecall = nextRecall;
    }

    return averagePrecision;
  }

  /**
   * Calculates the area under a curve defined by a list of points using the trapezoidal rule.
   *
   * The area under a curve defined by less than two points is canonically defined as 0.
   *
   * @param orderedPoints a list of points ordered by their x-coordinate (either ascending or descending)
   * @param xValue a function retrieving the x-coordinate of a point
   * @param yValue a function retrieving the y-coordinate of a point
   * @param <T> the type of the point (can be arbitrary)
   * @return the area under the curve
   */
  private static <T> double calculateAUC(Iterable<T> orderedPoints, ToDoubleFunction<T> xValue,
      ToDoubleFunction<T> yValue) {
    Iterator<T> iterator = orderedPoints.iterator();
    try {
      if (!iterator.hasNext()) {
        return 0;
      }
      double area = 0;

      T nextPoint = iterator.next();
      double lastX = xValue.applyAsDouble(nextPoint);
      double lastY = yValue.applyAsDouble(nextPoint);

      while (iterator.hasNext()) {
        nextPoint = iterator.next();
        double nextX = xValue.applyAsDouble(nextPoint);
        double nextY = yValue.applyAsDouble(nextPoint);
        area += (nextY + lastY) * Math.abs(nextX - lastX) / 2;
        lastX = nextX;
        lastY = nextY;
      }

      return area;
    } finally {
      Closeables.tryClose(iterator);
    }
  }

  /**
   * @return a summary of the results, expressed as a string
   */
  @VirtualField("Summary")
  public String getSummary() {
    BinaryConfusionMatrix highestAccuracyCM =
        Collections.max(_confusionMatrices, Comparator.comparingDouble(BinaryConfusionMatrixBase::getAccuracy));

    BinaryConfusionMatrix highestF1CM =
        Collections.max(_confusionMatrices, Comparator.comparingDouble(BinaryConfusionMatrixBase::getF1Score));

    StringBuilder builder = new StringBuilder();
    builder.append("Highest accuracy = " + highestAccuracyCM.getAccuracy() + "\n");
    builder.append(highestAccuracyCM.getSummary() + "\n\n");

    builder.append("Highest F1-score = " + highestF1CM.getF1Score() + "\n");
    builder.append(highestF1CM.getSummary() + "\n\n");

    builder.append("ROC AUC = " + getAUC() + ", Average Precision = " + getAveragePrecision());

    return builder.toString();
  }

  @Override
  public int compareTo(BinaryEvaluationResultBase o) {
    return Double.compare(this.getAUC(), o.getAUC());
  }
}
