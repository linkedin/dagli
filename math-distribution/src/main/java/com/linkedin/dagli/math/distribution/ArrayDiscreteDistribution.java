package com.linkedin.dagli.math.distribution;

import com.linkedin.dagli.util.array.ArraysEx;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * An immutable {@link DiscreteDistribution} that memorizes labels and probabilities entries in space-efficient arrays.
 */
public class ArrayDiscreteDistribution<T> extends AbstractDiscreteDistribution<T> {
  private static final long serialVersionUID = 1;

  // entries are stored via parallel arrays rather than concrete LabelProbability objects for efficiency
  private final T[] _labels; // this is an Object[] masquerading as T[]; this is safe because we never expose it outside
                             // this class
  private final double[] _probabilities;

  /**
   * Gets the label of the entry in this distribution at the given index.  The index refers to the position of the entry
   * in order of <b>highest probability to smallest</b> (with ties broken arbitrarily).  The order is consistent across
   * all methods of this object and fixed (so entries that are tied in probability will not swap positions).
   *
   * @return the label of the requested entry
   */
  public T getLabelByIndex(int index) {
    return _labels[index];
  }

  /**
   * Gets the label of the entry in this distribution at the given index.  The index refers to the position of the entry
   * in order of <b>highest probability to smallest</b> (with ties broken arbitrarily).  The order is consistent across
   * all methods of this object and fixed (so entries that are tied in probability will not swap positions).
   *
   * @return the probability of the requested entry
   */
  public double getProbabilityByIndex(int index) {
    return _probabilities[index];
  }

  /**
   * Gets a collector that can be used to create a ArrayDiscreteDistribution from a stream, e.g.
   * someDistribution.stream().doSomething().collect(ArrayDiscreteDistribution.collector());
   *
   * @param <T> the type of the label
   * @return a collector that can be used to create a ArrayDiscreteDistribution from a stream
   */
  public static <T> Collector<LabelProbability<T>, ?, ArrayDiscreteDistribution<T>> collector() {
    return Collector.<LabelProbability<T>, ArrayList<LabelProbability<T>>, ArrayDiscreteDistribution<T>>of(
        ArrayList::new, ArrayList::add, (l1, l2) -> {
          l1.addAll(l2);
          return l1;
        }, ArrayDiscreteDistribution::new);
  }

  /**
   * Creates a new ArrayDiscreteDistribution from a collection of LabelProbability entries.  This collection should not
   * contain duplicate labels.
   *
   * @param entryList The entries for the distribution.  Each entry should have a distinct label.
   */
  @SuppressWarnings("unchecked") // masquerading Object[] as T[] is safe because it's never exposed outside the class
  public ArrayDiscreteDistribution(Collection<LabelProbability<T>> entryList) {
    this((T[]) entryList.stream().map(LabelProbability::getLabel).toArray(),
        entryList.stream().mapToDouble(LabelProbability::getProbability).toArray(), null);
  }

  /**
   * Creates a new ArrayDiscreteDistribution from parallel arrays of labels and probabilities.  "Parallel" arrays
   * means that the entry at a particular index in one array corresponds with the element at that index in the other.
   *
   * The provided arrays are copied; the new distribution does not modify or take ownership of them.
   *
   * @param labels the array of (distinct) labels; it's safe to pass an Object[] masquerading as T[]
   * @param probabilities the array of probabilities
   */
  public ArrayDiscreteDistribution(T[] labels, double[] probabilities) {
    this(labels.clone(), probabilities.clone(), null);
  }

  /**
   * Creates a new ArrayDiscreteDistribution from a map of labels to probabilities.
   *
   * @param labelToProbabilityMap the map of labels to probabilities
   */
  public ArrayDiscreteDistribution(Object2DoubleMap<T> labelToProbabilityMap) {
    this(getArraysFromMap(labelToProbabilityMap));
  }

  /**
   * Simple container class for storing an array of labels and its corresponding array of probabilities.
   *
   * @param <T> the type of the label
   */
  private static class LabelAndProbabilityArrays<T> {
    /**
     * The array of labels.
     */
    T[] _labels;

    /**
     * The array of probabilities.
     */
    double[] _probabilities;

    /**
     * Creates a new instance with the specifiied labels and probabilities.
     *
     * @param labels the array of labels
     * @param probabilities the array of probabilities
     */
    LabelAndProbabilityArrays(T[] labels, double[] probabilities) {
      _labels = labels;
      _probabilities = probabilities;
    }
  }

  /**
   * Extracts the labels and probabilities from a map as parallel arrays and packs them in a LabelAndProbabilityArrays
   * object.  This method is required because of the constraints Java places on constructor chaining.
   *
   * @param labelToProbabilityMap a map from labels to their probabilities
   * @param <T> the type of the labels
   * @return a LabelAndProbabilityArrays containing the arrays of labels and probabilities
   */
  @SuppressWarnings("unchecked") // masquerading Object[] as T[] is safe here because it will never be exposed outside
                                 // this class
  private static <T> LabelAndProbabilityArrays<T> getArraysFromMap(Object2DoubleMap<T> labelToProbabilityMap) {
    // if the labelToProbabilityMap is of type Object2DoubleFixedArrayMap, it's very cheap to pull out its underlying
    // arrays:
    if (labelToProbabilityMap instanceof Object2DoubleFixedArrayMap) {
      return new LabelAndProbabilityArrays<>(((Object2DoubleFixedArrayMap<T>) labelToProbabilityMap).getKeyArray(),
          ((Object2DoubleFixedArrayMap<T>) labelToProbabilityMap).getValueArray());
    }

    // otherwise, we need to something a bit more generic:
    T[] labels = (T[]) new Object[labelToProbabilityMap.size()]; // masquerading Object[] as T[]
    double[] probabilities = new double[labelToProbabilityMap.size()];
    int offset = 0;

    // copy labels and probabilities into parallel arrays
    for (Object2DoubleMap.Entry<T> entry : labelToProbabilityMap.object2DoubleEntrySet()) {
      labels[offset] = entry.getKey();
      probabilities[offset] = entry.getDoubleValue();
      offset++;
    }

    return new LabelAndProbabilityArrays<>(labels, probabilities);
  }

