package com.linkedin.dagli.object;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.preparer.AbstractStreamPreparer1;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.transformer.AbstractPreparableTransformer1WithInput;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import it.unimi.dsi.fastutil.objects.Object2IntRBTreeMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;


/**
 * Ranks the inputs and returns their 0-based position in the sorted list, relative to the items seen during
 * preparation.  For example, if we see 2, 4, and 5 during preparation, and are then asked to rank "3", it would be the
 * second-lowest item in this list and thus get rank 1.
 *
 * Note that keeping track of so many items can be expensive.  Use {@link #withLimit(int)} method to restrict the number
 * of items tracked to the top K.
 *
 * If a comparator is not specified, the default is natural order.  In this case, a runtime exception will
 * result if the input type does not implementer Comparable (since it won't have a natural order).
 */
@ValueEquality
public class Rank extends AbstractPreparableTransformer1WithInput<Object, Integer, Rank.Prepared, Rank> {
  private static final long serialVersionUID = 1;

  private Comparator<?> _comparator = Comparator.naturalOrder();
  private int _limit = Integer.MAX_VALUE;
  private boolean _ignoreDuplicates = false;

  /**
   * Sets a limit on the number of objects ranked; only the top [limit] entries seen during preparation will be stored.
   * Any later entries with a numerically higher rank will be truncated to the limit; e.g. if the limit is 5 and the
   * true rank is 7, the returned rank will be 5.
   *
   * @param limit the number of highest-ordered objects to remember
   * @return a new Rank preparable with the specified limit
   */
  public Rank withLimit(int limit) {
    return clone(c -> c._limit = limit);
  }

  /**
   * By default, duplicate values will all be included in the ranking.  E.g. the ranks of the elements 1, 2, 2, 2, 3 are
   * 0, 1, 1, 1, 4.  Alternatively, duplicates may be ignored, in which case the ranking will be: 0, 1, 2.
   *
   * @param ignore if true, duplicate values are discarded
   * @return a new Rank preparable with duplicates ignored (or not) as specified.
   */
  public Rank withIgnoreDuplicates(boolean ignore) {
    return clone(c -> c._ignoreDuplicates = ignore);
  }

  /**
   * Sets a comparator to be used to sort and rank the items.  By default, items are ranked in natural order, so the
   * LOWEST value will have the highest rank.  Oftentimes you want to instead rank from highest to lowest, in which case
   * you can specify, e.g. Comparator.reverseOrder().
   *
   * You must also be very cautious about the serializability of your comparator if it is a lambda.  Carelessly created
   * lambdas may not successfully deserialize later; avoid lambdas, or make sure that the code that creates the lambda
   * is visible to the deserializer.  If you're not sure, use a standard serializable object implementing the comparator
   * instead.
   *
   * @param comparator the comparator; must be safely (de)serializable!
   * @return a new Rank preparable with the specified comparator.
   */
  public Rank withComparator(Comparator<?> comparator) {
    return clone(c -> c._comparator = comparator);
  }

  /**
   * Creates a new instance.
   */
  public Rank() {
    super();
  }

  /**
   * A transformer that, using a provided array of objects, their corresponding ranks, and an comparator (needed to
   * determine a rank when an object was not seen during preparation--its rank is found by finding its position amongst
   * the known items), maps objects to their rankings.
   */
  public static class Prepared extends AbstractPreparedTransformer1WithInput<Object, Integer, Prepared> {
    private static final long serialVersionUID = 1;

    private final Object[] _objects;
    private final int[] _ranks;
    private final Comparator<Object> _comparator;

    /**
     * Creates a new instance of the transformer.
     *
     * This constructor takes possession of all passed parameters, which should not be modified after this call.
     *
     * @param objects a ranked list of previously-seen objects (starting with those that are "lowest" in value and have
     *               the lowest rank values)
     * @param ranks either null or an array providing the ranks of the objects (the rank at a given index is taken to
     *              correspond to the object at the same index).  Must be the same length as the number of objects + 1
     *              (the last rank is the rank that will be assigned to a value greater than the maximum recorded
     *              value).  The first rank in the array should always be 0.  Ranks must be in monotonically increasing
     *              order.
     * @param comparator used to determine the ordered postion of a query object within the objects array
     */
    private Prepared(Object[] objects, int[] ranks, Comparator<Object> comparator) {
      super(MissingInput.get());
      _objects = objects;
      _ranks = ranks;
      _comparator = comparator;

      assert _ranks == null || (_objects.length + 1 == _ranks.length);
      assert _ranks == null || _ranks[0] == 0;
    }

    @Override
    public Integer apply(Object value0) {
      int res = Arrays.binarySearch(_objects, value0, _comparator);
      if (res < 0) {
        res = -(res + 1);
      }

      if (_ranks == null) {
        return res;
      } else {
        return _ranks[res];
      }
    }

    @Override
    protected boolean computeEqualsUnsafe(Prepared other) {
      return Arrays.equals(this._objects, other._objects) && Arrays.equals(this._ranks, other._ranks) && Objects.equals(
          this._comparator, other._comparator);
    }

    @Override
    protected int computeHashCode() {
      return Arrays.hashCode(this._objects) + Arrays.hashCode(this._ranks) + Objects.hashCode(this._comparator);
    }
  }

  /**
   * Preparer for the {@link Rank} transformer.
   */
  private static class Preparer extends AbstractStreamPreparer1<Object, Integer, Prepared> {
    private final int _limit;
    private final boolean _ignoreDuplicates;
    private final Object2IntRBTreeMap<Object> _treeMap;
    private long _totalCount = 0; // only used if _ignoreDuplicates == false

    Preparer(Rank rank) {
      _limit = rank._limit;
      _ignoreDuplicates = rank._ignoreDuplicates;
      _treeMap = new Object2IntRBTreeMap<>((Comparator<Object>) rank._comparator);
    }

    private long getTotalCount() {
      return _ignoreDuplicates ? _treeMap.size() : _totalCount;
    }

    @Override
    public PreparerResult<Prepared> finish() {

      Object[] objects = _treeMap.keySet().toArray();
      int[] ranks = null;

      if (!_ignoreDuplicates) {
        ranks = new int[objects.length + 1];
        int offset = 1; // we intentionally leave the first element 0 to simplify later lookup logic
        int nextRank = 0;
        for (int count : _treeMap.values()) {
          nextRank += count;
          ranks[offset++] = nextRank;
        }
      }

      return new PreparerResult<>(new Prepared(objects, ranks, _treeMap.comparator()));
    }

    @Override
    public void process(Object value0) {
      if (getTotalCount() < _limit) {
        if (_ignoreDuplicates) {
          _treeMap.put(value0, 1);
        } else {
          _totalCount++;
          _treeMap.addTo(value0, 1);
        }
      } else {
        Object worst = _treeMap.lastKey();
        if (_treeMap.comparator().compare(value0, worst) < 0) {
          if (_ignoreDuplicates) {
            int oldCount = _treeMap.put(value0, 1);
            if (oldCount == 0) { // make sure we really added something
              _treeMap.removeInt(worst); // delete worst entry entirely, regardless of count
            }
          } else {
            _treeMap.addTo(value0, 1);
            // decrement the count of the worst thing and remove if necessary
            if (_treeMap.addTo(worst, -1) == 1) {
              _treeMap.removeInt(worst);
            }
          }
        }
      }
    }
  }

  @Override
  protected Preparer getPreparer(PreparerContext context) {
    return new Preparer(this);
  }
}
