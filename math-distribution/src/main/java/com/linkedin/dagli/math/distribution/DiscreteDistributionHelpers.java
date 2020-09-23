package com.linkedin.dagli.math.distribution;

import it.unimi.dsi.fastutil.doubles.DoubleIterator;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Helper methods for DiscreteDistributions.  Not for use by external callers.  Internal use only.
 */
abstract class DiscreteDistributionHelpers {
  private DiscreteDistributionHelpers() { }

  /**
   * "Deduplicates" a stream of {@link LabelProbability}s that may contain multiple entries with the same label.
   *
   * @param deduplication the method to use for the deduplication
   * @param labelProbabilityStream a stream of {@link LabelProbability} entries to deduplicate
   * @param <R> the type of the labels
   * @return a map of labels to their probabilities, with the probabilities assigned to a previously-duplicated label
   *         dependent upon the deduplication method chosen
   */
  private static <R> Object2DoubleMap<R> deduplicateStream(Deduplication deduplication,
      Stream<LabelProbability<R>> labelProbabilityStream) {
    switch (deduplication) {
      case NONE: {
        // collect the stream to a list
        List<LabelProbability<R>> labelProbabilities = labelProbabilityStream.collect(Collectors.toList());

        // verify the uniqueness of the labels in these entries
        assert
            labelProbabilities.stream().map(LabelProbability::getLabel).distinct().count() == labelProbabilities.size();

        // we masquerade the extracted array of keys (type Object[]) as R[] so we can pass to Object2DoubleFixedArrayMap
        return new Object2DoubleFixedArrayMap<>(
            (R[]) labelProbabilities.stream().map(LabelProbability::getLabel).toArray(),
            labelProbabilities.stream().mapToDouble(LabelProbability::getProbability).toArray());
      }
      case MERGE: {
        Object2DoubleOpenHashMap<R> result = new Object2DoubleOpenHashMap<>();
        labelProbabilityStream.forEach(lp -> result.addTo(lp.getLabel(), lp.getProbability()));
        return result;
      }
      case MAX: {
        Object2DoubleOpenHashMap<R> result = new Object2DoubleOpenHashMap<>();
        labelProbabilityStream.forEach(lp -> result.mergeDouble(lp.getLabel(), lp.getProbability(), Math::max));
        return result;
      }
      default:
        throw new IllegalArgumentException("Unknown deduplication scheme");
    }
  }

  /**
   * Sums all the values of a map efficiently and returns the result.
   *
   * @param map the map whose values will be summed
   * @return the sum of all values in the map
   */
  private static <R> double sumValues(Object2DoubleMap<R> map) {
    // we need to do this the old fashion way, with iterators, to avoid boxing double -> Double
    double probSum = 0;
    DoubleIterator valIterator = map.values().iterator();
    while (valIterator.hasNext()) {
      probSum += valIterator.nextDouble();
    }
    return probSum;
  }

  /**
   * Given the original distribution being modified, a stream of entries from the modified distribution,
   * and a renormalization scheme, produces a new, renormalized distribution.
   *
   * This method should only be used internally by DiscreteDistribution; the API is subject to change at any time.
   *
   * @param original the original distribution
   * @param labelProbabilityStream a stream of entries that derives from the original distribution
   * @param deduplication the way the stream of entries should be de-duplicated (if necessary)
   * @param renormalization the way the stream of entries should be (re)normalized
   * @param <R> the type of label of the distribution
   * @return a new, renormalized distribution created from the labelProbabilityStream
   */
  static <R> DiscreteDistribution<R> renormalizedStream(
      DiscreteDistribution<?> original,
      Stream<LabelProbability<R>> labelProbabilityStream,
      Deduplication deduplication,
      Renormalization renormalization) {

    // first, deduplicate to get a label-to-probability map:
    Object2DoubleMap<R> labelToProbabilityMap = deduplicateStream(deduplication, labelProbabilityStream);

    // now, renormalize as needed
    switch (renormalization) {
      case NONE:
        return new ArrayDiscreteDistribution<>(labelToProbabilityMap);
      case CONSTANT_SUM:
        final double originalProbabilitySum = original.probabilitySum();
        final double newProbabilitySum = sumValues(labelToProbabilityMap);
        final double multiplier = newProbabilitySum > 0 ? originalProbabilitySum / newProbabilitySum : 1;
        if (multiplier != 1) {
          for (Object2DoubleMap.Entry<R> entry : labelToProbabilityMap.object2DoubleEntrySet()) {
            entry.setValue(entry.getDoubleValue() * multiplier);
          }
        }
        return new ArrayDiscreteDistribution<>(labelToProbabilityMap);
      default:
        throw new IllegalArgumentException("Unknown renormalization scheme");
    }
  }
}