  /**
   * Constructor that unpacks a LabelAndProbabilityArrays object and passes the result to the "real" constructor,
   * assuming ownership of the arrays therein.
   *
   * This "extra" constructor is needed because of the constraints Java places upon constructor chaining.
   *
   * @param labelAndProbabilityArrays a container containing the label and probability parallel arrays
   */
  private ArrayDiscreteDistribution(LabelAndProbabilityArrays<T> labelAndProbabilityArrays) {
    this(labelAndProbabilityArrays._labels, labelAndProbabilityArrays._probabilities, null);
  }

  /**
   * Creates a new ArrayDiscreteDistribution from parallel arrays of labels and probabilities.  "Parallel" arrays
   * means that the entry at a particular index in one array corresponds with the element at that index in the other.
   *
   * <strong>This method takes ownership of the provided arrays and may modify them.</strong>  The arrays should not be
   * changed after they have passed to this method.  The advantage of this method over the
   * {@link #ArrayDiscreteDistribution(Object[], double[])} constructor is that it is more efficient due to fewer array
   * copies required.
   *
   * @param labels the array of (distinct) labels; it's safe to pass an Object[] masquerading as a T[]
   * @param probabilities the array of probabilities
   */
  public static <T> ArrayDiscreteDistribution<T> wrap(T[] labels, double[] probabilities) {
    return new ArrayDiscreteDistribution<>(labels, probabilities, null);
  }

  /**
   * Creates a new ArrayDiscreteDistribution from parallel arrays of labels and probabilities.  "Parallel" arrays
   * means that the entry at a particular index in one array corresponds with the element at that index in the other.
   *
   * <b>Note:</b> this method takes ownership of the provided arrays and may modify them.  The arrays should not be
   * changed after they have passed to this method.
   *
   * @param labels the array of (distinct) labels
   * @param probabilities the array of probabilities
   * @param dummyArg an unused argument used to differentiate this constructor from another, otherwise identical one
   */
  private ArrayDiscreteDistribution(T[] labels, double[] probabilities, Void dummyArg) {
    // check the validity of our inputs with asserts
    assert Arrays.stream(labels).distinct().count() == labels.length; // all labels unique?
    assert Arrays.stream(probabilities).noneMatch(p -> p < 0); // no negative probabilities

    // this argument check is cheap enough to do every time
    if (labels.length != probabilities.length) {
      throw new IllegalArgumentException(
          "Length of labels array, " + labels.length + ", does not match the length of the probabilities array, "
              + probabilities.length);
    }

    // if the items in the list are not already in reverse order (highest probability to lowest), sort them
    if (!ArraysEx.isMonotonicallyDecreasing(probabilities)) {
      ArraysEx.sort(probabilities, labels);
      ArraysEx.reverse(probabilities);
      ArraysEx.reverse(labels);
    }

    // eliminate 0-probability events, if any
    int firstZeroIndex = firstZeroIndexInReverseSortedProbabilityArray(probabilities);
    if (firstZeroIndex < probabilities.length) {
      // we have at least one 0-probability event
      probabilities = Arrays.copyOf(probabilities, firstZeroIndex);
      labels = Arrays.copyOf(labels, firstZeroIndex);
    }

    _probabilities = probabilities;
    _labels = labels;
  }

  /**
   * Finds the offset of the first zero in a reverse-sorted (largest to smallest) array of non-negative, finite values.
   *
   * If the array contains no zeros, the return value is the length of the passed array.
   *
   * @param probabilities the reverse-sorted array of non-negative, finite doubles to scan
   * @return the offset of the first zero in the array, or the length of the array if it contains no zeros.
   */
  private static int firstZeroIndexInReverseSortedProbabilityArray(double[] probabilities) {
    // scan backwards, looking for the first non-zero entry
    for (int i = probabilities.length - 1; i >= 0; i--) {
      if (probabilities[i] != 0) {
        return i + 1; // i is the offset of the last non-zero; therefore i + 1 is the offset of the first zero
      }
    }

    return 0; // the array is all zeros (or zero-length)
  }

  /**
   * Private no-args constructor specifically for the benefit of Kryo
   */
  private ArrayDiscreteDistribution() {
    // These values will be overwritten by Kryo.  Final does not stop Kryo from modifying a field when loading an
    // object.
    _probabilities = null;
    _labels = null;
  }

  @Override
  public long size64() {
    return _labels.length;
  }

  @Override
  public Stream<LabelProbability<T>> stream() {
    return IntStream.range(0, _labels.length)
        .mapToObj(index -> new LabelProbability<>(_labels[index], _probabilities[index]));
  }
}
