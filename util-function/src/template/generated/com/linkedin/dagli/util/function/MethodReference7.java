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
class MethodReference7<A, B, C, D, E, F, G, R> implements Function7.Serializable<A, B, C, D, E, F, G, R>, Named {
  private static final long serialVersionUID = 1;

  // hash and equality distinguish between different types of MethodReferenceX classes even when the underlying method
  // references are identical
  private static final int CLASS_HASH = MethodReference7.class.hashCode();

  private final MethodReference _methodReference;
  private transient Function7<A, B, C, D, E, F, G, R> _cachedFunction = null;

  MethodReference7(MethodReference mr) {
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
  public MethodReference7(Serializable<A, B, C, D, E, F, G, R> func) {
    if (func instanceof MethodReference7) {
      // multiple applications of this constructor are idempotent:
      _methodReference = ((MethodReference7) func)._methodReference;
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

  private Function7<A, B, C, D, E, F, G, R> fromMethodReference() {
    if (_methodReference.isBound()) {
      MethodReference8<Object, A, B, C, D, E, F, G, R> unbound = new MethodReference8<>(_methodReference.unbind());
      return (A value1, B value2, C value3, D value4, E value5, F value6, G value7) -> unbound.apply(
          _methodReference.getBoundInstance(), value1, value2, value3, value4, value5, value6, value7);
    }

    MethodHandle mh = _methodReference.getMethodHandle();

    try {
      MethodType type = mh.type().generic();
      return (Function7<A, B, C, D, E, F, G, R>) LambdaMetafactory
          .metafactory(_methodReference.getLookup(), "apply", MethodType.methodType(Function7.class), type, mh,
              mh.type().wrap()).getTarget().invokeExact();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  public R apply(A value1, B value2, C value3, D value4, E value5, F value6, G value7) {
    return _cachedFunction.apply(value1, value2, value3, value4, value5, value6, value7);
  }

  @Override
  public int hashCode() {
    return CLASS_HASH + _methodReference.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof MethodReference7)) {
      return false;
    }

    return _methodReference.equals(((MethodReference7) obj)._methodReference);
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
