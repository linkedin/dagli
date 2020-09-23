package com.linkedin.dagli.math.distribution;

import com.linkedin.dagli.util.invariant.Arguments;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;


/**
 * Efficiently samples from a multinomial distribution using the Alias Method
 * (https://en.wikipedia.org/wiki/Alias_method).  Sampling takes constant time and building the alias table
 * (constructing the instance) takes time linear in the number of labels.
 */
public class AliasSampler<T> implements ReplacementSampler<T> {
  private static final long serialVersionUID = 1;

  private final int[] _aliases;
  private final double[] _probabilities;
  private final T[] _labels;

  @Override
  public int hashCode() {
    return Arrays.hashCode(_aliases) + Arrays.hashCode(_probabilities) + Arrays.hashCode(_labels);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof AliasSampler)) {
      return false;
    }

    AliasSampler<?> other = (AliasSampler<?>) obj;
    return Arrays.equals(this._probabilities, other._probabilities) && Arrays.equals(this._aliases, other._aliases)
        && Arrays.equals(this._labels, other._labels);
  }

  /**
   * Creates a new sampler from the provided distribution.  The probability of each element being selected will be
   * proportional to the probability it is assigned in the distribution; the probabilities in the distribution do not
   * need to sum to 1.
   *
   * @param distribution the discrete distribution from which to sample
   */
  public AliasSampler(DiscreteDistribution<T> distribution) {
    this(createLabelsAndProbabilities(distribution));
  }

  // helper constructor that pulls out the labels and probabilities from its passed LabelsAndProbabilities argument
  private AliasSampler(LabelsAndProbabilities<T> labelsAndProbabilities) {
    this(labelsAndProbabilities._labels, labelsAndProbabilities._probabilities, true);
  }

  @SuppressWarnings("unchecked") // cast from Object[] to T[] is safe because the array never escapes the sampler class
  private static <T> LabelsAndProbabilities<T> createLabelsAndProbabilities(DiscreteDistribution<T> distribution) {
    LabelsAndProbabilities<T> res = new LabelsAndProbabilities<>();

    int count = Math.toIntExact(distribution.size64());

    res._probabilities = new double[count];
    res._labels = (T[]) new Object[count];

    Iterator<LabelProbability<T>> iterator = distribution.iterator();
    for (int i = 0; i < count; i++) {
      LabelProbability<T> next = iterator.next();
      res._probabilities[i] = next.getProbability();
      res._labels[i] = next.getLabel();
    }

    return res;
  }

  /**
   * Record class used during construction
   * @param <T> the type of the label
   */
  private static class LabelsAndProbabilities<T> {
    double[] _probabilities;
    T[] _labels;
  }

  /**
   * Creates a new sampler from the provided distribution.  The probability of each element being selected will be
   * proportional to the probability it is assigned in the distribution; the probabilities in the distribution do not
   * need to sum to 1.
   *
   * @param labels the labels (events) corresponding to each probability; need not be distinct values, but length must
   *               match that of the probability array
   * @param probabilities the non-negative probabilities of the distribution (need not sum to 1, but should not sum to
   *                      0 unless the probability array is of length 0
   */
  public AliasSampler(T[] labels, double[] probabilities) {
    this(labels.clone(), probabilities.clone(), true);
  }

  // internal constructor that does not make copies of passed arrays; dummy parameter is unused and exists only to
  // disambiguate this constructor
  private AliasSampler(T[] labels, double[] probabilities, boolean dummy) {
    Arguments.check(probabilities.length == labels.length, "Probabilities and labels must have the same length");

    _probabilities = probabilities;
    _labels = labels;
    _aliases = new int[_probabilities.length];

    double probSum = 0;
    for (int i = 0; i < _probabilities.length; i++) {
      probSum += _probabilities[i];
      _aliases[i] = i;
    }

    Arguments.check(_probabilities.length == 0 || probSum > 0, "Probabilities must sum to a value > 0");

    // normalize the probabilities and put the entries into the large or small queues
    IntArrayList overfullQueue = new IntArrayList(_probabilities.length);
    IntArrayList underfullQueue = new IntArrayList(_probabilities.length);
    for (int i = 0; i < _probabilities.length; i++) {
      // the probability entry corresponds to the probability that we should select label i after we sample i uniformly
      //at random from the range of integers [0, labelCount).  Here's we're both finding this probability and
      // normalizing in one assignment (and yes, the probability can be > 1 for the moment...we'll fix this by shifting
      // probability to other entries with probabilities < 1 later):
      _probabilities[i] = _probabilities.length * (_probabilities[i] / probSum);

      if (_probabilities[i] > 1) {
        overfullQueue.add(i);
      } else if (_probabilities[i] < 1) {
        underfullQueue.add(i);
      }
    }

    while (!underfullQueue.isEmpty() && !overfullQueue.isEmpty()) {
      int nextUnderfull = underfullQueue.popInt();
      int nextOverfull = overfullQueue.popInt();

      // shift some of the probability mass over the overfull entry to the underfull one; the overfull entry now becomes
      // the "alias" for the underfull one.
      double remainder = 1.0 - _probabilities[nextUnderfull];
      _probabilities[nextOverfull] -= remainder;
      _aliases[nextUnderfull] = nextOverfull;

      // the hitherto overfull bucket may still be overfull or it may be underfull
      if (_probabilities[nextOverfull] > 1) {
        overfullQueue.add(nextOverfull); // still overfull
      } else if (_probabilities[nextOverfull] < 1) {
        underfullQueue.add(nextOverfull); // now it's underfull
      }
    }

    // we've run out of underfull or overfull entries; in principle this shouldn't happen, but in practice numerical
    // precision makes it possible.  Anything left should be very close to 1, so we just have to make them exactly 1.0:
    underfullQueue.forEach((int i) -> _probabilities[i] = 1);
    overfullQueue.forEach((int i) -> _probabilities[i] = 1);
  }

  /**
   * Samples a label from a discrete distribution with replacement, using the provided [0, 1) random value.
   *
   * @param standardUniformRandom a [0, 1) or [0, 1] random value sampled uniformly at random by your favorite random
   *                              number generator.
   * @return an {@link Optional} containing the sampled label, or nothing (empty) if the distribution has no labels with
   *         non-zero probability
   */
  @Override
  public Optional<T> sample(double standardUniformRandom) {
    if (_probabilities.length == 0) { // check for degenerate case of empty distribution
      return Optional.empty();
    }

    return Optional.of(_labels[sampleIndex(standardUniformRandom)]);
  }

  /**
   * Samples a label index from a discrete distribution with replacement, using the provided [0, 1) random value.
   *
   * If an array of labels was provided to construct this sampler, the label index corresponds to that array.  If a
   * {@link DiscreteDistribution} was provided, the label index corresponds with the enumeration order of the items
   * in that distribution (highest-probability label to lowest, with ties broken arbitrarily).
   *
   * @param standardUniformRandom a [0, 1) or [0, 1] random value sampled uniformly at random by your favorite random
   *                              number generator.
   * @return the sampled index, or -1 if distribution used to construct this instance had no non-zero probability labels
   */
  public int sampleIndex(double standardUniformRandom) {
    if (_probabilities.length == 0) {
      return -1;
    }

    double upscaledRandom = standardUniformRandom * _probabilities.length;
    int index = (int) (upscaledRandom);
    if (index == _probabilities.length) { // this might be possible with a random value very close to 1.0 (or 1.0)
      index = _probabilities.length - 1;
    }

    // this should be [0, 1), but it doesn't matter if it's slightly higher due to numerical imprecision:
    double residualRandomValue = upscaledRandom - index;
    return residualRandomValue < _probabilities[index] ? index : _aliases[index];
  }
}
