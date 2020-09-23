package com.linkedin.dagli.array;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import java.util.Arrays;
import java.util.List;


/**
 * Transforms an array of objects into a list that is backed by the input array using {@link Arrays#asList(Object[])}.
 * Changes to the array will be reflected in the list (and vice-versa).
 *
 * @param <T> the type of element in the input array
 */
@ValueEquality
public class ArrayAsList<T> extends AbstractPreparedTransformer1WithInput<T[], List<T>, ArrayAsList<T>> {
  private static final long serialVersionUID = 1;

  public ArrayAsList() { }

  public ArrayAsList(Producer<? extends T[]> input) {
    super(input);
  }

  @Override
  public List<T> apply(T[] array) {
    return Arrays.asList(array);
  }
}
