package com.linkedin.dagli.dag;

import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer2;
import com.linkedin.dagli.transformer.Transformer;


public class Sum extends AbstractPreparedTransformer2<Number, Number, Long, Sum> {
  private static final long serialVersionUID = 1;

  @Override
  public Long apply(Number value0, Number value1) {
    return value0.longValue() + value1.longValue();
  }

  public Sum withInputs(Producer<? extends Number> input0, Producer<? extends Number> input1) {
    return super.withAllInputs(input0, input1);
  }

  @Override
  protected boolean computeEqualsUnsafe(Sum other) {
    return Transformer.sameUnorderedInputs(this, other);
  }

  @Override
  protected int computeHashCode() {
    return Transformer.hashCodeOfUnorderedInputs(this);
  }
}
