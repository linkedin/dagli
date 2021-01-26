// AUTOGENERATED CODE.  DO NOT MODIFY DIRECTLY!  Instead, please modify the util/function/ComposedFunction.ftl file.
// See the README in the module's src/template directory for details.
package com.linkedin.dagli.util.function;

import java.util.Objects;
import com.linkedin.dagli.util.named.Named;


/**
 * A function class implementing {@link ShortFunction7.Serializable} that composes
 * {@link ShortFunction7} with a {@link Function1}.  The function is only <strong>actually</strong> serializable
 * if its constituent composed functions are serializable, of course.
 */
class ShortComposedFunction7<A, B, C, D, E, F, G, Q> implements ShortFunction7.Serializable<A, B, C, D, E, F, G>, Named {
  private static final long serialVersionUID = 1;

  private final Function7<A, B, C, D, E, F, G, Q> _first;
  private final ShortFunction1<? super Q> _andThen;

  /**
   * Creates a new instance that composes two functions, i.e. {@code andThen(first(inputs))}.
   *
   * @param first the first function to be called in the composition
   * @param andThen the second function to be called in the composition, accepting the {@code first} functions result
   *                as input
   */
  ShortComposedFunction7(Function7<A, B, C, D, E, F, G, Q> first, ShortFunction1<? super Q> andThen) {
    _first = first;
    _andThen = andThen;
  }

  @Override
  @SuppressWarnings("unchecked")
  public ShortComposedFunction7<A, B, C, D, E, F, G, Q> safelySerializable() {
    return new ShortComposedFunction7<>(((Function7.Serializable<A, B, C, D, E, F, G, Q>) _first).safelySerializable(),
        ((ShortFunction1.Serializable<? super Q>) _andThen).safelySerializable());
  }

  @Override
  public short apply(A value1, B value2, C value3, D value4, E value5, F value6, G value7) {
    return _andThen.apply(_first.apply(value1, value2, value3, value4, value5, value6, value7));
  }

  @Override
  public int hashCode() {
    return Objects.hash(ShortComposedFunction7.class, _first, _andThen);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ShortComposedFunction7) {
      return this._first.equals(((ShortComposedFunction7) obj)._first)
          && this._andThen.equals(((ShortComposedFunction7) obj)._andThen);
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