package com.linkedin.dagli.math.vector;

/**
 * Standard interface for mutable vectors.  Not all mutable vectors support modifying aribtrary indices; e.g.
 * dense vectors can only permit modification of their underlying array elements.  Attempting to modify elements at
 * indices outside the minMutableIndex() and maxMutableIndex() range will result in an (Array)IndexOutOfBounds exception.
 */
public interface MutableVector extends Vector {
  /**
   * Sets the vector element at the given index to the given value.
   * Note that if the underlying vector stores values as floats the value may be truncated.
   *
   * @param index the index at which to set the value.
   * @param value the (double) value to set.  This may be truncated to a float depending on the implementation.
   */
  void put(long index, double value);

  /**
   * Transforms every non-zero element in the vector, invoking <code>transformer</code> to determine its new value.
   *
   * @param transformer the transformer to apply to every non-zero element
   */
  void transformInPlace(VectorElementTransformer transformer);

  /**
   * Adds the given amount to the value of the specified vector element.
   *
   * @param index the index of the vector element
   * @param amount the amount to increase the value (can be negative)
   * @return the previous value, <strong>before</strong> increasing
   */
  default double increase(long index, double amount) {
    double oldVal = get(index);
    put(index, oldVal  + amount);
    return oldVal;
  }

  /**
   * Adds another vector to this one.
   *
   * @param other the vector to add to this one; it will not be modified.
   */
  default void addInPlace(Vector other) {
    other.forEach(element -> put(element.getIndex(), element.getValue() + get(element.getIndex())));
  }

  /**
   * Multiplies this vector by a scalar.
   *
   * @param scalar a multiplier that will be applied to each element.
   */
  default void multiplyInPlace(double scalar) {
    transformInPlace((index, value) -> value * scalar);
  }

  /**
   * Divides each component of the vector by a scalar.
   *
   * @param scalar the divisor that will be applied to each element.
   */
  default void divideInPlace(double scalar) {
    transformInPlace((index, value) -> value / scalar);
  }
}
