package com.linkedin.dagli.object;

import com.linkedin.dagli.annotation.equality.ValueEquality;


/**
 * Produces the maximum value of all its {@link Comparable} inputs (i.e. the resulting value is the same for every input
 * and is the maximum value that was seen during preparation).
 *
 * "Maximum" is determined by the natural ordering of the objects.  By default, the result is only null if all input
 * values are null.
 *
 * @param <T> the type of the objects whose maximum should be found
 */
@ValueEquality
public class Max<T extends Comparable<T>> extends AbstractMinMaxTransformer<T, Max<T>> {
  private static final long serialVersionUID = 1;

  @Override
  boolean isFirstPreferred(T first, T second) {
    return first.compareTo(second) > 0;
  }
}
