package com.linkedin.dagli.objectio;

/**
 * Functional interface for {@link ObjectIterator}'s family of {@code next...()} methods; useful for simplify the
 * implementation of iterators that wrap other iterators.
 *
 * @param <T> the type of item provided by the iterator
 */
@FunctionalInterface
public interface ObjectIteratorNext<T> {
  int next(ObjectIterator<T> iterator, Object[] target, int offset, int count);
}
