package com.linkedin.dagli.vector;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import java.util.OptionalLong;



/**
 * Produces the highest index of any non-zero element in the input vector.  If the input is a 0-vector (all elements are
 * 0), the return value specified by {@link #withResultOnZeroVector(Long)} is returned instead (by default,
 * <code>null</code> is returned for 0-vectors).
 */
@ValueEquality
public class MaxNonZeroVectorElementIndex
    extends AbstractPreparedTransformer1WithInput<Vector, Long, MaxNonZeroVectorElementIndex> {
  private static final long serialVersionUID = 1;

  private Long _resultOnZeroVector = null;

  /**
   * Returns a copy of this instance that will return the specified value when the input is a zero vector (no non-zero
   * elements).  By default, the returned value is null.
   *
   * @param resultOnZeroVector the value (possibly null) to return when the input is a zero vector
   * @return a copy of this instance that will return the specified value when the input is a zero vector
   */
  public MaxNonZeroVectorElementIndex withResultOnZeroVector(Long resultOnZeroVector) {
    return clone(c -> c._resultOnZeroVector = resultOnZeroVector);
  }

  @Override
  public Long apply(Vector value0) {
    OptionalLong maxIndex = value0.maxNonZeroElementIndex();

    return maxIndex.isPresent() ? maxIndex.getAsLong() : _resultOnZeroVector;
  }
}
