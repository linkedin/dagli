package com.linkedin.dagli.object;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.function.Consumer;


/**
 * Maps arbitrary objects to consecutive integers starting at 0.
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
 * @param <T> the type of item indexed by this transformer
 */
@ValueEquality
public class Index<T>
    extends AbstractIndex<T, T, Integer, Index.Prepared<T>, Index<T>> {

  private static final long serialVersionUID = 1;

  @Override
  protected Prepared<T> getPrepared(Object2IntOpenHashMap<T> indexMap) {
    return new Prepared<>(indexMap);
  }

  @Override
  protected void repeatForEachItem(T items, Consumer<T> consumer) {
    consumer.accept(items); // items is always a single item we can consume directly
  }

  /**
   * Transformer that uses an existing map from items to indices to map objects to their corresponding index values.
   *
   * @param <T> the type of the item to be indexed
   */
  @ValueEquality
  static class Prepared<T> extends AbstractIndex.Prepared<T, T, Integer, Prepared<T>> {
    private static final long serialVersionUID = 1;

    // no-arg constructor for Kryo
    private Prepared() { }

    /**
     * Creates a new instance of the transformer.
     *
     * @param indexMap map in which values will be looked up to find their index
     */
    public Prepared(Object2IntOpenHashMap<T> indexMap) {
      super(indexMap);
    }

    @Override
    public Integer apply(T value0) {
      int val = _indexMap.getInt(value0);
      return val == IGNORED_ITEM_INDEX ? null : val;
    }
  }
}