package com.linkedin.dagli.map;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.preparer.AbstractStreamPreparer1;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.transformer.AbstractPreparableTransformer1WithInput;
import com.linkedin.dagli.transformer.ConstantResultTransformation1;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;


/**
 * Transformer that counts the number of times each distinct (as determined by {@link Object#equals(Object)}) value is
 * observed across all examples, producing a multiset represented by a
 * {@link Object2LongMap} where each observed value is associated with the number of times
 * it was observed (this transformer is constant-result and always produces the same multiset instance).
 *
 * The returned {@link Object2LongMap} is presently capable of storing no more than {@code 805306368} (~800 million)
 * <strong>distinct</strong> values.  Once this limit is reached, any subsequently encountered values not already in
 * the multiset will be ignored.
 */
@ValueEquality
public class Multiset<T> extends AbstractPreparableTransformer1WithInput<
    T,
    Object2LongMap<T>,
    ConstantResultTransformation1.Prepared<T, Object2LongMap<T>>,
    Multiset<T>> {
  private static final long serialVersionUID = 1;

  @Override
  protected boolean hasIdempotentPreparer() {
    return true;
  }

  @Override
  protected boolean hasAlwaysConstantResult() {
    return true;
  }

  @Override
  protected Preparer getPreparer(PreparerContext context) {
    return new Preparer();
  }

  /**
   * Preparer that computes the multiset.
   */
  private class Preparer extends
      AbstractStreamPreparer1<T, Object2LongMap<T>, ConstantResultTransformation1.Prepared<T, Object2LongMap<T>>> {
    private final Object2LongOpenHashMap<T> _counts = new Object2LongOpenHashMap<>();

    @Override
    public PreparerResult<ConstantResultTransformation1.Prepared<T, Object2LongMap<T>>> finish() {
      // in case you're wondering why we convert the open hash map with primitive values (lower memory consumption)
      // to a HashMap, it's because the HashMap's values may be returned many times; it's better to box each long
      // once to return an indefinite number of clones of that Long by recreating it on each input.
      return new PreparerResult<>(new ConstantResultTransformation1.Prepared<>(_counts));
    }

    @Override
    public void process(T value) {
      if (_counts.size() == 805306368) {
        // can't add new items
        if (!_counts.containsKey(value)) {
          return;
        }
      }
      _counts.addTo(value, 1);
    }
  }
}
