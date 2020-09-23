package com.linkedin.dagli.math.vector;

/**
 * Consumes vector elements expressed as index and value arguments.
 */
@FunctionalInterface
public interface VectorElementConsumer {
  /**
   * Do something with the vector element
   *
   * @param index index of the vector element
   * @param value value of the vector element
   */
  void consume(long index, double value);
}
