// AUTOGENERATED CODE.  DO NOT MODIFY DIRECTLY!  Instead, please modify the util/function/ComposedFunction.ftl file.
// See the README in the module's src/template directory for details.
package com.linkedin.dagli.util.function;

import java.util.Objects;
import com.linkedin.dagli.util.named.Named;


/**
 * A function class implementing {@link CharacterFunction4.Serializable} that composes
 * {@link CharacterFunction4} with a {@link Function1}.  The function is only <strong>actually</strong> serializable
 * if its constituent composed functions are serializable, of course.
 */
class CharacterComposedFunction4<A, B, C, D, Q> implements CharacterFunction4.Serializable<A, B, C, D>, Named {
  private static final long serialVersionUID = 1;

  private final Function4<A, B, C, D, Q> _first;
  private final CharacterFunction1<? super Q> _andThen;

  /**
   * Creates a new instance that composes two functions, i.e. {@code andThen(first(inputs))}.
   *
   * @param first the first function to be called in the composition
   * @param andThen the second function to be called in the composition, accepting the {@code first} functions result
   *                as input
   */
  CharacterComposedFunction4(Function4<A, B, C, D, Q> first, CharacterFunction1<? super Q> andThen) {
    _first = first;
    _andThen = andThen;
  }

  @Override
  @SuppressWarnings("unchecked")
  public CharacterComposedFunction4<A, B, C, D, Q> safelySerializable() {
    return new CharacterComposedFunction4<>(((Function4.Serializable<A, B, C, D, Q>) _first).safelySerializable(),
        ((CharacterFunction1.Serializable<? super Q>) _andThen).safelySerializable());
  }

  @Override
  public char apply(A value1, B value2, C value3, D value4) {
    return _andThen.apply(_first.apply(value1, value2, value3, value4));
  }

  @Override
  public int hashCode() {
    return Objects.hash(CharacterComposedFunction4.class, _first, _andThen);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof CharacterComposedFunction4) {
      return this._first.equals(((CharacterComposedFunction4) obj)._first)
          && this._andThen.equals(((CharacterComposedFunction4) obj)._andThen);
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
