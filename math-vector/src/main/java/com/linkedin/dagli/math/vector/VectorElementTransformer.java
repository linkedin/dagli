package com.linkedin.dagli.math.vector;

/**
 * Transformers vector elements expressed as index and value arguments, returning the new value for the given index.
 */
@FunctionalInterface
public interface VectorElementTransformer {
  /**
   * Examine the vector element and return a new value for the given index.
   *
   * @param index index of the vector element
   * @param value value of the vector element
   * @return new value for the given index
   */
  double transform(long index, double value);
}