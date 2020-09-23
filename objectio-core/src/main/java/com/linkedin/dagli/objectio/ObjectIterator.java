/*
 * Copyright 2017 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */

package com.linkedin.dagli.objectio;

import java.util.Iterator;


/**
 * An interface for an iterator that supports additional operations, such as skipping elements or retrieving many items
 * in a single call rather than getting them one at a time.
 *
 * ObjectIterators implement {@link Iterator} but also have methods for filling arrays with subsequent elements that
 * are somewhat analogous to the methods for fetching bytes via an {@link java.io.InputStream}.
 *
 * @apiNote  Yes, all Iterators are already "object" iterators.  The name "ObjectIterator" is used for naming
 *           consistency with the rest of the API.
 *
 * @param <T> the type of object iterated over
 */
public interface ObjectIterator<T> extends Iterator<T>, AutoCloseable {
  /**
   * Tries to the obtain {@code count} items from the iterator, without blocking.
   *
   * This is one of three {@code next...(...)} methods for retrieving multiple items at once:
   * (1) {@link #next(Object[], int, int)} returns a specified number of items, possibly blocking to do so
   * (2) {@link #nextAvailable(Object[], int, int)} returns up to a specified number of items, blocking only if doing
   *     so is required to obtain at least one item.
   * (3) {@link #tryNextAvailable(Object[], int, int)} returns up to a specified number of items and never blocks.
   *
   * This method will obtain 0 items if no items can be returned without blocking.  If the iterator can be determined
   * (without blocking) to be exhausted ({@link #hasNext()} would return false), -1 will be returned.  Returning a
   * positive value less than {@code count} does <i>not</i> necessarily mean that no more elements are available without
   * blocking and an immediately subsequent call to this method may be able to retrieve more.
   *
   * Note that some implementations always require blocking to obtain elements, in which case this method will never
   * succeed in obtaining <i>any</i> items.
   *
   * Implementations that have a trivial but non-zero chance of blocking (e.g. on an almost never-contested lock) or
   * busy-wait for a short period (e.g. doing repeated compare-and-sets to accomplish atomic updates) are allowed.
   *
   * Care must be taken if the iterated type is generic as it is possible to pollute the heap.  For example, if the
   * iterated type is {@code List<String>}, this method will pollute the heap if passed a destination array of type
   * {@code List<Integer>[]} as lists of strings will then be masquerading as lists of integers, causing eventual
   * downstream typing errors.
   *
   * @param destination the array to which the next items will be copied; if there are fewer than {@code count} items
   *                    immediately available from the iterator, some or all of the unwritten elements (up to
   *                    {@code destination[offset + count - 1]}) <strong>may</strong> be replaced by nulls
   * @param offset the offset in the array at which copying begins
   * @param count the maximum number of items to attempt to obtain without blocking
   * @return the number of items that were actually copied, or -1 if the iterator is known to be exhausted
   */
  default int tryNextAvailable(Object[] destination, int offset, int count) {
    return 0;
  }

  /**
   * Obtains the next sequence of items.
   *
   * This is one of three {@code next...()} methods for retrieving multiple items at once:
   * (1) {@link #next(Object[], int, int)} returns a specified number of items, possibly blocking to do so
   * (2) {@link #nextAvailable(Object[], int, int)} returns up to a specified number of items, blocking only if doing
   *     so is required to obtain at least one item.
   * (3) {@link #tryNextAvailable(Object[], int, int)} returns up to a specified number of items and never blocks.
   *
   * The difference between this method and {@link #nextAvailable(Object[], int, int)} is that this method will obtain
   * all {@code count} items (unless the iterator is exhausted), rather than merely one or more.  The disadvantage is
   * that this may entail a higher computational cost per item retrieved, although the difference is likely to be
   * negligible outside of performance critical code).
   *
   * Care must be taken if the iterated type is generic as it is possible to pollute the heap.  For example, if the
   * iterated type is {@code List<String>}, this method will pollute the heap if passed a destination array of type
   * {@code List<Integer>[]} as lists of strings will then be masquerading as lists of integers, causing eventual
   * downstream typing errors.
   *
   * @param destination the array to which the next items will be copied; if there are fewer than {@code count} items
   *                    remaining in the iterator, some or all of the unwritten elements (up to
   *                    {@code destination[offset + count - 1]}) <strong>may</strong> be replaced by nulls
   * @param offset the offset in the array at which copying begins
   * @param count the number of items to copy
   *
   * @return the actual number of items copied, which will be count unless the end of the iterator is reached, in which
   *         case only the elements remaining in the iterator will be copied
   * @throws ArrayStoreException if the actual component type of the {@code destination} array is a subtype of T and the
   *                             item to be stored is not of that type
   */
  default int next(Object[] destination, int offset, int count) {
    // handle common case where all requested items are available
    int fetched = nextAvailable(destination, offset, count);
    if (fetched == count || fetched == 0) {
      return fetched;
    }

    int readSoFar = fetched;
    do {
      fetched = nextAvailable(destination, offset + readSoFar, count - readSoFar);
      readSoFar += fetched;
    } while (readSoFar < count && fetched > 0);

    return readSoFar;
  }

