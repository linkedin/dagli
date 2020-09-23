package com.linkedin.dagli.math.vector;

/**
 * Interface for mutable "dense" vectors.  We define a mutable dense vector as a vector with:
 * (1) Indices of non-zero values beginning at 0 (no negative indices associated with non-zero values)
 * (2) A finite number of potentially non-zero value elements with indices 0...n-1, where n is the "capacity" of the
 *     vector.  The capacity is not necessarily fixed and, depending on the implementation, may grow during the lifetime
 *     of the vector.
 * (3) {@link #get(long)} as a O(1) (constant time) operation.
 * (4) {@link #put(long, double)} as a O(1) (constant time) operation when the modified index is within the current
 *     capacity.
 */
public interface MutableDenseVector extends MutableVector, DenseVector {
  /**
   * The <em>maximum</em> capacity of the dense vector.  This will always be the current {@link #capacity()} or larger.
   *
   * Modifying the values of elements at indices 0...maxCapacity()-1 must succeed (barring memory allocation
   * exceptions).  Attempting to modify an element outside of this range will cause a {@link IndexOutOfBoundsException}
   * exception.
   *
   * @return the maximum capacity of the dense vector
   */
  long maxCapacity();
}
