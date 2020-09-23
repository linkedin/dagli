package com.linkedin.dagli.util.collection;

/**
 * A list that wraps an underlying array and supports only read operations.  However, changes to the underlying array
 * will be reflected in the list.  The benefit of this class over creating an unmodifiable (and possibly sublist)
 * wrapper around the list returned by {@link java.util.Arrays#asList(Object[])} is efficiency, as this class avoids the
 * creation and intercession of one (or two) wrapper classes.
 *
 * @param <T> the type of element stored in this list
 */
public class UnmodifiableArrayList<T> extends AbstractFixedCapacityArrayList<T> {
  private static final long serialVersionUID = 1;

  /**
   * Creates a new instance that will wrap the specified array.  The list will contain the first {@code size}
   * elements in the array.
   *
   * @param wrappedArray the array to wrap; so long as the first {@code size} elements are of type T,  it is safe
   *                     to provide an instance that is actually Object[] (or any other supertype of T[]), rather than
   *                     a true T[] array
   * @param size the number of items from the wrapped array to include in the list
   */
  public UnmodifiableArrayList(T[] wrappedArray, int size) {
    super(wrappedArray, size);
  }
}
