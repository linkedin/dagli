package com.linkedin.dagli.reducer;

import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.TransformerVariadic;
import java.util.List;


/**
 * Reduces the case where a variadic transformer is effectively a no-op (the input is equivalent to the output) when it
 * has a single input, in which case the no-op transformer is removed from the graph.
 *
 * Note that while input should be <em>equivalent</em> to the output, it does not have been satisfy
 * {@link Object#equals(Object)}.  For example, {@code CompositeSparseVector} is effectively a no-op with a single
 * input vector, since even though the output of the transformer would be a different sparse vector, it would be an
 * arbitrary re-mapping of the elements (and {@code CompositeSparseVector} does not promise any specific re-mapping).
 *
 * Conversely, {@code DensifiedVector} is <strong>not</strong> a no-op with a single input (even if that input is a
 * dense vector), since it remaps vector elements in a prescribed manner into a contiguous index space.
 */
public class RemoveIfUnaryReducer<R> implements Reducer<TransformerVariadic<? extends R, R>> {
  @Override
  public Level getLevel() {
    return Level.ESSENTIAL; // high value relative to the cost
  }

  @Override
  public void reduce(TransformerVariadic<? extends R, R> target, Context context) {
    List<? extends Producer<? extends R>> parents = context.getParents(target);
    if (parents.size() == 1) {
      context.tryReplaceUnviewed(target, () -> context.getParents(target).get(0));
    }
  }

  @Override
  public boolean equals(Object o) {
    return o != null && getClass() == o.getClass();
  }

  @Override
  public int hashCode() {
    return 0x773f771d; // arbitrary random value
  }
}
