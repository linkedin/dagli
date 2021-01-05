// AUTOGENERATED CODE.  DO NOT MODIFY DIRECTLY!  Instead, please modify the util/function/DefaultOnNullArgument.ftl file.
// See the README in the module's src/template directory for details.
package com.linkedin.dagli.util.function;

import com.linkedin.dagli.util.named.Named;
import java.util.Objects;


/**
 * A function class implementing DoubleFunction1.Serializable<A> that returns 0 if any of
 * the function's inputs are null.
 */
class DoubleDefaultOnNullArgument1<A> implements DoubleFunction1.Serializable<A>, Named {
  private static final long serialVersionUID = 1;
  private static final int CLASS_HASH = DoubleDefaultOnNullArgument1.class.hashCode();
  private final DoubleFunction1<A> _wrapped;

  DoubleDefaultOnNullArgument1(DoubleFunction1<A> wrapped) {
    // stacking this wrapper multiple times should be idempotent:
    if (wrapped instanceof DoubleDefaultOnNullArgument1) {
      _wrapped = ((DoubleDefaultOnNullArgument1) wrapped)._wrapped;
    } else {
      _wrapped = Objects.requireNonNull(wrapped);
    }
  }

  @Override
  public DoubleDefaultOnNullArgument1<A> safelySerializable() {
    return new DoubleDefaultOnNullArgument1<>(((DoubleFunction1.Serializable<A>) _wrapped).safelySerializable());
  }

  @Override
  public double apply(A value1) {
    if (value1 == null) {
      return 0;
    }
    return _wrapped.apply(value1);
  }

  @Override
  public int hashCode() {
    return CLASS_HASH + _wrapped.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof DoubleDefaultOnNullArgument1) {
      return this._wrapped.equals(((DoubleDefaultOnNullArgument1) obj)._wrapped);
    }
    return false;
  }

  @Override
  public String toString() {
    return "arg == null ? 0 : " + Named.getName(_wrapped);
  }

  @Override
  public String getShortName() {
    return "arg == null ? 0 : " + Named.getShortName(_wrapped);
  }
}
