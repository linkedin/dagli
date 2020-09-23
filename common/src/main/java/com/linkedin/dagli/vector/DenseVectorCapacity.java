package com.linkedin.dagli.vector;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;


/**
 * Calculates the capacity of a {@link DenseVector} as returned by {@link DenseVector#capacity()}.
 */
@ValueEquality
public class DenseVectorCapacity extends AbstractPreparedTransformer1WithInput<DenseVector, Long, DenseVectorCapacity> {
  private static final long serialVersionUID = 1;

  @Override
  public Long apply(DenseVector value) {
    return value.capacity();
  }
}
