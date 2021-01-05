// AUTOGENERATED CODE.  DO NOT MODIFY DIRECTLY!  Instead, please modify the util/function/ComposedFunction.ftl file.
// See the README in the module's src/template directory for details.
package com.linkedin.dagli.util.function;

import java.util.Objects;
import com.linkedin.dagli.util.named.Named;


/**
 * A function class implementing {@link FloatFunction1.Serializable} that composes
 * {@link FloatFunction1} with a {@link Function1}.  The function is only <strong>actually</strong> serializable
 * if its constituent composed functions are serializable, of course.
 */
class FloatComposedFunction1<A, Q> implements FloatFunction1.Serializable<A>, Named {
  private static final long serialVersionUID = 1;

  private final Function1<A, Q> _first;
  private final FloatFunction1<? super Q> _andThen;

  /**
   * Creates a new instance that composes two functions, i.e. {@code andThen(first(inputs))}.
   *
   * @param first the first function to be called in the composition
   * @param andThen the second function to be called in the composition, accepting the {@code first} functions result
   *                as input
   */
  FloatComposedFunction1(Function1<A, Q> first, FloatFunction1<? super Q> andThen) {
    _first = first;
    _andThen = andThen;
  }

  @Override
  @SuppressWarnings("unchecked")
  public FloatComposedFunction1<A, Q> safelySerializable() {
    return new FloatComposedFunction1<>(((Function1.Serializable<A, Q>) _first).safelySerializable(),
        ((FloatFunction1.Serializable<? super Q>) _andThen).safelySerializable());
  }

  @Override
  public float apply(A value1) {
    return _andThen.apply(_first.apply(value1));
  }

  @Override
  public int hashCode() {
    return Objects.hash(FloatComposedFunction1.class, _first, _andThen);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof FloatComposedFunction1) {
      return this._first.equals(((FloatComposedFunction1) obj)._first)
          && this._andThen.equals(((FloatComposedFunction1) obj)._andThen);
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
