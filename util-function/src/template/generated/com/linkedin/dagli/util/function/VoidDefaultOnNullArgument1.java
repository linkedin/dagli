// AUTOGENERATED CODE.  DO NOT MODIFY DIRECTLY!  Instead, please modify the util/function/DefaultOnNullArgument.ftl file.
// See the README in the module's src/template directory for details.
package com.linkedin.dagli.util.function;

import com.linkedin.dagli.util.named.Named;
import java.util.Objects;


/**
 * A function class implementing VoidFunction1.Serializable<A> that returns null if any of
 * the function's inputs are null.
 */
class VoidDefaultOnNullArgument1<A> implements VoidFunction1.Serializable<A>, Named {
  private static final long serialVersionUID = 1;
  private static final int CLASS_HASH = VoidDefaultOnNullArgument1.class.hashCode();
  private final VoidFunction1<A> _wrapped;

  VoidDefaultOnNullArgument1(VoidFunction1<A> wrapped) {
    // stacking this wrapper multiple times should be idempotent:
    if (wrapped instanceof VoidDefaultOnNullArgument1) {
      _wrapped = ((VoidDefaultOnNullArgument1) wrapped)._wrapped;
    } else {
      _wrapped = Objects.requireNonNull(wrapped);
    }
  }

  @Override
  public VoidDefaultOnNullArgument1<A> safelySerializable() {
    return new VoidDefaultOnNullArgument1<>(((VoidFunction1.Serializable<A>) _wrapped).safelySerializable());
  }

  @Override
  public void apply(A value1) {
    if (value1 == null) {
      return;
    }
    _wrapped.apply(value1);
  }

  @Override
  public int hashCode() {
    return CLASS_HASH + _wrapped.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof VoidDefaultOnNullArgument1) {
      return this._wrapped.equals(((VoidDefaultOnNullArgument1) obj)._wrapped);
    }
    return false;
  }

  @Override
  public String toString() {
    return "arg == null ? null : " + Named.getName(_wrapped);
  }

  @Override
  public String getShortName() {
    return "arg == null ? null : " + Named.getShortName(_wrapped);
  }
}