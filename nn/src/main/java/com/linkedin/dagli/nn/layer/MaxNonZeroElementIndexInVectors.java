package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;


/**
 * Calculates the maximum non-zero element index among all the vectors in a list.  If no vector in the list has a
 * non-zero element with an index greater than 0, the result is 0.
 */
@ValueEquality
class MaxNonZeroElementIndexInVectors
    extends AbstractPreparedTransformer1WithInput<Iterable<? extends Vector>, Long, MaxNonZeroElementIndexInVectors> {
  private static final long serialVersionUID = 1;

  @Override
  public Long apply(Iterable<? extends Vector> vectors) {
    long max = 0;
    for (Vector vec : vectors) {
      max = Math.max(max, vec.maxNonZeroElementIndex().orElse(0));
    }
    return max;
  }
}
