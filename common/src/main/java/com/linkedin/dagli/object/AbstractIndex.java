package com.linkedin.dagli.object;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.preparer.AbstractStreamPreparer1;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.transformer.AbstractPreparableTransformer1WithInput;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1;
import com.linkedin.dagli.util.invariant.Arguments;
import com.linkedin.dagli.view.AbstractTransformerView;
import com.linkedin.dagli.view.TransformerView;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;


/**
 * Base class for {@link Index} and {@link Indices}, which map arbitrary objects to consecutive integers starting at 0.
 *
 * Values that are not explicitly mapped (values not seen during preparation, or ignored during preparation due to
 * {@link #withMaxUniqueObjects(int)} or {@link #withMinimumFrequency(int)}) are considered "unknown" and
 * mapped to an index that depends on the specified {@link UnknownItemPolicy}.
 *
 * The default policy is to assign a new, unique index to "unknown" items, while also treating the least-common item
 * seen during training (preparation) as "unknown".  This avoids a potentially dangerous scenario where an index value
 * could be encountered during inference without ever being encountered during training; this policy can be changed with
 * {@link #withUnknownItemPolicy(UnknownItemPolicy)}.
 *
 * @param <I> the type of the input to this transformer
 * @param <T> the type of item indexed by this transformer
 * @param <R> the type of result produced by this transformer
 * @param <N> the derived type of the prepared transformer
 * @param <S> the derived transformer type
 */
