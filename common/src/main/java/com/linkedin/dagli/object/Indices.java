package com.linkedin.dagli.object;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.util.collection.Iterables;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.function.Consumer;


/**
 * Maps arbitrary objects contained in {@link Iterable}s to consecutive integers starting at 0.
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
 * The index space is common across all positions in the list; i.e. the object corresponding to index 42 at any position
 * in the produced list is the same.
 *
 * @param <T> the type of item indexed by this transformer
 */
@ValueEquality
public class Indices<T>
    extends AbstractIndex<Iterable<? extends T>, T, IntList, Indices.Prepared<T>, Indices<T>> {

  private static final long serialVersionUID = 1;

  @Override
  protected Prepared<T> getPrepared(Object2IntOpenHashMap<T> indexMap) {
    return new Prepared<>(indexMap);
  }

  @Override
  protected void repeatForEachItem(Iterable<? extends T> items, Consumer<T> consumer) {
    items.forEach(consumer);
  }

  /**
   * Transformer that uses an existing map from items to indices to map objects (provided as {@link Iterable}s) to
   * {@link IntList}s of their corresponding indices.
   *
   * @param <T> the type of the item to be indexed
   */
  @ValueEquality
  static class Prepared<T> extends AbstractIndex.Prepared<Iterable<? extends T>, T, IntList, Prepared<T>> {
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
    public IntList apply(Iterable<? extends T> items) {
      final IntArrayList result;

      if (this._indexMap.defaultReturnValue() == IGNORED_ITEM_INDEX) {
        // ignoring unmapped items
        result = new IntArrayList();
        for (T item : items) {
          int index = _indexMap.getInt(item);
          if (index != IGNORED_ITEM_INDEX) {
            result.add(index);
          }
        }
      } else {
        result = new IntArrayList(Math.toIntExact(Iterables.size64(items)));
        for (T item : items) {
          result.add(_indexMap.getInt(item));
        }
      }

      return result;
    }
  }
}