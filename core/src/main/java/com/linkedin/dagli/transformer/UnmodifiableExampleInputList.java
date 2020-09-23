package com.linkedin.dagli.transformer;

import com.linkedin.dagli.util.invariant.Arguments;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.RandomAccess;


/**
 * Unmodifiable list of values from a "row" in a multidimensional array.
 *
 * Dagli often passes input data as an Object[][] array, where the second index is the example.  A list of the inputs
 * for example i is thus given by {@code array[0][i], array[1][i]...array[n][i]}.  This is useful for providing typed
 * inputs to variadic transformers.
 *
 * @param <T> the type of input
 */
class UnmodifiableExampleInputList<T> extends AbstractList<T> implements Serializable, RandomAccess {
  private static final long serialVersionUID = 1;

  private final int _size;
  private final T[][] _wrapped;
  private final int _exampleIndex;

  /**
   * Creates a new list of the specified example's inputs.
   *
   * @param wrapped a multidimensional array of values; may be an Object[][] masquerading as T[][]
   * @param exampleIndex the index of the example
   * @param size the number of inputs expected for this example
   */
  UnmodifiableExampleInputList(T[][] wrapped, int exampleIndex, int size) {
    assert size <= wrapped.length;
    assert size >= 0;

    _size = size;
    _wrapped = wrapped;
    _exampleIndex = exampleIndex;
  }

  @Override
  public T get(int index) {
    Arguments.check(index < _size);
    return _wrapped[index][_exampleIndex];
  }

  @Override
  public int size() {
    return _size;
  }
}
