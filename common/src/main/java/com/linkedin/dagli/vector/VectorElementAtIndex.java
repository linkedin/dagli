package com.linkedin.dagli.vector;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer2;

@ValueEquality
public class VectorElementAtIndex extends AbstractPreparedTransformer2<Vector, Number, Double, VectorElementAtIndex> {
  private static final long serialVersionUID = 1;

  /**
   * @param vectorInput the input vector from which an element value will be retrieved
   * @return a copy of this instance that will use the specified vector input
   */
  public VectorElementAtIndex withVectorInput(Producer<? extends Vector> vectorInput) {
    return super.withInput1(vectorInput);
  }

  /**
   * Returns a copy of this instance that will use the specified input index.  {@link Number}s are converted to indices
   * using {@link Number#longValue()}, which may involve truncation/rounding.
   *
   * @param indexInput the input index which specifies which element's value will be retrieved from the input vector
   * @return a copy of this instance that will use the specified input index
   */
  public VectorElementAtIndex withIndexInput(Producer<? extends Number> indexInput) {
    return super.withInput2(indexInput);
  }
  /**
   * @param index the index which specifies which element's value will be retrieved from the input vector
   * @return a copy of this instance that will use the specified index
   */
  public VectorElementAtIndex withIndex(long index) {
    return withIndexInput(new Constant<>(index));
  }

  @Override
  public Double apply(Vector vector, Number index) {
    return vector.get(index.longValue());
  }
}
