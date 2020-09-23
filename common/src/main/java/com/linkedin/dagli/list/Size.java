package com.linkedin.dagli.list;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import com.linkedin.dagli.util.collection.Iterables;


/**
 * Gets the size of an input {@link Iterable} as a (32-bit) integer.  An exception will be thrown during DAG execution
 * if the input's size exceeds {@link Integer#MAX_VALUE}.
 */
@ValueEquality
public class Size extends AbstractPreparedTransformer1WithInput<Iterable<?>, Integer, Size> {
  private static final long serialVersionUID = 1;

  @Override
  public Integer apply(Iterable<?> iterable) {
    return Math.toIntExact(Iterables.size64(iterable));
  }
}
