// AUTOGENERATED CODE.  DO NOT MODIFY DIRECTLY!  Instead, please modify the util/function/MethodReference.ftl file.
// See the README in the module's src/template directory for details.
package com.linkedin.dagli.util.function;

import com.linkedin.dagli.util.named.Named;
import java.io.IOException;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;


/**
 * Represents a safely serializable method reference (assuming the JVM supports {@link MethodReference}, which is not
 * guaranteed).  If the JVM does not support {@link MethodReference}, an exception may be thrown when creating the
 * method reference; fortunately, however, deserialization will not be affected: if you can serialize it, you'll be able
 * to deserialize later on any JVM.
 */
class ShortMethodReference7<A, B, C, D, E, F, G> implements ShortFunction7.Serializable<A, B, C, D, E, F, G>, Named {
  private static final long serialVersionUID = 1;

  // hash and equality distinguish between different types of MethodReferenceX classes even when the underlying method
  // references are identical
  private static final int CLASS_HASH = ShortMethodReference7.class.hashCode();

  private final MethodReference _methodReference;
  private transient ShortFunction7<A, B, C, D, E, F, G> _cachedFunction = null;

  ShortMethodReference7(MethodReference mr) {
    _methodReference = mr;
    initCachedFunction();
  }

  /**
   * Creates a new instance.
   *
   * The passed func parameter must be a method reference, such as Object::toString or String::length.  A runtime
   * exception will be thrown if func is a function object or an anonymous lambda (e.g. "a -> a + 5").  An exception
   * may also be thrown if your JVM implementation does not support the library's internal method for retrieving
   * information about the passed method reference.
   *
   * @param func a method reference lambda to wrap.  If this function is not safely serializable, a runtime exception
   *        will be thrown.
   */
  public ShortMethodReference7(Serializable<A, B, C, D, E, F, G> func) {
    if (func instanceof ShortMethodReference7) {
      // multiple applications of this constructor are idempotent:
      _methodReference = ((ShortMethodReference7) func)._methodReference;
    } else {
      // this line will thrown an exception if func is not a method reference:
      _methodReference = new MethodReference(func);
    }
    initCachedFunction();
  }

  // deserialization hook to ensure _cachedFunction is always set
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    initCachedFunction();
  }

  private void initCachedFunction() {
    if (_cachedFunction == null) {
      _cachedFunction = fromMethodReference();
    }
  }

  private ShortFunction7<A, B, C, D, E, F, G> fromMethodReference() {
    if (_methodReference.isBound()) {
      ShortMethodReference8<Object, A, B, C, D, E, F, G> unbound =
          new ShortMethodReference8<>(_methodReference.unbind());
      return (A value1, B value2, C value3, D value4, E value5, F value6, G value7) -> unbound.apply(
          _methodReference.getBoundInstance(), value1, value2, value3, value4, value5, value6, value7);
    }

    MethodHandle mh = _methodReference.getMethodHandle();

    try {
      MethodType type = mh.type().generic().changeReturnType(short.class);
      return (ShortFunction7<A, B, C, D, E, F, G>) LambdaMetafactory
          .metafactory(_methodReference.getLookup(), "apply", MethodType.methodType(ShortFunction7.class), type, mh,
              mh.type().wrap().changeReturnType(short.class)).getTarget().invokeExact();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  public short apply(A value1, B value2, C value3, D value4, E value5, F value6, G value7) {
    return _cachedFunction.apply(value1, value2, value3, value4, value5, value6, value7);
  }

  @Override
  public int hashCode() {
    return CLASS_HASH + _methodReference.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ShortMethodReference7)) {
      return false;
    }

    return _methodReference.equals(((ShortMethodReference7) obj)._methodReference);
  }

  @Override
  public String toString() {
    return _methodReference.toString();
  }

  @Override
  public String getName() {
    return _methodReference.getName();
  }

  @Override
  public String getShortName() {
    return _methodReference.getShortName();
  }
}