package com.linkedin.dagli.math.vector;

/**
 * Consumes a vector element as two primitive arguments, used in contexts where creating a {@link VectorElement} object
 * would be an unnecessary and avoidable computational expense.
 */
@FunctionalInterface
public interface VectorElementFunction<T> {
  /**
   * Do something with a vector element
   *
   * @param index the index of the element
   * @param value the value of the element
   */
  T apply(long index, double value);
}
