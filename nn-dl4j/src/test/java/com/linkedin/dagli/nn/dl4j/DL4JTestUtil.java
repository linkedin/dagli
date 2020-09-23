package com.linkedin.dagli.nn.dl4j;

import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import com.linkedin.dagli.math.distribution.LabelProbability;
import com.linkedin.dagli.util.collection.Iterables;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


public class DL4JTestUtil {
  private DL4JTestUtil() { }

  /**
   * Compares the error of a list of predictions against a baseline that simply predicts the mode of the true labels.
   * The returned value is calculated as (# mistakes) / (# of items that are not the mode).
   *
   * @param labels the true labels
   * @param predictions the predicted labels
   * @return the relative error (lower is better)
   */
  public static double errorRelativeToModeBaseline(List<?> labels, List<?> predictions) {
    Object mode = Iterables.mode(labels).get();
    long modeCount = labels.stream().filter(l -> Objects.equals(mode, l)).count();

    int mistakes = 0;
    for (int i = 0; i < predictions.size(); i++) {
      if (!Objects.equals(predictions.get(i), labels.get(i))) {
        mistakes++;
      }
    }

    return ((double) mistakes) / modeCount;
  }

  public static <T> List<T> mostLikelyLabels(Collection<DiscreteDistribution<T>> distributions) {
    return distributions.stream()
        .map(DiscreteDistribution::max)
        .map(Optional::get)
        .map(LabelProbability::getLabel)
        .collect(Collectors.toList());
  }
}
