// AUTOGENERATED CODE.  DO NOT MODIFY DIRECTLY!  Instead, please modify the util/function/ComposedFunction.ftl file.
// See the README in the module's src/template directory for details.
package com.linkedin.dagli.util.function;

import java.util.Objects;
import com.linkedin.dagli.util.named.Named;


/**
 * A function class implementing {@link Function5.Serializable} that composes
 * {@link Function5} with a {@link Function1}.  The function is only <strong>actually</strong> serializable
 * if its constituent composed functions are serializable, of course.
 */
class ComposedFunction5<A, B, C, D, E, R, Q> implements Function5.Serializable<A, B, C, D, E, R>, Named {
  private static final long serialVersionUID = 1;

  private final Function5<A, B, C, D, E, Q> _first;
  private final Function1<? super Q, ? extends R> _andThen;

  /**
   * Creates a new instance that composes two functions, i.e. {@code andThen(first(inputs))}.
   *
   * @param first the first function to be called in the composition
   * @param andThen the second function to be called in the composition, accepting the {@code first} functions result
   *                as input
   */
  ComposedFunction5(Function5<A, B, C, D, E, Q> first, Function1<? super Q, ? extends R> andThen) {
    _first = first;
    _andThen = andThen;
  }

  @Override
  @SuppressWarnings("unchecked")
  public ComposedFunction5<A, B, C, D, E, R, Q> safelySerializable() {
    return new ComposedFunction5<>(((Function5.Serializable<A, B, C, D, E, Q>) _first).safelySerializable(),
        ((Function1.Serializable<? super Q, ? extends R>) _andThen).safelySerializable());
  }

  @Override
  public R apply(A value1, B value2, C value3, D value4, E value5) {
    return _andThen.apply(_first.apply(value1, value2, value3, value4, value5));
  }

  @Override
  public int hashCode() {
    return Objects.hash(ComposedFunction5.class, _first, _andThen);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ComposedFunction5) {
      return this._first.equals(((ComposedFunction5) obj)._first)
          && this._andThen.equals(((ComposedFunction5) obj)._andThen);
    }
    return false;
  }

  @Override
  public String toString() {
    return Named.getShortName(_andThen) + "(" + Named.getShortName(_first) + ")";
  }

  @Override
  public String getShortName() {
    return Named.getShortName(_andThen) + "(...)";
  }
}