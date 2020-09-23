package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;


/**
 * Finds the maximum value, expressed as a long, from a collection of {@link Number}s.  If an empty collection is
 * passed, the result will be {@link Long#MAX_VALUE}.
 */
@ValueEquality
class MaxLongInIterable
    extends AbstractPreparedTransformer1WithInput<Iterable<? extends Number>, Long, MaxLongInIterable> {
  private static final long serialVersionUID = 1;

  @Override
  public Long apply(Iterable<? extends Number> values) {
    long max = Long.MIN_VALUE;

    for (Number number : values) {
      max = Math.max(number.longValue(), max);
    }

    return max;
  }
}
