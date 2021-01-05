// AUTOGENERATED CODE.  DO NOT MODIFY DIRECTLY!  Instead, please modify the util/function/DefaultOnNullArgument.ftl file.
// See the README in the module's src/template directory for details.
package com.linkedin.dagli.util.function;

import com.linkedin.dagli.util.named.Named;
import java.util.Objects;


/**
 * A function class implementing VoidFunction9.Serializable<A, B, C, D, E, F, G, H, I> that returns null if any of
 * the function's inputs are null.
 */
class VoidDefaultOnNullArgument9<A, B, C, D, E, F, G, H, I> implements
    VoidFunction9.Serializable<A, B, C, D, E, F, G, H, I>, Named {
  private static final long serialVersionUID = 1;
  private static final int CLASS_HASH = VoidDefaultOnNullArgument9.class.hashCode();
  private final VoidFunction9<A, B, C, D, E, F, G, H, I> _wrapped;

  VoidDefaultOnNullArgument9(VoidFunction9<A, B, C, D, E, F, G, H, I> wrapped) {
    // stacking this wrapper multiple times should be idempotent:
    if (wrapped instanceof VoidDefaultOnNullArgument9) {
      _wrapped = ((VoidDefaultOnNullArgument9) wrapped)._wrapped;
    } else {
      _wrapped = Objects.requireNonNull(wrapped);
    }
  }

  @Override
  public VoidDefaultOnNullArgument9<A, B, C, D, E, F, G, H, I> safelySerializable() {
    return new VoidDefaultOnNullArgument9<>(
        ((VoidFunction9.Serializable<A, B, C, D, E, F, G, H, I>) _wrapped).safelySerializable());
  }

  @Override
  public void apply(A value1, B value2, C value3, D value4, E value5, F value6, G value7, H value8, I value9) {
    if (value1 == null || value2 == null || value3 == null || value4 == null || value5 == null || value6 == null
        || value7 == null || value8 == null || value9 == null) {
      return;
    }
    _wrapped.apply(value1, value2, value3, value4, value5, value6, value7, value8, value9);
  }

  @Override
  public int hashCode() {
    return CLASS_HASH + _wrapped.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof VoidDefaultOnNullArgument9) {
      return this._wrapped.equals(((VoidDefaultOnNullArgument9) obj)._wrapped);
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
