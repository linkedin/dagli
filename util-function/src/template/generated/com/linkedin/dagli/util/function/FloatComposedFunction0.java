// AUTOGENERATED CODE.  DO NOT MODIFY DIRECTLY!  Instead, please modify the util/function/ComposedFunction.ftl file.
// See the README in the module's src/template directory for details.
package com.linkedin.dagli.util.function;

import java.util.Objects;
import com.linkedin.dagli.util.named.Named;


/**
 * A function class implementing {@link FloatFunction0.Serializable} that composes
 * {@link FloatFunction0} with a {@link Function1}.  The function is only <strong>actually</strong> serializable
 * if its constituent composed functions are serializable, of course.
 */
class FloatComposedFunction0<Q> implements FloatFunction0.Serializable, Named {
  private static final long serialVersionUID = 1;

  private final Function0<Q> _first;
  private final FloatFunction1<? super Q> _andThen;

  /**
   * Creates a new instance that composes two functions, i.e. {@code andThen(first(inputs))}.
   *
   * @param first the first function to be called in the composition
   * @param andThen the second function to be called in the composition, accepting the {@code first} functions result
   *                as input
   */
  FloatComposedFunction0(Function0<Q> first, FloatFunction1<? super Q> andThen) {
    _first = first;
    _andThen = andThen;
  }

  @Override
  @SuppressWarnings("unchecked")
  public FloatComposedFunction0<Q> safelySerializable() {
    return new FloatComposedFunction0<>(((Function0.Serializable<Q>) _first).safelySerializable(),
        ((FloatFunction1.Serializable<? super Q>) _andThen).safelySerializable());
  }

  @Override
  public float apply() {
    return _andThen.apply(_first.apply());
  }

  @Override
  public int hashCode() {
    return Objects.hash(FloatComposedFunction0.class, _first, _andThen);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof FloatComposedFunction0) {
      return this._first.equals(((FloatComposedFunction0) obj)._first)
          && this._andThen.equals(((FloatComposedFunction0) obj)._andThen);
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