@ValueEquality
abstract class AbstractIndex<I, T, R, N extends AbstractIndex.Prepared<I, T, R, N>, S extends AbstractIndex<I, T, R, N, S>>
    extends AbstractPreparableTransformer1WithInput<I, R, N, S> {
  private static final long serialVersionUID = 1;
  protected static final int IGNORED_ITEM_INDEX = -2;

  private int _maxMappings = Integer.MAX_VALUE;
  private int _maxMappingsDuringPreparation = Integer.MAX_VALUE; // the effective value is always at least maxMappings
  private int _minimumRetentionFrequency = 1;
  private Comparator<? super T> _mappingOrderComparator = null; // must be serializable
  private UnknownItemPolicy _unknownItemPolicy = UnknownItemPolicy.DISTINCT;

  /**
   * Returns a view of this {@link AbstractIndex} instance that will provide the number of items indexed (or,
   * equivalently, the maximum explicit index + 1).  If {@link #withMaxUniqueObjects(int)} is not used to set an upper
   * limit on the number of indexed items lower than the number of distinct items seen <strong>and</strong> the
   * {@link UnknownItemPolicy} is not {@link UnknownItemPolicy#DISTINCT}, this is also the number of distinct input
   * values encountered during preparation.  Note that {@link UnknownItemPolicy#DISTINCT} is the default policy.
   */
  public TransformerView<Integer, Prepared<?, ?, ?, ?>> asIndexedCount() {
    return new IndexedCountView(this);
  }

  @ValueEquality
  private static class IndexedCountView extends AbstractTransformerView<Integer, Prepared<?, ?, ?, ?>, IndexedCountView> {
    private static final long serialVersionUID = 1;

    /**
     * Creates a new view of the specified transformer
     * @param viewedTransformer the Index transformer being viewed
     */
    public IndexedCountView(AbstractIndex<?, ?, ?, ?, ?> viewedTransformer) {
      super(viewedTransformer);
    }

    @Override
    protected Integer prepare(Prepared<?, ?, ?, ?> preparedTransformerForNewData) {
      return preparedTransformerForNewData._indexMap.size();
    }
  }

  /**
   * Returns a view of this {@link AbstractIndex} instance that will provide a {@link Int2ObjectMap} from the indexed objects to
   * their indices.  This map will not include all distinct objects seen if their quantity exceeds the maximum unique
   * objects limit (set with {@link #withMaxUniqueObjects(int)}).
   *
   * @return a view of this {@link AbstractIndex} instance that will provide a {@link Int2ObjectMap} from the indexed objects to
   *         their indices
   */
  public TransformerView<Int2ObjectMap<T>, Prepared<?, T, ?, ?>> asIndexToObjectMap() {
    return new IndexToObjectView<>(this);
  }

  @ValueEquality
  private static class IndexToObjectView<T>
      extends AbstractTransformerView<Int2ObjectMap<T>, Prepared<?, T, ?, ?>, IndexToObjectView<T>> {
    private static final long serialVersionUID = 1;

    /**
     * Creates a new view of the specified transformer
     * @param viewedTransformer the Index transformer being viewed
     */
    public IndexToObjectView(AbstractIndex<?, T, ?, ?, ?> viewedTransformer) {
      super(viewedTransformer);
    }

    @Override
    protected Int2ObjectMap<T> prepare(Prepared<?, T, ?, ?> preparedTransformerForNewData) {
      Int2ObjectOpenHashMap<T> result =
          new Int2ObjectOpenHashMap<>(preparedTransformerForNewData._indexMap.size());

      preparedTransformerForNewData._indexMap.object2IntEntrySet()
          .fastForEach(entry -> result.put(entry.getIntValue(), entry.getKey()));

      return result;
    }
  }

  /**
   * Returns a view of this {@link AbstractIndex} instance that will provide a {@link Long2ObjectMap} from the indexed objects
   * to their indices.  This map will not include all distinct objects seen if their quantity exceeds the maximum unique
   * objects limit (set with {@link #withMaxUniqueObjects(int)}).
   *
   * The map returned has semantically identical entries to that provided by {@link #asIndexToObjectMap()}.  This
   * method exists as a convenience because a map from longs (rather than ints) to objects is often required.
   *
   * @return a view of this {@link AbstractIndex} instance that will provide a {@link Long2ObjectMap} from the indexed objects
   *         to their indices
   */
  public TransformerView<Long2ObjectMap<T>, Prepared<?, T, ?, ?>> asLongIndexToObjectMap() {
    return new LongIndexToObjectView<>(this);
  }

  @ValueEquality
  private static class LongIndexToObjectView<T>
      extends AbstractTransformerView<Long2ObjectMap<T>, Prepared<?, T, ?, ?>, LongIndexToObjectView<T>> {
    private static final long serialVersionUID = 1;

    /**
     * Creates a new view of the specified transformer
     * @param viewedTransformer the Index transformer being viewed
     */
    public LongIndexToObjectView(AbstractIndex<?, T, ?, ?, ?> viewedTransformer) {
      super(viewedTransformer);
    }

    @Override
    protected Long2ObjectMap<T> prepare(Prepared<?, T, ?, ?> preparedTransformerForNewData) {
      Long2ObjectOpenHashMap<T> result =
          new Long2ObjectOpenHashMap<>(preparedTransformerForNewData._indexMap.size());

      preparedTransformerForNewData._indexMap.object2IntEntrySet()
          .fastForEach(entry -> result.put(entry.getIntValue(), entry.getKey()));

      return result;
    }
  }

  /**
   * Gets the maximum number of mappings (unique elements) that will be remembered by this Index.  By default, this is
   * Integer.MAX_VALUE.
   *
   * @return the maximum number of mappings
   */
  public int getMaxUniqueObjects() {
    return _maxMappings;
  }

  /**
   * Set the maximum number of unique elements that will be indexed/remembered by this instance.  By default, this is
   * Integer.MAX_VALUE.
   *
   * @param max the maximum number of objects to remember
   * @return a copy of this instance that will remember the specified maximum number of unique objects.
   */
  public S withMaxUniqueObjects(int max) {
    Arguments.check(max > 0, "Maximum cannot be less than 1");
    return this.clone(c -> ((AbstractIndex<?, ?, ?, ?, ?>) c)._maxMappings = max);
  }

  /**
   * Gets the maximum number of mappings (unique elements) that can be stored in memory during preparation of this
   * instance.  The returned value will always be at least as large as that returned by {@link #getMaxUniqueObjects()}.
   *
   * By default, this is {@link Integer#MAX_VALUE}.
   *
   * @return the maximum number of mappings that can be temporarily kept in memory during preparation of this instance
   */
  public int getMaxUniqueObjectsDuringPreparation() {
    return Math.max(_maxMappingsDuringPreparation, _maxMappings);
  }

  /**
   * Returns a copy of this instance that will store up to the specified maximum number of unique elements kept in
   * memory during preparation of this instance.  This can be larger than the final number of unique elements that
   * will be mapped by the resulting prepared transformer, which trades potentially increased memory usage for better
   * estimates of item frequency (which determine which items are ultimately mapped).
   *
   * <strong>Example:</strong> if, at some time during preparation, this maximum is reached, no new items will be
   * remembered.  If this happens halfway through training, but all subsequent items have the hitherto-unseen value "X",
   * "X" will not be contained in the final mapping despite being common in the data.  The solution here could be either
   * to shuffle (randomize the order of) the examples prior to training, or to increase this working maximum so that
   * more (perhaps all) items seen during preparation can be remembered until preparation is complete, at which point
   * only the number of unique objects specified by {@link #withMaxUniqueObjects(int)} with the highest observed
   * frequency will be assigned mappings.
   *
   * By default, the maximum is {@link Integer#MAX_VALUE} (effectively no limit).  A value lower than
   * {@link #getMaxUniqueObjects()} may be set without error, but the actual maximum number of items temporarily
   * remembered during preparation will always be at least {@link #getMaxUniqueObjects()}.
   *
   * @param max the maximum number of objects to remember during preparation; higher values have the potential to
   *            use more memory
   * @return a copy of this instance that will remember up to the specified maximum number of unique objects during
   *         preparation
   */
  public S withMaxUniqueObjectsDuringPreparation(int max) {
    return this.clone(c -> ((AbstractIndex<?, ?, ?, ?, ?>) c)._maxMappingsDuringPreparation = max);
  }

  /**
   * Returns a copy of this instance that will use the specified comparator to order the indices that are assigned to
   * the mapped items (note that which items are dropped if the map lacks sufficient capacity is still determined by
   * their frequency, not comparator order.)
   *
   * Items that compare lower will have lower indices.  For example, if the mapped items are {"C", "A", "B"} and
   * a natural ordering comparator is used the mapped indices will be {@code {"A" -> 0, "B" -> 1, "C" -> 2}}.
   *
   * By default, if no comparator is specified, indices will be assigned in decreasing order of frquency (the most
   * frequent item will be assigned to index 0, the second-most frequent index 1, etc.)
   *
   * The provided {@link Comparator} <strong>must</strong> be serializable.  Comparators created from static methods on
   * {@link Comparator} are serializable (except where they wrap an existing non-serializable comparator or other
   * non-serializable object).
   *
   * @param comparator a serializable comparator used to determine the indices assigned to the items
   * @return a copy of this instance that will use the specified comparator
   */
  public S withIndexOrderComparator(Comparator<? super T> comparator) {
    Arguments.check(comparator instanceof Serializable, "The provided comparator must be serializable");
    return clone(c -> ((AbstractIndex<?, T, ?, ?, ?>) c)._mappingOrderComparator = comparator);
  }

  /**
   * @return  the {@link UnknownItemPolicy} that will be used to determine the index of items that are not explicitly
   *          assigned indices.
   */
  public UnknownItemPolicy getUnknownItemPolicy() {
    return _unknownItemPolicy;
  }

  /**
   * Returns a copy of this instance that will use the specified {@link UnknownItemPolicy}.
   *
   * This policy determines the index of items that were either not seen during preparation or were ignored because the
   * explicit index map was full or the item did not occur sufficiently frequently.
   *
   *
   *
   * The default policy is {@link UnknownItemPolicy#DISTINCT}, which assigns "unknown" items a new, dedicated index,
   * but also makes sure that at least one "unknown" item with this index is seen during training (by always dropping
   * the lowest-frequency item and treating it as "unkown").  This avoids a potential pitfall where downstream nodes
   * might be adversely affected when encountering a novel index during inference that was never seen during
   * preparation.
   *
   * Alternatively, the simplest policy is {@link UnknownItemPolicy#NEW}, which just assigns unknown items a new,
   * dedicated index without any auto-dropping of items during preparation.  While straightforward, this may be
   * problematic if downstream nodes are not robust against novel indices at inference-time.
   *
   * @param unknownItemPolicy the unknown item policy to use
   * @return a copy of this instance that will use to specified policy
   */
  public S withUnknownItemPolicy(UnknownItemPolicy unknownItemPolicy) {
    return clone(c -> ((AbstractIndex<?, ?, ?, ?, ?>) c)._unknownItemPolicy = unknownItemPolicy);
  }

  /**
   * @return the minimum frequency of an item during preparation for it to be assigned an explicit index; rarer items
   *         are ignored and are considered "unknown", with their indices assigned according to
   *         {@link #getUnknownItemPolicy()}
   */
  public int getMinimumRetentionFrequency() {
    return _minimumRetentionFrequency;
  }

  /**
   * Returns a copy of this instance that will use the specified minimum retention frequency.
   *
   * Items that are seen fewer times than this during preparation will be ignored and are considered "unknown", with
   * their indices assigned according to {@link #getUnknownItemPolicy()}.
   *
   * The default value is 1.
   *
   * @param minimumRetentionFrequency the minimum number of times an item must be seen during preparation to be assigned
   *                                  an explicit index (and not be considered "unknown"); must be {@code >= 1}
   * @return a copy of this instance that will use the specified minimum retention frequency
   */
  public S withMinimumFrequency(int minimumRetentionFrequency) {
    Arguments.check(minimumRetentionFrequency >= 1, "Minimum frequency must be at least 1");
    return clone(c -> ((AbstractIndex<?, ?, ?, ?, ?>) c)._minimumRetentionFrequency = minimumRetentionFrequency);
  }

  protected abstract N getPrepared(Object2IntOpenHashMap<T> indexMap);

  protected abstract void repeatForEachItem(I items, Consumer<T> consumer);

  @Override
  protected Preparer<I, T, R, N, S> getPreparer(PreparerContext context) {
    return new Preparer<>(this);
  }

  /**
   * Preparer for the {@link AbstractIndex} transformer.
   */
  static class Preparer<I, T, R, N extends AbstractIndex.Prepared<I, T, R, N>, S extends AbstractIndex<I, T, R, N, S>>
      extends AbstractStreamPreparer1<I, R, N> {
    private final AbstractIndex<I, T, R, N, S> _owner;
    private final long _maxMappingsDuringPreparation;

    private Object2LongOpenHashMap<T> _valueCounts;
    private long _unknownCount = 0;

    Preparer(AbstractIndex<I, T, R, N, S> owner) {
      _owner = owner;
      _valueCounts = new Object2LongOpenHashMap<>(128); // keep track of value frequency
      _maxMappingsDuringPreparation = owner.getMaxUniqueObjectsDuringPreparation();
    }

    @Override
    public PreparerResult<N> finish() {
      boolean sawUnknownItems = _unknownCount > 0;

      // trim low-count entries
      if (_owner._minimumRetentionFrequency > 1) {
        ObjectIterator<Object2LongMap.Entry<T>> iterator = _valueCounts.object2LongEntrySet().fastIterator();
        while (iterator.hasNext()) {
          long frequency = iterator.next().getLongValue();
          if (frequency < _owner._minimumRetentionFrequency) {
            iterator.remove();
            sawUnknownItems = true;
          }
        }
      }

      // note that if _owner._mappingOrderComparator != null, we don't need to trim down the map here--we can do it in
      // the next step at no extra cost
      if (_valueCounts.size() > _owner._maxMappings && _owner._mappingOrderComparator != null) {
        sawUnknownItems = true; // we are dropping at least one mapping

        Object2LongOpenHashMap<T> trimmedValueCounts = new Object2LongOpenHashMap<>(_owner._maxMappings);
        _valueCounts.object2LongEntrySet()
            .stream()
            .sorted(Comparator.<Object2LongMap.Entry<T>>comparingLong(Object2LongMap.Entry::getLongValue).reversed())
            .limit(_owner._maxMappings)
            .forEach(entry -> trimmedValueCounts.put(entry.getKey(), entry.getLongValue()));
        _valueCounts = trimmedValueCounts;
      }

      int resultSize = Math.min(_owner._maxMappings, _valueCounts.size());
      final Object2IntOpenHashMap<T> indexMap = new Object2IntOpenHashMap<>(resultSize);

      if (_owner._mappingOrderComparator != null) {
        // note that in this case _valueCounts has already been trimmed down to be of size resultSize (above)
        _valueCounts.keySet()
            .stream()
            .sorted(_owner._mappingOrderComparator)
            .forEach(item -> indexMap.put(item, indexMap.size()));
      } else {
        _valueCounts.object2LongEntrySet()
            .stream()
            .sorted(Comparator.<Object2LongMap.Entry<T>>comparingLong(Object2LongMap.Entry::getLongValue).reversed())
            .limit(resultSize)
            .map(Map.Entry::getKey)
            .forEach(item -> indexMap.put(item, indexMap.size()));

        sawUnknownItems |= indexMap.size() < _valueCounts.size(); // did we drop items?
      }

      final int unknownIndex;

      // determine what policy we should use based on whether or not "unknown" items were seen during preparation
      UnknownItemPolicy effectivePolicy = _owner._unknownItemPolicy;
      if (effectivePolicy == UnknownItemPolicy.DISTINCT) {
        if (!sawUnknownItems) {
          // the min count key should not be null (it would imply indexMap is empty which, given that sawUnknownItems
          // is false, means there were no examples at all).  However, even if it is null it doesn't create a problem
          // for us, so we don't bother checking for this case here.
          T minCountKey = minCountKey(indexMap);
          indexMap.removeInt(minCountKey);
        }
        effectivePolicy = UnknownItemPolicy.NEW;
      }

      switch (effectivePolicy) {
        case IGNORE:
          unknownIndex = IGNORED_ITEM_INDEX;
          break;
        case NEGATIVE_ONE:
          unknownIndex = -1;
          break;
        case NEW:
          unknownIndex = indexMap.size();
          break;
        case MOST_FREQUENT:
          unknownIndex = _owner._mappingOrderComparator == null ? 0 : maxCountIndex(indexMap);
          break;
        case LEAST_FREQUENT:
          unknownIndex =
              _owner._mappingOrderComparator == null ? Math.max(0, indexMap.size() - 1) : minCountIndex(indexMap);
          break;
        default:
          throw new IllegalStateException("Unexpected unknown item policy encountered");
      }

      indexMap.defaultReturnValue(unknownIndex);
      return new PreparerResult<>(_owner.getPrepared(indexMap));
    }

    private int maxCountIndex(Object2IntOpenHashMap<T> indexMap) {
      return indexMap.object2IntEntrySet()
          .stream()
          .max(Comparator.comparingLong(entry -> _valueCounts.getOrDefault(entry.getKey(), 0)))
          .map(Map.Entry::getValue).orElse(0);
    }

    private int minCountIndex(Object2IntOpenHashMap<T> indexMap) {
      return indexMap.object2IntEntrySet()
          .stream()
          .min(Comparator.comparingLong(entry -> _valueCounts.getOrDefault(entry.getKey(), 0)))
          .map(Map.Entry::getValue).orElse(0);
    }

    private T minCountKey(Object2IntOpenHashMap<T> indexMap) {
      return indexMap.object2IntEntrySet()
          .stream()
          .min(Comparator.comparingLong(entry -> _valueCounts.getOrDefault(entry.getKey(), 0)))
          .map(Map.Entry::getKey).orElse(null);
    }

    @Override
    public void process(I items) {
      _owner.repeatForEachItem(items, item -> {
        // don't increment if a new item would put us over our working map size limit
        if (_valueCounts.size() < _maxMappingsDuringPreparation || _valueCounts.containsKey(item)) {
          _valueCounts.addTo(item, 1);
        } else {
          _unknownCount++;
        }
      });
    }
  }

  /**
   * Transformer that uses a provided object-to-index map to look up the index for input values.
   */
  @ValueEquality
  abstract static class Prepared<I, T, R, S extends Prepared<I, T, R, S>> extends AbstractPreparedTransformer1<I, R, S> {
    private static final long serialVersionUID = 1;

    final Object2IntOpenHashMap<T> _indexMap;

    // no-arg constructor for Kryo
    Prepared() {
      _indexMap = null;
    }

    /**
     * Creates a new instance of the transformer.
     *
     * @param indexMap map in which values will be looked up to find their index
     */
    public Prepared(Object2IntOpenHashMap<T> indexMap) {
      super();
      _indexMap = Objects.requireNonNull(indexMap);
    }
  }
}
