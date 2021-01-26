// AUTOGENERATED CODE.  DO NOT MODIFY DIRECTLY!  Instead, please modify the util/function/DefaultOnNullArgument.ftl file.
// See the README in the module's src/template directory for details.
package com.linkedin.dagli.util.function;

import com.linkedin.dagli.util.named.Named;
import java.util.Objects;


/**
 * A function class implementing FloatFunction2.Serializable<A, B> that returns 0 if any of
 * the function's inputs are null.
 */
class FloatDefaultOnNullArgument2<A, B> implements FloatFunction2.Serializable<A, B>, Named {
  private static final long serialVersionUID = 1;
  private static final int CLASS_HASH = FloatDefaultOnNullArgument2.class.hashCode();
  private final FloatFunction2<A, B> _wrapped;

  FloatDefaultOnNullArgument2(FloatFunction2<A, B> wrapped) {
    // stacking this wrapper multiple times should be idempotent:
    if (wrapped instanceof FloatDefaultOnNullArgument2) {
      _wrapped = ((FloatDefaultOnNullArgument2) wrapped)._wrapped;
    } else {
      _wrapped = Objects.requireNonNull(wrapped);
    }
  }

  @Override
  public FloatDefaultOnNullArgument2<A, B> safelySerializable() {
    return new FloatDefaultOnNullArgument2<>(((FloatFunction2.Serializable<A, B>) _wrapped).safelySerializable());
  }

  @Override
  public float apply(A value1, B value2) {
    if (value1 == null || value2 == null) {
      return 0;
    }
    return _wrapped.apply(value1, value2);
  }

  @Override
  public int hashCode() {
    return CLASS_HASH + _wrapped.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof FloatDefaultOnNullArgument2) {
      return this._wrapped.equals(((FloatDefaultOnNullArgument2) obj)._wrapped);
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