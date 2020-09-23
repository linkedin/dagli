package com.linkedin.dagli.list;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import com.linkedin.dagli.util.collection.Iterables;


/**
 * Gets the size of an input {@link Iterable} as a long.
 */
@ValueEquality
public class Size64 extends AbstractPreparedTransformer1WithInput<Iterable<?>, Long, Size64> {
  private static final long serialVersionUID = 1;

  @Override
  public Long apply(Iterable<?> iterable) {
    return Iterables.size64(iterable);
  }
}
