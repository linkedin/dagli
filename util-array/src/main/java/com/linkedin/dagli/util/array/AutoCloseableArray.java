package com.linkedin.dagli.util.array;

import com.linkedin.dagli.util.exception.Exceptions;


/**
 * Wraps an array of {@link AutoCloseable} elements and itself implements {@link AutoCloseable}, calling
 * {@link AutoCloseable#close()} on all (non-null) array elements when this instance closes.  This is primarily useful
 * for try-finally initializers.
 *
 * Checked exceptions thrown by the elements' {@link AutoCloseable#close()} will be rethrown as unchecked exceptions
 * in accordance with {@link Exceptions#asRuntimeException(Throwable)}.  Elements are
 * closed sequentially in the array and an exception will prevent the closure of subsequent elements.
 */
public class AutoCloseableArray<T extends AutoCloseable> implements AutoCloseable {
  private final T[] _array;

  /**
   * @return the array wrapped by this {@link AutoCloseableArray}
   */
  public T[] get() {
    return _array;
  }

  /**
   * Convenience method that returns the element with the given index.
   *
   * @param index the index of the element to fetch
   * @return the fetched element
   */
  public T get(int index) {
    return _array[index];
  }

  /**
   * Wraps an array of {@link AutoCloseable} elements (some of which may be null) as an {@link AutoCloseableArray}.
   * No copy of the array is made; rather, {@link AutoCloseableArray} stores a reference and this is what will be
   * returned by the {@link #get()} method.
   *
   * @param array the array to wrap
   */
  public AutoCloseableArray(T[] array) {
    _array = array;
  }

  @Override
  public void close() {
    close(_array);
  }

  /**
   * Given an array of AutoCloseable elements, closes each element in the array.
   *
   * If the array is null, this method is a no-op.  If an array element is null, it is ignored.  If an exception
   * occurs while closing an element, it will be converted to a RuntimeException via
   * {@link Exceptions#asRuntimeException(Throwable)} and re-thrown; any later elements in the array will not be closed.
   *
   * @param array the array of AutoCloseable elements that should all be closed; may be null, in which case this method
   *              is a no-op
   * @param <T> the type of element in the array
   */
  public static <T extends AutoCloseable> void close(T[] array) {
    if (array != null) {
      try {
        for (T element : array) {
          if (element != null) {
            element.close();
          }
        }
      } catch (Throwable e) {
        throw Exceptions.asRuntimeException(e);
      }
    }
  }
}
