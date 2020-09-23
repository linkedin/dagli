package com.linkedin.dagli.math.vector;

/**
 * Interface for "dense" vectors.  We define a dense vector as a vector with:
 * (1) Indices of non-zero values beginning at 0 (no negative indices associated with non-zero values)
 * (2) A finite number of potentially non-zero value elements with indices 0...n-1, where n is the "capacity" of the
 *     vector.
 * (3) {@link #get(long)} as a O(1) (constant time) operation with respect to the number of elements in the vector.
 */
public interface DenseVector extends Vector {
  /**
   * The current capacity of the dense vector.  This will always be at least {@link #maxNonZeroElementIndex()} + 1.
   * Accessing the values of elements at indices 0...capacity()-1 must be a O(1) (constant time) operation.
   *
   * Implementations of this method should have O(1) time complexity.
   *
   * @return the current capacity of the dense vector
   */
  long capacity();
}
