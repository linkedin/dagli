package com.linkedin.dagli.evaluation;

import com.linkedin.dagli.annotation.struct.Struct;
import com.linkedin.dagli.annotation.struct.VirtualField;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


/**
 * Represents an evaluation of multinomial classification.
 *
 * {@link MultinomialEvaluationResult}s are {@link Comparable} and are compared using their weighted accuracy.
 */
@Struct("MultinomialEvaluationResult")
class MultinomialEvaluationResultBase extends AbstractClassificationEvaluationResult
    implements Comparable<MultinomialEvaluationResult> {
  private static final long serialVersionUID = 1;

  /**
   * Defines the confusion matrix: gives the weight and counts of each actual/predicted label pair.
   */
  Map<ActualAndPredictedLabel, WeightAndCount> _actualAndPredictedLabelPairToWeightAndCountMap = null;

  /**
   * @return a map between each true label and its weight and count
   */
  @VirtualField("ActualLabelToWeightAndCountMap")
  public Map<Object, WeightAndCount> getActualLabelToWeightAndCountMap() {
    HashMap<Object, WeightAndCount> result = new HashMap<>();
    for (Map.Entry<ActualAndPredictedLabel, WeightAndCount> entry : _actualAndPredictedLabelPairToWeightAndCountMap.entrySet()) {
      WeightAndCount wc = result.computeIfAbsent(entry.getKey().getActualLabel(),
          lp -> WeightAndCount.Builder.setWeight(0).setCount(0).build());
      wc.addToThis(entry.getValue());
    }

    return result;
  }

  /**
   * @return a map between each predicted label and its weight and count
   */
  @VirtualField("PredictedLabelToWeightAndCountMap")
  public Map<Object, WeightAndCount> getPredictedLabelToWeightAndCountMap() {
    HashMap<Object, WeightAndCount> result = new HashMap<>();
    for (Map.Entry<ActualAndPredictedLabel, WeightAndCount> entry : _actualAndPredictedLabelPairToWeightAndCountMap.entrySet()) {
      WeightAndCount wc = result.computeIfAbsent(entry.getKey().getPredictedLabel(),
          lp -> WeightAndCount.Builder.setWeight(0).setCount(0).build());
      wc.addToThis(entry.getValue());
    }

    return result;
  }

  /**
   * @return a summary of the results, expressed as a string
   */
  @VirtualField("Summary")
  public String getSummary() {
    Optional<Map.Entry<Object, WeightAndCount>> max = getActualLabelToWeightAndCountMap().entrySet()
        .stream()
        .max((e1, e2) -> (int) Math.signum(e1.getValue()._weight - e2.getValue()._weight));

    String res = super.getSummary();
    if (max.isPresent()) {
      res += "\n[Sanity check] Weighted accuracy of just choosing the label with highest total weight: " + (
          max.get().getValue()._weight / _totalWeight);
    }

    return res;
  }

  @Override
  public int compareTo(MultinomialEvaluationResult o) {
    return Double.compare(getWeightedAccuracy(), o.getWeightedAccuracy());
  }
}
