package com.linkedin.dagli.object;

import com.linkedin.dagli.annotation.equality.ValueEquality;


/**
 * Produces the minimum value of all its {@link Comparable} inputs (i.e. the resulting value is the same for every input
 * and is the minimum value that was seen during preparation).
 *
 * "Minimum" is determined by the natural ordering of the objects.  By default, the result is only null if all input
 * values are null.
 *
 * @param <T> the type of the objects whose minimum should be found
 */
@ValueEquality
public class Min<T extends Comparable<T>> extends AbstractMinMaxTransformer<T, Min<T>> {
  private static final long serialVersionUID = 1;

  @Override
  boolean isFirstPreferred(T first, T second) {
    return first.compareTo(second) < 0;
  }
}