  /**
   * Obtains as many as {@code count} items from the iterator without blocking, but always obtains at least one item
   * even if blocking is required.  Returns 0 if the iterator is exhausted.
   *
   * This is one of three {@code next...()} methods for retrieving multiple items at once:
   * (1) {@link #next(Object[], int, int)} returns a specified number of items, possibly blocking to do so
   * (2) {@link #nextAvailable(Object[], int, int)} returns up to a specified number of items, blocking only if doing
   *     so is required to obtain at least one item.
   * (3) {@link #tryNextAvailable(Object[], int, int)} returns up to a specified number of items and never blocks.
   *
   * Care must be taken if the iterated type is generic as it is possible to pollute the heap.  For example, if the
   * iterated type is {@code List<String>}, this method will pollute the heap if passed a destination array of type
   * {@code List<Integer>[]} as lists of strings will then be masquerading as lists of integers, causing eventual
   * downstream typing errors.
   *
   * @param destination the array to which the next items will be copied; if there are fewer than {@code count} items
   *                    remaining in the iterator, some or all of the unwritten elements (up to
   *                    {@code destination[offset + count - 1]}) <strong>may</strong> be replaced by nulls
   * @param offset the offset in the array at which copying begins
   * @param count the maximum number of items to copy
   *
   * @return the actual number of items copied
   * @throws ArrayStoreException if the actual component type of the {@code destination} array is a subtype of T and the
   *                             item to be stored is not of that type
   */
  default int nextAvailable(Object[] destination, int offset, int count) {
    int fetched = tryNextAvailable(destination, offset, count);
    if (fetched != 0) {
      return Math.max(fetched, 0);
    }

    int copied = 0;
    while (hasNext() && copied < count) {
      destination[offset + copied] = next();
      copied++;
    }

    return copied;
  }

  /**
   * Obtains the next sequence of items.
   *
   * At most destination.length elements will be copied.  Fewer elements may be copied if the end of the iterator is
   * reached.
   *
   * Care must be taken if the iterated type is generic as it is possible to pollute the heap.  For example, if the
   * iterated type is {@code List<String>}, this method will pollute the heap if passed a destination array of type
   * {@code List<Integer>[]} as lists of strings will then be masquerading as lists of integers, causing eventual
   * downstream typing errors.
   *
   * @param destination the array to which the next items will be copied; if there are not enough items remaining in the
   *                    iterator to fill the array some or all of the remaining elements <strong>may</strong> be
   *                    replaced by nulls
   *
   * @return the actual number of items copied, which may be less than destination.length if the end of the iterator is
   *         reached
   */
  default int next(Object[] destination) {
    return next(destination, 0, destination.length);
  }

  /**
   * Skips the next element.
   *
   * @return true if an item was skipped, false if the iterator was already at its end (hasNext() == false).
   */
  default boolean skip() {
    return skip(1) > 0;
  }

  /**
   * Skips the given number of elements.
   *
   * If the requested number of elements exceeds the number remaining, skips to the end of the iterator (such that
   * hasNext() == false).
   *
   * Skipping elements may be faster than reading them with next().  The default implementation simply calls next()
   * repeatedly.
   *
   * @param toSkip the number of elements to skip
   * @return the number of elements skipped; if fewer than toSkip items remain in the iterator, the number of items
   *         remaining in the iterator is returned.
   */
  default long skip(long toSkip) {
    for (long i = 0; i < toSkip; i++) {
      if (!hasNext()) {
        return i;
      }
      next();
    }
    return toSkip;
  }

  /**
   * "Closes" this iterator and releases its resources.  Further operations on the iterator after being
   * closed are undefined.
   */
  @Override
  void close();

  /**
   * Casts an instance to an effective "supertype" interface.  The semantics of {@link ObjectIterator} guarantee that
   * the returned type is valid for the instance.
   *
   * Note that although this and other {@code cast(...)} methods are safe, this safety extends only to the interfaces
   * for which they are implemented.  The covariance and contravariance relationships existing for these interfaces do
   * not necessarily hold for their implementing classes.
   *
   * @param iterator the instance to cast
   * @param <R> the type of item read by the returned iterator
   * @return the passed iterator, typed to a new "supertype" interface of the original
   */
  @SuppressWarnings("unchecked")
  static <R> ObjectIterator<R> cast(ObjectIterator<? extends R> iterator) {
    return (ObjectIterator<R>) iterator;
  }

  /**
   * Gets a canonical iterator containing no elements.
   *
   * @param <T> the type of element enumerated by this iterator (though only in principle, since it's empty)
   * @return an iterator over zero elements
   */
  @SuppressWarnings("unchecked")
  static <T> ObjectIterator<T> empty() {
    return EmptyIterator.INSTANCE;
  }

  /**
   * Gets an iterator that iterates over a single value
   *
   * @param element the element that will be in the iterator
   * @param <T> the type of the element
   * @return an iterator containing a single copy of the provided element
   */
  static <T> ObjectIterator<T> singleton(T element) {
    return new ConstantReader.Iterator<>(element, 1);
  }

  /**
   * If iterator is a ObjectIterator, returns iterator.
   *
   * Otherwise, creates a ObjectIterator wrapper around iterator and returns the wrapper.
   *
   * @param iterator an iterator to wrap
   * @param <T> the type of element in the iterator
   * @return a ObjectIterator containing the elements of the iterator
   */
  static <T> ObjectIterator<? extends T> wrap(Iterator<T> iterator) {
    if (iterator instanceof ObjectIterator) {
      return (ObjectIterator<T>) iterator;
    }

    return new IterableReader.Iterator<>(iterator);
  }
}
