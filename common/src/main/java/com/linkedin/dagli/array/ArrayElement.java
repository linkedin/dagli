package com.linkedin.dagli.array;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import com.linkedin.dagli.util.invariant.Arguments;


/**
 * Transformer that returns a specific element from an input array.
 *
 * @param <T> the type of element in the input array
 */
@ValueEquality
public class ArrayElement<T> extends AbstractPreparedTransformer1WithInput<T[], T, ArrayElement<T>> {
  private static final long serialVersionUID = 1;

  private int _index = -1;

  /**
   * Returns a copy of this instance that will fetch the specified element from the input array.
   *
   * @param index the 0-based element index to fetch; this must not be greater than or equal to the length of the
   *              inputted arrays
   * @return a copy of this instance that will fetch the specified element from the input array
   */
  public ArrayElement<T> withIndex(int index) {
    Arguments.check(index >= 0, "Index must be >= 0");
    return clone(c -> c._index = index);
  }

  @Override
  public T apply(T[] value0) {
    return value0[_index];
  }

  @Override
  public void validate() {
    super.validate();
    Arguments.check(_index >= 0, "Index of ArrayElement transformer must be set via withIndex(...)");
  }
}
