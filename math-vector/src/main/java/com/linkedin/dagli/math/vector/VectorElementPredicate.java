package com.linkedin.dagli.math.vector;

/**
 * Interface for performing some boolean test against a vector element.
 */
@FunctionalInterface
public interface VectorElementPredicate {
  /**
   * Tests the provided vector element.
   *
   * @param index the index of the element
   * @param value the value of the element
   * @return true or false indicates success or failure of the test.
   */
  boolean test(long index, double value);
}
