package com.linkedin.dagli.util.collection;

import com.linkedin.dagli.util.invariant.Arguments;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.Objects;
import java.util.RandomAccess;


/**
 * Base class for lists of fixed capacity that wrap a provided array.
 *
 * @param <T> the type of element stored in this list
 */
class AbstractFixedCapacityArrayList<T> extends AbstractList<T> implements Serializable, RandomAccess {
  private static final long serialVersionUID = 1;

  final T[] _wrapped; // may well be Object[] masquerading as Object[]--thus, we do not allow it to escape as T[]
  int _size;

  /**
   * Returns the wrapped array backing this list.  This array may include elements that are not part of the list.
   *
   * @return the wrapped array
   */
  public Object[] getWrappedArray() { // don't return as T[] because the array might not really be of type T[]
    return _wrapped;
  }

  /**
   * Creates a new instance that will wrap the specified array.  The list will contain the first {@code initialSize}
   * elements in the array.
   *
   * @param wrappedArray the array to wrap; so long as the first {@code initialSize} elements are of type T,  it is safe
   *                     to provide an instance that is actually Object[] (or any other supertype of T[]), rather than
   *                     a true T[] array
   * @param initialSize the number of items from the wrapped array to include in the list
   */
  public AbstractFixedCapacityArrayList(T[] wrappedArray, int initialSize) {
    _wrapped = Objects.requireNonNull(wrappedArray);
    _size = Arguments.inInclusiveRange(initialSize, 0, wrappedArray.length, "Initial size",
        () -> "Size cannot exceed length of wrapped array");
  }

  void checkIndex(int index) {
    if (index >= _size) {
      throw new IndexOutOfBoundsException("Attempted to access item at index " + index + " from list of size " + _size);
    }
  }

  @Override
  public T get(int index) {
    checkIndex(index);
    return _wrapped[index];
  }

  @Override
  public int size() {
    return _size;
  }
}
