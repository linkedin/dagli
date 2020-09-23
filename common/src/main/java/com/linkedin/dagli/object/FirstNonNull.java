package com.linkedin.dagli.object;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.reducer.Reducer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformerVariadic;
import java.util.Collection;
import java.util.Collections;
import java.util.List;



/**
 * A coalescing transformer that returns its first non-null input in its input list.  If no inputs are non-null, null
 * is returned.
 */
@ValueEquality
public class FirstNonNull<T> extends AbstractPreparedTransformerVariadic<T, T, FirstNonNull<T>> {
  private static final long serialVersionUID = 1;
  // check to see if we can statically determine our result from constant inputs
  @SuppressWarnings("unchecked")
  private static final List<Reducer<FirstNonNull<?>>> REDUCERS = Collections.singletonList(
      (target, context) -> {
        List<? extends Producer<?>> parents = context.getParents(target);
        for (Producer<?> parent : parents) {
          if (parent instanceof Constant) {
            if (((Constant<?>) parent).getValue() != null) {
              context.replace(target, (Producer) parent);
            } else { // null constant
              continue;
            }
          }
          return;
        }
      });

  @Override
  protected Collection<? extends Reducer<? super FirstNonNull<T>>> getGraphReducers() {
    return REDUCERS;
  }

  @Override
  public T apply(List<? extends T> values) {
    for (T val : values) {
      if (val != null) {
        return val;
      }
    }

    return null;
  }
}
