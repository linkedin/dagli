package com.linkedin.dagli.math.vector;

import java.util.Iterator;


/**
 * {@link VectorElementIterator}s are an {@link Iterator} over {@link VectorElement}s, but also add methods for
 * processing vector elements efficiently, without instantiating {@link VectorElement} objects.
 */
public interface VectorElementIterator extends Iterator<VectorElement> {
  /**
   * Runs a {@link VectorElementConsumer} function over all remaining vector elements in this iterator.
   *
   * @param consumer a function that consumes vector elements to produce some desirable side-effect
   */
  default void forEachRemaining(VectorElementConsumer consumer) {
    while (hasNext()) {
      next(consumer);
    }
  }

  /**
   * Runs a {@link VectorElementPredicate} function over all remaining vector elements in this iterator.  Iteration is
   * halted if the predicate returns false.
   *
   * @param predicate a function that consumes vector elements to produce some desirable side-effect and returns a
   *                  value indicating whether iteration should continue (true to continue, false to stop)
   */
  default void forEachRemainingUntilFalse(VectorElementPredicate predicate) {
    while (hasNext() && mapNext(predicate::test)) {
      // this space deliberately left blank
    }
  }

  @Override
  default VectorElement next() {
    return mapNext(VectorElement::new);
  }

  /**
   * Processes the vector element with the provided function and returns the result.  This consumes the current vector
   * element and advances the iterator.
   *
   * @param mapper the mapping function that returns the desired result
   * @param <T> the return type of the mapping function
   * @return the return value of the mapping function as applied to the next element
   */
  <T> T mapNext(VectorElementFunction<T> mapper);

  /**
   * Processes the vector element with the provided function to obtain some desirable side-effect.  This consumes the
   * current vector element and advances the iterator.
   *
   * @param consumer the consuming function that has some desirable side-effect
   */
  default void next(VectorElementConsumer consumer) {
    mapNext((index, value) -> {
      consumer.consume(index, value);
      return null;
    });
  }


}
