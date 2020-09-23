package com.linkedin.dagli.list;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import com.linkedin.dagli.util.function.BooleanFunction1;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;


/**
 * Filters a list according to some specified criteria, which can be a predicate method, set of objects to include, or
 * set of objects to exclude.
 *
 * @param <T> the type of element in the list to filter
 */
@ValueEquality
public class FilteredList<T>
    extends AbstractPreparedTransformer1WithInput<Iterable<? extends T>, List<T>, FilteredList<T>> {
  private static final long serialVersionUID = 1;

  // the filter function that will be used (only list elements for which the filter returns true will be kept in the
  // transformer's result); if this is null, an exception will be thrown when the transformer is validated
  private BooleanFunction1.Serializable<? super T> _filter = null;

  /**
   * Sets the function that determines which elements will remain in the list.  Only elements for which the function
   * returns true will remain in the list.
   *
   * The function provided must be a "safely serializable" method reference or a function object.  Passing an anonymous
   * lambda function will result in an exception.  See {@link BooleanFunction1.Serializable#safelySerializable()} for
   * further details.
   *
   * @param function the method reference (e.g. obj::contains) or function object to use
   * @return a copy of this instance that will use this filter
   */
  public FilteredList<T> withInclusionFunction(BooleanFunction1.Serializable<? super T> function) {
    return clone(c -> c._filter = function.safelySerializable());
  }

  /**
   * Sets the function that determines which elements will be excluded from the list.  Only elements for which the
   * function returns false will remain in the list.
   *
   * The function provided must be a "safely serializable" method reference or a function object.  Passing an anonymous
   * lambda function will result in an exception.  See {@link BooleanFunction1.Serializable#safelySerializable()} for
   * further details.
   *
   * @param function the method reference (e.g. obj::contains) or function object to use
   * @return a copy of this instance that will use this filter
   */
  public FilteredList<T> withExclusionFunction(BooleanFunction1.Serializable<? super T> function) {
    return withInclusionFunction(function.safelySerializable().negate());
  }

  /**
   * Sets the {@link java.util.Set} that determines which elements will remain in the list.  Only elements in the set
   * will be kept in the filtered list.
   *
   * @param set the {@link java.util.Set} to use.  The FilteredList takes ownership of this Set, which must be
   *            serializable.
   * @return a copy of this instance that will use this filter
   */
  public FilteredList<T> withInclusionSet(Set<? super T> set) {
    return withInclusionFunction(set::contains);
  }

  /**
   * Sets the {@link java.util.Set} that determines which elements will remain in the list.  Only elements NOT in the
   * set will be kept in the filtered list.
   *
   * @param set the {@link java.util.Set} to use.  The FilteredList takes ownership of this Set, which must be
   *            serializable.
   * @return a copy of this instance that will use this filter
   */
  public FilteredList<T> withExclusionSet(Set<? super T> set) {
    return withExclusionFunction(set::contains);
  }

  @Override
  public void validate() {
    super.validate();
    if (_filter == null) {
      throw new IllegalStateException("The filter for the FilteredList has not been set");
    }
  }

  @Override
  public List<T> apply(Iterable<? extends T> value0) {
    ArrayList<T> result =
        value0 instanceof Collection ? new ArrayList<>(((Collection) value0).size()) : new ArrayList<>();

    for (T val : value0) {
      if (_filter.apply(val)) {
        result.add(val);
      }
    }

    return result;
  }
}
