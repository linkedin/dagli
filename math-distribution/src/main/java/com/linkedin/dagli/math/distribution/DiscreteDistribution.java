package com.linkedin.dagli.math.distribution;

import com.linkedin.dagli.util.invariant.Arguments;
import it.unimi.dsi.fastutil.Size64;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;


/**
 * Discrete distributions map labels (events) of a given type to real-valued probabilities.  Depending on the
 * application, these probabilities do not necessarily sum to 1, e.g. a "distribution" resulting from a multilabel
 * prediction, or a distribution with some events omitted.
 *
 * Distributions do not store events with 0 probability, and such events are ignored: they will not be returned by
 * iterators nor  reflected in the {@link #size64()}, etc.
 *
 * Probabilities less than 0 are not allowed, but degenerate "probabilities" greater than 1 are possible if the
 * implementation allows them.
 *
 * @param <T> the type of the label
 */
public interface DiscreteDistribution<T>
    extends Iterable<LabelProbability<T>>, Size64, Serializable, ReplacementSampler<T> {
  /**
   * Get the probability for a particular label.  Labels that do not have entries in the distribution have probability
   * 0.
   *
   * @param label the label whose probability should be sought; may be null, as null is a potential label
   * @return a [0, 1] probability
   */
  default double get(T label) {
    return stream().filter(labelProbability -> Objects.equals(labelProbability.getLabel(), label))
        .findFirst()
        .map(labelProbability -> labelProbability.getProbability())
        .orElse(0.0);
  }

  @Override
  default Optional<T> sample(double standardUniformRandom) {
    Arguments.check(standardUniformRandom >= 0, "Standard uniform random must be in the range [0, 1]");
    Arguments.check(standardUniformRandom <= 1, "Standard uniform random must be in the range [0, 1]");

    // naive algorithm for obtaining sample: walk through labels one-by-one
    double probabilityMassOffset = probabilitySum();
    if (probabilityMassOffset == 0) {
      return Optional.empty();
    }

    probabilityMassOffset *= standardUniformRandom;

    Iterator<LabelProbability<T>> iter = iterator();
    LabelProbability<T> lp = null;
    while (iter.hasNext()) {
      lp = iter.next();
      probabilityMassOffset -= lp.getProbability();
      if (probabilityMassOffset <= 0) {
        return Optional.of(lp.getLabel());
      }
    }

    // floating-point imprecision might mean we are able to reach this point; we just need to return the last label:
    return Optional.of(lp.getLabel());
  }

  /**
   * Gets the LabelProbability for the maximum-probability label.  If there are multiple such labels, ties are broken
   * arbitrarily.  As degenerate empty distributions (no events with non-zero probability) are possible, a max() may not
   * be present.
   *
   * @return A LabelProbability for a label with maximum probability.
   */
  default Optional<LabelProbability<T>> max() {
    return stream().findFirst();
  }

  /**
   * Convenience method that returns the most likely label in this distribution (if the distribution is non-empty).
   * Equivalent to {@code max().map(LabelProbability::getLabel)}.
   *
   * @return the most likely label
   */
  default Optional<T> mostLikelyLabel() {
    return max().map(LabelProbability::getLabel);
  }

  /**
   * Gets the number of LabelProbability entries in the distribution; i.e. the number of entries returned by stream().
   *
   * @return The number of entries that will be returned by stream().
   */
  @Override
  default long size64() {
    return stream().count();
  }

  /**
   * Streams over the LabelProbability entries in descending order of probability.  Events with 0 probability will not
   * be included.
   *
   * Ties amongst entries with the same probability are broken arbitrarily.
   *
   * @return The entries of this discrete distribution in descending order of probability.
   */
  Stream<LabelProbability<T>> stream();

  /**
   * Iterates over the LabelProbability entries in descending order of probability.  Events with 0 probability will not
   * be included.
   *
   * Ties amongst entries with the same probability may be broken arbitrarily.
   *
   * @return The entries of this discrete distribution in descending order of probability.
   */
  @Override
  default Iterator<LabelProbability<T>> iterator() {
    return stream().iterator();
  }

  /**
   * Returns the sum of the probabilities of all the entries in the distribution.  This may not equal 1 if:
   * (1) Some entries are omitted (e.g. it's common to keep only the highest-probability entries and "forget" the
   *     others).
   * (2) The "distribution" actually represents the probabilities of multiple independent events (in which case the
   *     total probability may exceed 1).
   *
   * @return the total probability of all entries in this distribution.
   */
  default double probabilitySum() {
    return stream().mapToDouble(LabelProbability::getProbability).sum();
  }

  /**
   * "Multiplies" this distribution with others.  The product distribution is defined as having probabilities that, for
   * each label, are the product of the probabilities of that label in all the multiplied distributions.  So, for
   * example, if we have three distributions:
   * <pre>{@code
   * A -> {"one" -> 0.5, "two" -> "0.5"}
   * B -> {"one" -> 0.1, "two" -> "0.5"}
   * C -> {"one" -> 0.5}
   * }</pre>
   *
   * Then A.multiply(B, C) results in the distribution {@code "one" -> 0.025}.  Notice that in distribution C the
   * probability of event "two" was implicitly zero, so its probability in the product distribution is thus also
   * necessarily zero.
   *
   * @param others the other distributions to multiply with this distribution; these distributions may have arbitrary
   *               label types, but events with labels not present in this distribution will not be included in the
   *               returned product distribution regardless so this doesn't pose a problem
   * @return a new product distribution, or this distribution (if no other distributions are provided)
   */
  @SuppressWarnings("unchecked") // unchecked cast within method body is safe
  default DiscreteDistribution<T> multiply(DiscreteDistribution<?>... others) {
    if (others.length == 0) {
      return this;
    }

    // start by putting all of our label-to-probability events in a map
    Object2DoubleOpenHashMap<T> initialProbabilities = new Object2DoubleOpenHashMap<>();
    this.forEach(lp -> initialProbabilities.put(lp.getLabel(), lp.getProbability()));

    // Stash our cumulative probabilities in an array.  We do this so the array (which is effectively final) can be
    // passed into a lambda, despite the fact that we'll be changing the value of its first and only element.  If we
    // tried to use the map directly (without the wrapping array acting as a second-level pointer) the code would not
    // compile.
    Object2DoubleOpenHashMap<T>[] cumulativeProbabilities = new Object2DoubleOpenHashMap[] { initialProbabilities };

    // now iterate through the other distributions, multiplying their probabilities with the cumulative product and
    // removing those events that don't appear (have zero probability) in the other distribution
    for (DiscreteDistribution<?> other : others) {
      Object2DoubleOpenHashMap<T> newProbabilities = new Object2DoubleOpenHashMap<>(cumulativeProbabilities[0].size());

      other.forEach(lp -> {
        double cumulativeProbability = cumulativeProbabilities[0].getOrDefault(lp.getLabel(), 0);
        if (cumulativeProbability > 0) {
          // if the label was already in cumulativeProbabilities, it must be of type T.  The (T) cast below is safe:
          newProbabilities.put((T) lp.getLabel(), cumulativeProbability * lp.getProbability());
        } // else this event is missing from one or more of the other multiplied distributions and should be ignored
      });

      // Replace the previous cumulative probability map with the new map--this is important because it means we
      // drop those cumulative probabilities corresponding to events/labels that were not in the "other"
      // distribution in this iteration (i.e. their probability was implicitly 0, so they should be omitted from the
      // product).
      cumulativeProbabilities[0] = newProbabilities;
    }

    return new ArrayDiscreteDistribution<>(cumulativeProbabilities[0]);
  }

  /**
   * "Multiplies" this distribution with others, while enforcing a minimum probability value for all events (note that
   * this may allow you to imply that your otherwise-valid multinomial distribution has events whose probabilities sum
   * to more than 1).  The product distribution is defined as having probabilities that, for
   * each label, are the product of the probabilities of that label in all the multiplied distributions.  So, for
   * example, if we have three distributions:
   * <pre>{@code
   * A -> {"one" -> 0.5, "two" -> "0.5"}
   * B -> {"one" -> 0.1, "two" -> "0.5"}
   * C -> {"one" -> 0.5}
   * }</pre>
   *
   * Then A.multiply(0.2, B, C) results in the distribution {@code {"one" -> 0.05, "two" -> 0.05}}.  Notice that in
   * distribution B, the probability of event "one" is taken to be 0.2 (and not 0.1), and in C the probability of event
   * "two" is also taken to be 0.2 despite not being present in the distribution.
   *
   * Note that the minimum probability value only applies for events that are present in at least one of the operands of
   * the multiplication; the product of the above multiplication will <strong>not</strong>, for example, have an event
   * "three" with probability 0.2^3.
   *
   * Due to the underlying implementation of this method, the total number of distinct keys across all multiplied
   * distributions should not exceed {@link Integer#MAX_VALUE}.  This limitation may be removed in the future.
   *
   * @param others the other distributions to multiply with this distribution
   * @param minimumEventProbability when computing the multiplication, any event with a probability less than
   *                                minimumEventProbability in a particular distribution, or that is missing from that
   *                                distribution entirely, will be considered to have probability
   *                                minimumEventProbability
   *
   * @return a new product distribution
   */
  default DiscreteDistribution<T> multiply(double minimumEventProbability, DiscreteDistribution<? extends T>... others) {
    class ProbabilityAndCount {
      double _product;
      int _count = 1;

      ProbabilityAndCount(double initialValue) {
        _product = initialValue;
      }

      ProbabilityAndCount multiply(double operand) {
        _product *= operand;
        _count++;
        return this;
      }
    }

    HashMap<T, ProbabilityAndCount> eventMap = new HashMap<>(Math.toIntExact(this.size64()));

    // start by putting all of this instance's events in a map
    this.forEach(lp -> eventMap.put(lp.getLabel(),
        new ProbabilityAndCount(Math.max(minimumEventProbability, lp.getProbability()))));

    // now update for all the other distributions
    for (DiscreteDistribution<? extends T> other : others) {
      other.forEach(lp -> eventMap.compute(lp.getLabel(),
          (k, v) -> v == null ? new ProbabilityAndCount(Math.max(minimumEventProbability, lp.getProbability()))
              : v.multiply(Math.max(minimumEventProbability, lp.getProbability()))));
    }

    int multiplicandCount = others.length + 1;
    // stores the multipliers corresponding to each number of "missed events", e.g. the number of distributions that
    // didn't include an explicit probability for that event
    double[] missingEventMultipliers = new double[multiplicandCount];
    missingEventMultipliers[0] = 1;
    for (int i = 1; i < missingEventMultipliers.length; i++) {
      missingEventMultipliers[i] = missingEventMultipliers[i - 1] * minimumEventProbability;
    }

    // now calculate the final probabilities and stick them in a map
    Object2DoubleOpenHashMap<T> probabilityMap = new Object2DoubleOpenHashMap<>(eventMap.size());

    // we've tracked the number of distributions that had explicit probabilities for each event
    // (ProbabilityAndCount::_count) so we can now use this information to account for those distributions that did not,
    // as each such distribution implicitly assigned "missing" events the probability minimumEventProbability
    eventMap.forEach(
        (k, v) -> probabilityMap.put(k, v._product * missingEventMultipliers[multiplicandCount - v._count]));

    return new ArrayDiscreteDistribution<T>(probabilityMap);
  }

  /**
   * Normalizes a distribution by scaling the probabilities of its constituent entries such that they sum to 1.0.  Due
   * to the imprecision of floating-point math, the sum may not be <b>exactly</b> 1.0 and, moreover, this method is not
   * idempotent: normalizing the same distribution repeatedly may result in very slightly different distributions each
   * time.
   *
   * As a special case, normalizing an empty distribution (containing no events) will result in another empty
   * distribution.
   *
   * @return a distribution based on this one, with its probabilities scaled to sum to 1.0, or this distribution if
   *         its entries already sum to 1.0.
   */
  default DiscreteDistribution<T> normalize() {
    return normalize(1.0);
  }

  /**
   * Normalizes a distribution by scaling the probabilities of its constituent entries such that they sum to a specified
   * value.  Due to the imprecision of floating-point math, the sum may not be <b>exactly</b> that specified and,
   * moreover, this method is not idempotent: normalizing the same distribution repeatedly may result in very slightly
   * different distributions each time.
   *
   * As a special case, normalizing an empty distribution (containing no events) will result in an empty
   * distribution, as will normalizing with a newProbabilitySum of 0.
   *
   * @param newProbabilitySum a non-negative value; entries in the distribution will be scaled such that they sum to
   *                          this quantity (modulo floating-point imprecision).
   * @return a distribution based on this one, with its probabilities scaled to sum to newProbabilitySum, or this
   *         distribution if it already has the desired sum
   */
  default DiscreteDistribution<T> normalize(double newProbabilitySum) {
    if (newProbabilitySum < 0) {
      throw new IllegalArgumentException("Cannot scale probabilities to have a sum less than 0");
    } else if (newProbabilitySum == 0) {
      // special case: all events now have 0 probability:
      return DiscreteDistributions.empty();
    }

    double sum = probabilitySum();
    if (sum == newProbabilitySum) {
      // we're already normalized
      return this;
    }

    double multiplier = newProbabilitySum / sum;
    return mapValues(prob -> prob * multiplier, Renormalization.NONE);
  }

  /**
   * Creates a new distribution by mapping labels to new, potentially different labels.
   *
   * @param mapper the label-mapping function that will generate new values.  Note that null is a valid label.  If you
   *               wish to both map <strong>and</strong> filter labels simultaneously, use
   *               {@link #map(Function, Deduplication, Renormalization)} instead.
   * @param deduplication if two or more entries' labels are mapped to the same label, this determines how the duplicate
   *                      labels will be resolved.  If you are <strong>certain</strong> that no duplicates will be
   *                      created, using {@link Deduplication#NONE} will skip a (relatively expensive) deduplication
   *                      step.
   * @param renormalization if deduplication results in some entries being discarded, the distribution can be
   *                        renormalized
   * @param <R> the type of label the returned distribution will have
   * @return a new distribution with remapped labels
   */
  default <R> DiscreteDistribution<R> mapLabels(Function<? super T, ? extends R> mapper,
      Deduplication deduplication, Renormalization renormalization) {
    return map(lp -> lp.mapLabel(mapper), deduplication, renormalization);
  }

  /**
   * Creates a new distribution by changing this distribution's event probabilities to new, potentially different
   * probabilities.
   *
   * Because this is done irrespective of the event's label, it will be useful only in limited circumstances.  To map
   * the entries of this distribution while taking the label into consideration, use the
   * {@link #map(Function, Deduplication, Renormalization)} method instead.
   *
   * @param mapper a function that maps the old probability values to new probability values
   * @param renormalization determines how the probabilities are (optionally) renormalized after being changed
   * @return a new distribution with remapped probabilities
   */
  default DiscreteDistribution<T> mapValues(DoubleUnaryOperator mapper, Renormalization renormalization) {
    // map and renormalize the result
    return DiscreteDistributionHelpers.renormalizedStream(this, stream().map(lp -> lp.mapProbability(mapper)),
        Deduplication.NONE, renormalization);
  }

  /**
   * Creates a new distribution by changing this distribution's event probabilities to new, potentially different
   * probabilities.
   *
   * Because this is done irrespective of the event's label, it will be useful only in limited circumstances.  To map
   * the entries of this distribution while taking the label into consideration, use the
   * {@link #map(Function, Deduplication, Renormalization)} method instead.
   *
   * @param mapper a function that maps the old probability values to new probability values
   * @return a new distribution with remapped probabilities
   */
  default DiscreteDistribution<T> mapValues(DoubleUnaryOperator mapper) {
    return mapValues(mapper, Renormalization.NONE);
  }

  /**
   * Creates a new distribution by mapping {@link LabelProbability} entries ({@code label -> probability} pairs) to new,
   * potentially different entries.  Mapping an entry to a null will result in that entry being deleted entirely.
   *
   * @param mapper a {@link LabelProbability}-mapping function that will generate new entries from existing entries.  If
   *               the returned entry's label is null, it will be omitted from the result.
   * @param deduplication if two or more entries' labels are mapped to the same label, this determines how the
   *                      duplication will be resolved.  If you are <strong>certain</strong> that no duplicates will be
   *                      created, using {@link Deduplication#NONE} will skip a (relatively expensive) deduplication
   *                      step.
   * @param renormalization if entries are removed (by mapping their labels to null) or probabilities are changed, this
   *                        determines how the probabilities are (optionally) renormalized
   * @param <R> the type of label the returned distribution will have
   * @return a new distribution with remapped labels
   */
  default <R> DiscreteDistribution<R> map(Function<LabelProbability<T>, LabelProbability<R>> mapper,
      Deduplication deduplication, Renormalization renormalization) {
    // map the labels and filter out any entries that now have a null label
    Stream<LabelProbability<R>> mapped = stream().map(mapper).filter(Objects::nonNull);

    // deduplicate and renormalize the result
    return DiscreteDistributionHelpers.renormalizedStream(this, mapped, deduplication, renormalization);
  }

  /**
   * Creates a new distribution by filtering entries in this distribution by their label, then (optionally)
   * renormalizing the probabilities of the remaining entries.
   *
   * @param predicate a function that returns true for labels that should be kept, and false for those that should be
   *                  discarded
   * @param renormalization the renormalization scheme to use for the remaining entries
   * @return a new distribution created by filtering the entries of this one
   */
  default DiscreteDistribution<T> filterLabels(Predicate<T> predicate, Renormalization renormalization) {
    return filter(lp -> predicate.test(lp.getLabel()), renormalization);
  }

  /**
   * Creates a new distribution by filtering entries in this distribution, then (optionally)
   * renormalizing the probabilities of the remaining entries.
   *
   * @param predicate a function that returns true for entries that should be kept, and false for those that should be
   *                  discarded
   * @param renormalization the renormalization scheme to use for the remaining entries
   * @return a new distribution created by filtering the entries of this one
   */
  default DiscreteDistribution<T> filter(Predicate<LabelProbability<T>> predicate, Renormalization renormalization) {
    // filtering never creates duplicates; thus, we can skip deduplication when renormalizing
    return DiscreteDistributionHelpers.renormalizedStream(this, stream().filter(predicate), Deduplication.NONE,
        renormalization);
  }
}
