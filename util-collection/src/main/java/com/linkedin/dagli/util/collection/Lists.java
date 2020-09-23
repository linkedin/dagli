package com.linkedin.dagli.util.collection;

import java.util.List;
import java.util.stream.Collectors;


/**
 * Utility methods for {@link List}s.
 */
public abstract class Lists {
  private Lists() { }

  /**
   * Returns a list containing the first {@code limit} elements of a provided list, or the original list itself if
   * {@code limit >= list.size()} at the time of this call.
   *
   * Notice that this behavior diverges from {@link List#subList(int, int)}, which may return a new sublist view
   * instance even if the view is equivalent to the original list.
   *
   * @param list the original list whose first {@code limit} elements are to be returned as a list
   * @param limit the maximum size for the returned list
   * @param <T> the type of element in the list
   * @return a sublist containing the first {@code limit} elements of the provided list, or the original list itself if
   *         {@code limit >= list.size()} at the time of this call.
   */
  public static <T> List<T> limit(List<T> list, int limit) {
    return limit >= list.size() ? list : list.subList(0, limit);
  }

  /**
   * Given a list of lists, returns a (possibly different) list of lists where:
   * (1) the outer list (the single, encapsulating list that contains the other lists) will have its size limited to
   *     {@code outerLimit}
   * (1) the inner lists (the encapsulated lists of element type {@code T}) will have their sizes limited to
   *     {@code innerLimit}
   *
   * The method attempts to avoid creating new sublist views where possible, preferring the original lists.
   *
   * @param outerList the original list of lists that will be constrained by the given limits
   * @param outerLimit the limit on the size of the outer list
   * @param innerLimit the limit on the sizes of the inner lists
   * @param <T> the type of element in the list
   * @return a list of lists that uses sublist views as needed to enforce the specified limits; may be the original
   *         hyperlist
   */
  public static <T> List<? extends List<? extends T>> limit(List<? extends List<? extends T>> outerList, int outerLimit, int innerLimit) {
    List<? extends List<? extends T>> limitedOuterList = limit(outerList, outerLimit);

    for (List<? extends T> innerList : limitedOuterList) {
      if (innerList.size() > innerLimit) {
        // creating a new outer list cannot be avoided, since we have to modify at least one of the entries
        return limitedOuterList.stream().map(inner -> limit(inner, innerLimit)).collect(Collectors.toList());
      }
    }

    return limitedOuterList;
  }

  /**
   * Given a "matrix" defined as a list of lists (which is taken to be "row-major" such that the inner lists specify
   * each row of the matrix), copies a column of matrix values to a provided array.
   *
   * Note that this method will also retrieve the <i>row</i> of a <i>column-major</i> "matrix" where the inner lists are
   * the columns.
   *
   * @param matrix the "matrix", comprised of a list of lists
   * @param columnIndex the 0-based index of the column; each contained list within the "matrix" must have an element at
   *                    this position, or an {@link IndexOutOfBoundsException} will be thrown
   * @param destination the target array that will receive the copied column elements; must be of length at least
   *                    {@code matrix.size()}
   * @param <T> the type of element being copied
   */
  public static <T> void copyColumnToArray(List<? extends List<? extends T>> matrix, int columnIndex, T[] destination) {
    for (int i = 0; i < matrix.size(); i++) {
      destination[i] = matrix.get(i).get(columnIndex);
    }
  }
}
