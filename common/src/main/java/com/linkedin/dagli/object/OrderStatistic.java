package com.linkedin.dagli.object;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.function.FunctionResult1;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.map.Multiset;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer3;
import com.linkedin.dagli.util.invariant.Arguments;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongSortedMap;
import it.unimi.dsi.fastutil.objects.ObjectSortedSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;


/**
 * The <a href="https://en.wikipedia.org/wiki/Order_statistic"><i>k</i>th order statistic</a> is the kth smallest
 * value is a sequence of values.
 *
 * This transformer finds the kth order statistic from a (sorted) multiset that associates each value with its count.
 * the order statistic may also be expressed as a percentile (e.g. the median is the 50th percentile).  If you do not
 * have a sorted multiset handy, there are methods that will automatically construct one for you.
 */
@ValueEquality
public class OrderStatistic<T>
    extends AbstractPreparedTransformer3<List<? extends T>, long[], Number, T, OrderStatistic<T>> {
  private static final long serialVersionUID = 1;

  // used to determine how the third, "k" input translates to a particular value in the ordered sequence of values
  private Mode _mode = null;

  /**
   * Determine how the "k" parameter translates to a particular value in the ordered sequence of values
   */
  private enum Mode {
    /**
     * k is a [0, 1] floating point value specifying a percentile
     */
    PERCENTILE,

    /**
     * k is a non-negative integer specifying a 1-based index in the list from smallest to largest
     */
    KTH_SMALLEST,

    /**
     * k is a non-negative integer specifying a 1-based index in the list from largest to smallest
     */
    KTH_LARGEST
  }

  /**
   * Simple transformer that accumulates the values of a LongCollection and puts the cumulative values into an array.
   */
  @ValueEquality
  private static class Accumulate extends AbstractPreparedTransformer1WithInput<LongCollection, long[], Accumulate> {
    private static final long serialVersionUID = 1;

    @Override
    public long[] apply(LongCollection longs) {
      long sum = 0;
      long[] result = new long[longs.size()];
      LongIterator iterator = longs.iterator();
      for (int i = 0; i < result.length; i++) {
        sum += iterator.nextLong();
        result[i] = sum;
      }

      return result;
    }
  }

  /**
   * Sorts a multiset according to some comparator.
   */
  @ValueEquality
  private static class SortedMultiset<T> extends
      AbstractPreparedTransformer1WithInput<Object2LongMap<? extends T>, Object2LongSortedMap<T>, SortedMultiset<T>> {
    private static final long serialVersionUID = 1;

    private final Comparator<? super T> _comparator;

    private SortedMultiset(Comparator<? super T> comparator) {
      _comparator = comparator;
    }

    @Override
    public Object2LongSortedMap<T> apply(Object2LongMap<? extends T> map) {
      Object2LongLinkedOpenHashMap<T> result = new Object2LongLinkedOpenHashMap<>(map.size());
      map.object2LongEntrySet()
          .stream()
          .sorted(Map.Entry.comparingByKey(_comparator))
          .forEach(entry -> result.put(entry.getKey(), entry.getLongValue()));

      return result;
    }
  }

  /**
   * Returns a copy of this instance that will find the kth order statistic of the values from the given input, as
   * naturally ordered (i.e. {@link Comparator#naturalOrder()}).
   *
   * This method automatically creates a preparable transformer ancestor; a DAG using the returned copy will therefore
   * be preparable (even though {@link OrderStatistic} is itself a
   * {@link com.linkedin.dagli.transformer.PreparedTransformer}).
   *
   * @param input an input that provides the values whose kth order statistics should be calculated
   * @return a copy of this instance that will find the kth order statistic of the values from the given input
   */
  @SuppressWarnings("unchecked") // keys of created multiset are known to be comparable
  public OrderStatistic<T> withValuesInput(Producer<? extends Comparable<T>> input) {
    return withMultisetInput(new Multiset<T>().withInput((Producer) input));
  }

  /**
   * Returns a copy of this instance that will find the kth order statistic of the values from the given multiset, as
   * naturally ordered (i.e. {@link Comparator#naturalOrder()}).
   *
   * @param input an input that provides the multiset whose kth order statistics should be calculated
   * @return a copy of this instance that will find the kth order statistic of the values of inputted multiset
   */
  @SuppressWarnings("unchecked") // "extends Comparable<? extends T>" implies "extends T"
  public OrderStatistic<T> withMultisetInput(
      Producer<? extends Object2LongMap<? extends Comparable<? extends T>>> input) {
    // Producer<? extends Object2LongMap<? extends Comparable<? extends T>>> implies
    // Producer<? extends Object2LongMap<? extends T>>; see Producer::castComparable for details.
    return withSortedMultisetInput(
        new SortedMultiset<T>((Comparator) Comparator.naturalOrder()).withInput((Producer) input));
  }

  /**
   * Returns a copy of this instance that will find the kth order statistic of the values from the given sorted
   * multiset.
   *
   * @param input an input that provides the multiset whose kth order statistics should be calculated
   * @return a copy of this instance that will find the kth order statistic of the values of inputted multiset
   */
  public OrderStatistic<T> withSortedMultisetInput(Producer<? extends Object2LongSortedMap<? extends T>> input) {
    return withInput2(new Accumulate().withInput(
        new FunctionResult1<Object2LongSortedMap<? extends T>, LongCollection>(Object2LongSortedMap::values).withInput(
            input)))
        .withInput1(new FunctionResult1<Object2LongSortedMap<? extends T>, ObjectSortedSet<? extends T>>(
            Object2LongSortedMap::keySet).andThen(ArrayList::new).withInput(input));
  }

  /**
   * Returns a copy of this instance that will retrieve the kth smallest value.  The smallest value corresponds to
   * {@code k == 1} (k is 1-based).
   *
   * @param k the value for k
   * @return a copy of this instance that will retrieve the kth smallest value
   */
  public OrderStatistic<T> withKthSmallest(long k) {
    Arguments.check(k >= 0, "k must be non-negative");
    return withKthSmallestInput(new Constant<>(k));
  }

  /**
   * Returns a copy of this instance that will retrieve the kth smallest value.  The smallest value corresponds to
   * {@code k == 1} (k is 1-based).
   *
   * @param k the input providing the value for k
   * @return a copy of this instance that will retrieve the kth smallest value
   */
  public OrderStatistic<T> withKthSmallestInput(Producer<? extends Number> k) {
    OrderStatistic<T> result = withInput3(k);
    result._mode = Mode.KTH_SMALLEST;
    return result;
  }

  /**
   * Returns a copy of this instance that will retrieve the kth largest value.  The largest value corresponds to
   * {@code k == 1} (k is 1-based).
   *
   * @param k the value for k
   * @return a copy of this instance that will retrieve the kth largest value
   */
  public OrderStatistic<T> withKthLargest(long k) {
    Arguments.check(k >= 0, "k must be non-negative");
    return withKthLargestInput(new Constant<>(k));
  }

  /**
   * Returns a copy of this instance that will retrieve the kth largest value.  The largest value corresponds to
   * {@code k == 1} (k is 1-based).
   *
   * @param k the input providing the value for k
   * @return a copy of this instance that will retrieve the kth largest value
   */
  public OrderStatistic<T> withKthLargestInput(Producer<? extends Number> k) {
    OrderStatistic<T> result = withInput3(k);
    result._mode = Mode.KTH_LARGEST;
    return result;
  }

  /**
   * Returns a copy of this instance that will retrieve the value at the given percentile (expressed as a [0, 1]
   * proportion).
   *
   * @param percentile the percentile
   * @return a copy of this instance that will retrieve the value at the given percentile
   */
  public OrderStatistic<T> withPercentile(double percentile) {
    Arguments.check(percentile >= 0 && percentile <= 1, "Percentile must be in the range [0, 1]");
    return withPercentileInput(new Constant<>(percentile));
  }

  /**
   * Returns a copy of this instance that will retrieve the value at the given percentile (expressed as a [0, 1]
   * proportion).
   *
   * @param percentile the input providing the percentile
   * @return a copy of this instance that will retrieve the value at the given percentile
   */
  public OrderStatistic<T> withPercentileInput(Producer<? extends Number> percentile) {
    OrderStatistic<T> result = withInput3(percentile);
    result._mode = Mode.PERCENTILE;
    return result;
  }

  @Override
  public T apply(List<? extends T> items, long[] cumulativeCounts, Number sought) {
    long total = cumulativeCounts[cumulativeCounts.length - 1];
    final long k;
    switch (_mode) {
      case PERCENTILE:
        k = Math.max(1, (long) (Math.round(total * sought.doubleValue())));
        break;
      case KTH_SMALLEST:
        k = sought.longValue();
        break;
      case KTH_LARGEST:
        k = total - sought.longValue() + 1;
        break;
      default:
        throw new IllegalStateException("Unknown mode");
    }

    int index = Arrays.binarySearch(cumulativeCounts, k);
    index = index < 0 ? -(index + 1) : index;

    return items.get(index);
  }
}
