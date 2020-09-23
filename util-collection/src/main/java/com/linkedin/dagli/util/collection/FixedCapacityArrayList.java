package com.linkedin.dagli.util.collection;

import java.io.Serializable;
import java.util.Arrays;
import java.util.RandomAccess;


/**
 * A list that wraps an underlying array.  Changes to the list "write through" to the wrapped array.  Unlike the list
 * returned by {@link java.util.Arrays#asList(Object[])}, however, {@link FixedCapacityArrayList} is variable-sized up
 * to the capacity of the wrapped array.  Instances cannot grow past this capacity and always wrap their original array.
 *
 * The values in the original array will be untouched so long as they are not part of the list; for example, the
 * {@link #clear()} method will replace elements in the array that were storing list items with nulls, but not any
 * remaining array elements.
 *
 * @param <T> the type of element stored in this list
 */
public class FixedCapacityArrayList<T> extends AbstractFixedCapacityArrayList<T> implements Serializable, RandomAccess {
  private static final long serialVersionUID = 1;

  /**
   * Creates a new <strong>empty</strong> list that will store elements in the provided array.
   *
   * @param wrappedArray the array in which elements will be stored
   */
  @SuppressWarnings("unchecked") // safe because the wrappedArray never escapes from this class typed as T[]
  public FixedCapacityArrayList(Object[] wrappedArray) {
    this((T[]) wrappedArray, 0);
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
  public FixedCapacityArrayList(T[] wrappedArray, int initialSize) {
    super(wrappedArray, initialSize);
  }

  @Override
  public boolean add(T t) {
    _wrapped[_size++] = t;
    return true;
  }

  @Override
  public T set(int index, T element) {
    checkIndex(index);
    T res = _wrapped[index];
    _wrapped[index] = element;
    return res;
  }

  @Override
  public void add(int index, T element) {
    if (index == _size) { // handle common, easy case
      add(element);
      return;
    }

    // make sure index is outside the list
    checkIndex(index);

    // shift subsequent _size - index items up one position (note that arraycopy(...) guarantees this works as intended,
    // even though a simple forward copy loop would not!)
    System.arraycopy(_wrapped, index, _wrapped, index + 1, _size - index);

    _wrapped[index] = element;
  }

  @Override
  public void clear() {
    Arrays.fill(_wrapped, 0, _size, null);
    _size = 0;
  }

  @Override
  public T remove(int index) {
    T result = _wrapped[index];

    // shift remaining elements down one position, overwriting the item at position "index"; note that this arraycopy()
    // call also implicitly checks that index < _size:
    System.arraycopy(_wrapped, index + 1, _wrapped, index, _size - (index + 1));

    _wrapped[_size] = null; // clear item
    _size--; // decrement size
    return result;
  }
}
