package com.linkedin.dagli.list;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformerVariadic;
import java.util.ArrayList;
import java.util.List;


/**
 * Creates a list from the values produced by one or more producers.
 *
 * @param <T> the type of object being combined into a list
 */
@ValueEquality
public class VariadicList<T> extends AbstractPreparedTransformerVariadic<T, List<T>, VariadicList<T>> {
  private static final long serialVersionUID = 1;

  /**
   * Creates a new instance that initially has no inputs (these must be specified with {@link #withInputs(Producer[])})
   */
  public VariadicList() { }

  /**
   * Creates a new instance that will create a list from the provided inputs
   * @param inputs the inputs to listify
   */
  @SafeVarargs
  public VariadicList(Producer<? extends T>...inputs) {
    super(inputs);
  }

  /**
   * Creates a new instance that will create a list from the provided inputs
   * @param inputs the inputs to listify
   */
  public VariadicList(List<? extends Producer<? extends T>> inputs) {
    super(inputs);
  }

  @Override
  public List<T> apply(List<? extends T> values) {
    return new ArrayList<>(values); // create a copy that can safely persist past the end of this method
  }
}
