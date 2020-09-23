package com.linkedin.dagli.dag;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer2;
import java.util.Objects;

@ValueEquality
public class IsMatch<T> extends AbstractPreparedTransformer2<T, T, T, IsMatch<T>> {
  private static final long serialVersionUID = 1;

  @Override
  public T apply(T value0, T value1) {
    return Objects.equals(value0, value1) ? value0 : null;
  }

  public IsMatch<T> withInputs(Producer<? extends T> input0, Producer<? extends T> input1) {
    return super.withAllInputs(input0, input1);
  }
}
