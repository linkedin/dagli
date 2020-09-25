package com.linkedin.dagli.object;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.preparer.AbstractStreamPreparer1;
import com.linkedin.dagli.preparer.Preparer1;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparableTransformer1;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1;
import com.linkedin.dagli.transformer.PreparedTransformer1;
import com.linkedin.dagli.tuple.Tuple2;
import com.linkedin.dagli.util.collection.Iterables;
import com.linkedin.dagli.util.collection.LinkedNode;
import com.linkedin.dagli.util.invariant.Arguments;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * Bucketizes values by sorting them and splitting them into a requested number of buckets, attempting to keep the
 * buckets as close to evenly-filled (in terms of the number of examples that fall into each bucket) as
 * possible.  The result of this transformer is the index of the bucket between 0 and [number of buckets - 1]
 * corresponding to each input value.  Values smaller or larger than any previously seen during preparation will be
 * assigned to bucket index 0 or bucket index [number of buckets - 1], respectively.
 *
 * Bucket assignment using dynamic programming (in time O(# distinct items * # buckets)) to find an optimal bucketing
 * that minimizes the total squared error: \sum_i (Ceiling((example count) / (bucket count)) - (# items in bucket i))^2
 *
 * If you have predefined buckets, use {@link BucketIndex.Prepared} instead.
 */
@ValueEquality
public class BucketIndex extends
    AbstractPreparableTransformer1<Comparable<?>, Integer, PreparedTransformer1<Comparable<?>, Integer>, BucketIndex> {
  private static final long serialVersionUID = 1;

  private int _bucketCount = 10;

  /**
   * Returns a copy of this instance that will use, at most, the specified number of buckets.  The actual number of
   * buckets may be less than requested if there are fewer distinct values than number of buckets.
   *
   * The default number of buckets is 10.
   *
   * @param count the number of buckets to use (must be {@code >= 1})
   * @return a copy of this instance that will use the specified number of buckets
   */
  public BucketIndex withBucketCount(int count) {
    Arguments.check(count >= 1, "Bucket count must be at least 1");
    return clone(c -> c._bucketCount = count);
  }

  /**
   * Returns a copy of this instance that will accept its (comparable) inputs from the provided producer and bucket
   * them according to their natural ordering.
   *
   * @param producer the producer providing the input values to this transformer
   * @return a copy of this instance that will accept its inputs from the provided producer
   */
  public BucketIndex withInput(Producer<? extends Comparable<?>> producer) {
    return withInput1(producer);
  }

  @Override
  protected Preparer1<Comparable<?>, Integer, PreparedTransformer1<Comparable<?>, Integer>> getPreparer(
      PreparerContext context) {
    return new Preparer(_bucketCount);
  }

  private static class Preparer<T extends Comparable<? super T>>
      extends AbstractStreamPreparer1<T, Integer, Prepared<T>> {
    private final Object2LongOpenHashMap<T> _counts = new Object2LongOpenHashMap<>();
    private final int _bucketCount;
    private long _total = 0;

    Preparer(int bucketCount) {
      _bucketCount = bucketCount;
    }

    @Override
    public void process(T value1) {
      _counts.addTo(value1, 1);
      _total++;
    }

    private Tuple2<LinkedNode<T>, Double> findOptimalBuckets(
        List<Object2LongMap.Entry<T>> items, long targetBucketSize, int firstRemainingItemIndex,
        int bucketsRemaining, Tuple2<LinkedNode<T>, Double>[][] cache) {
      Tuple2<LinkedNode<T>, Double> result;

      if (firstRemainingItemIndex == items.size()) { // no more items, no more error
        return Tuple2.of(null, 0.0);
      } else if (cache[bucketsRemaining - 1][firstRemainingItemIndex] != null) { // cached?
        return cache[bucketsRemaining - 1][firstRemainingItemIndex];
      } else if (bucketsRemaining == 1) { // everything goes in the remaining bucket
        long sum = 0;
        for (int i = firstRemainingItemIndex; i < items.size(); i++) {
          sum += items.get(i).getLongValue();
        }
         result = Tuple2.of(null, Math.pow(sum - targetBucketSize, 2));
      } else {
        long countSoFar = 0;
        double bestError = Double.POSITIVE_INFINITY;
        result = null;
        for (int i = firstRemainingItemIndex; i < items.size(); i++) {
          final Tuple2<LinkedNode<T>, Double> remainder =
              findOptimalBuckets(items, targetBucketSize, i + 1, bucketsRemaining - 1, cache);

          countSoFar += items.get(i).getLongValue();
          double totalError = Math.pow(countSoFar - targetBucketSize, 2) + remainder.get1();

          if (totalError <= bestError) {
            T curItem = items.get(i).getKey();
            result = Tuple2.of(remainder.get0() == null ? new LinkedNode<>(curItem) : remainder.get0().add(curItem),
                totalError);
            bestError = totalError;
          }
        }
      }

      assert result != null;
      cache[bucketsRemaining - 1][firstRemainingItemIndex] = result;
      return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public PreparerResult<Prepared<T>> finish() {
      List<Object2LongMap.Entry<T>> sortedEntries = _counts.object2LongEntrySet()
          .stream()
          .sorted(Map.Entry.comparingByKey())
          .collect(Collectors.toList());

      Tuple2<LinkedNode<T>, Double> bestBucketization =
          findOptimalBuckets(sortedEntries, (_total + _bucketCount - 1) / _bucketCount, 0, _bucketCount,
              new Tuple2[_bucketCount][sortedEntries.size()]);

      final List<T> bucketBounds;
      if (bestBucketization.get0() == null) {
        bucketBounds = Collections.emptyList();
      } else {
        bucketBounds = bestBucketization.get0().toArrayList();
        Collections.reverse(bucketBounds);
      }
      return new PreparerResult<>(new Prepared<T>().withBucketBounds(bucketBounds));
    }
  }

  /**
   * Bucketizes an input value according to a sorted list of bucket boundaries, producing an {@link Integer} bucket
   * index.
   *
   * The bucket index of an input value is the index of the first value in the bucket boundary list that
   * is greater than or equal to that input value, or {@code bucketList.size()} if the input value is greater than all
   * items in the bucket boundary list.
   *
   * @param <T> the type of the item to be bucketized (must be comparable)
   */
  @ValueEquality
  public static class Prepared<T extends Comparable<? super T>>
      extends AbstractPreparedTransformer1<T, Integer, Prepared<T>> {
    private static final long serialVersionUID = 1;

    private List<? extends T> _bucketBounds = null;

    @Override
    public void validate() {
      super.validate();
      Objects.requireNonNull(_bucketBounds, "The bucket bounds have not been set; use withBucketBounds(...)");
    }

    /**
     * Returns a copy of this instance that will accept its (comparable) inputs from the provided producer and bucket
     * them according to their natural ordering.
     *
     * @param producer the producer providing the input values to this transformer
     * @return a copy of this instance that will accept its inputs from the provided producer
     */
    public BucketIndex.Prepared<T> withInput(Producer<? extends T> producer) {
      return withInput1(producer);
    }

    /**
     * Returns a copy of this instance that will use the provided bucket bounds.  Each bound represents the "ceiling"
     * for a bucket, such that the bucket for a value is the index of the first bound that is equal or greater in size
     * to that value.  If a value is greater than all the bounds (including when the bound list is empty), the bucket
     * index will be {@code size(bucketBounds)}.
     *
     * @param bucketBounds a list of bucket "ceiling" bounds, sorted according to their natural order (from smallest to
     *                     largest)
     * @return a copy of this instance that will use the provided bucket bounds
     */
    public Prepared<T> withBucketBounds(Iterable<? extends T> bucketBounds) {
      Arguments.check(Iterables.isSorted(bucketBounds, Comparator.naturalOrder()),
          "Bucket bounds must be in natural (ascending) order");
      return clone(c -> c._bucketBounds = Iterables.concatenate(bucketBounds));
    }

    /**
     * Returns a copy of this instance that will use the provided bucket bounds.  Each bound represents the "ceiling"
     * for a bucket, such that the bucket for a value is the index of the first bound that is equal or greater in size
     * to that value.  If a value is greater than all the bounds (including when the bound list is empty), the bucket
     * index will be {@code size(bucketBounds)}.
     *
     * @param bucketBounds an array of bucket "ceiling" bounds, sorted according to their natural order (from smallest
     *                     to largest)
     * @return a copy of this instance that will use the provided bucket bounds
     */
    @SuppressWarnings("unchecked")
    public Prepared<T> withBucketBounds(T... bucketBounds) {
      return withBucketBounds(Arrays.asList(bucketBounds));
    }

    @Override
    public Integer apply(T value) {
      int bucket = Collections.binarySearch(_bucketBounds, value);
      return (bucket >= 0) ? bucket : -(bucket + 1);
    }
  }
}
