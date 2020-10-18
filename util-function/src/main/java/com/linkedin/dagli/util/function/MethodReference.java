package com.linkedin.dagli.util.function;

import com.linkedin.dagli.util.invariant.Arguments;
import com.linkedin.dagli.util.named.Named;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * MethodReference provides a safe, serializable way to capture method references.
 *
 * Method references take the form of "Class::staticMethod", "Class::instanceMethod", or "instance::instanceMethod".
 * In the last case the instance itself must be serialized along with other method information.  Note that "Class"
 * may also be an interface.
 *
 * Pass the method reference to MethodReference's constructor to instantiate a new instance of MethodReference.
 * getMethodHandle() returns a method handle to the method, which can then be invoke()'d.
 *
 * MethodReference attempts to detect when a lambda is erroneously passed in place of a method reference and throws an
 * exception in such cases, however, this detection is in principle JDK-dependent and thus not absolutely guaranteed to
 * work.  Do not rely on it as a foolproof method reference detector.
 *
 * You should never serialize a method reference directly, as they, like all lambdas, are extremely tricky to
 * serialize correctly when the serializing and deserializing programs are different.
 */
class MethodReference implements Serializable, Named {
  private static final long serialVersionUID = 1;

  private static final String LAMBDA_PREFIX = "lambda$";

  private final String _class;
  private final String _methodName;
  private final String _methodSignature;
  private final int _methodKind;
  private final Object _instance;

  /**
   * Creates a method like this one except that it has no bound instance.  This increases the arity of the method by 1,
   * as the former bound instance must now be passed as the first argument.
   *
   * @return an unbound method reference
   */
  MethodReference unbind() {
    return new MethodReference(_class, _methodName, _methodSignature, _methodKind);
  }

  // Creates an unbound reference
  private MethodReference(String cls, String methodName, String methodSignature, int methodKind) {
    _class = cls;
    _methodName = methodName;
    _methodSignature = methodSignature;
    _methodKind = methodKind;
    _instance = null;
  }

  /**
   * Default initializer for Kryo
   */
  private MethodReference() {
    this(null, null, null, 0); // Kryo will supply these values
  }

  MethodReference(Serializable function) {
    try {
      final Method method = function.getClass().getDeclaredMethod("writeReplace");
      method.setAccessible(true);
      SerializedLambda sl = (SerializedLambda) method.invoke(function);
      _class = sl.getImplClass().replace('/', '.');
      _methodName = sl.getImplMethodName();
      _methodKind =
          Arguments.inIntSet(sl.getImplMethodKind(), () -> "Only static, virtual, and constructor methods are allowed",
              MethodHandleInfo.REF_invokeStatic, MethodHandleInfo.REF_invokeVirtual,
              MethodHandleInfo.REF_newInvokeSpecial, MethodHandleInfo.REF_invokeInterface);
      Arguments.check(_methodKind != MethodHandleInfo.REF_invokeSpecial,
          "MethodReference cannot (yet) represent a 'special' function, such as a non-static inner-class "
              + "constructor.  This restriction may be lifted in the future; for now we recommend defining a "
              + "serializable function object to perform your desired purpose and using that instead.");
      Arguments.check(!_methodName.startsWith(LAMBDA_PREFIX),
          "MethodReference cannot represent a lambda function because these are backed by arbitrary, "
          + "anonymous implementations and thus inherently unsafe to serialize.  Use method references (e.g. "
          + "String::length) instead.");
      Arguments.check(sl.getCapturedArgCount() <= 1,
        "The provided function has unexpected captured arguments and is thus presumably a lambda function.  "
          + "MethodReference cannot represent a lambda function because these are backed by arbitrary, "
            + "anonymous implementations and thus inherently unsafe to serialize.  Use method references (e.g. "
            + "String::length) instead.");

      if (sl.getCapturedArgCount() == 1) {
        _instance = sl.getCapturedArg(0);
      } else {
        _instance = null;
      }

      _methodSignature = sl.getImplMethodSignature();
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e); // well, that's not good
    }
  }

  MethodHandles.Lookup getLookup() {
    try {
      Class<?> cls = Class.forName(_class);

      if (cls.getPackage().getName().startsWith("java.")) {
        cls = MethodReference.class; // if we used a java.* class, metafactory would use the bootstrap class loader,
        // which can't see non-java.* classes (also as of Java 9 the JRE would print out a stern warning regarding
        // "illegal" reflective access)
      }
      // as of Java 9 privateLookupIn(...) provides an alternative to reflecting private fields within core Java classes
      // to get a Lookup object that can see otherwise-inaccessible methods (which is inherently brittle)
      return MethodHandles.privateLookupIn(cls, MethodHandles.lookup());
    } catch (ClassNotFoundException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  Object getBoundInstance() {
    return _instance;
  }

  boolean isBound() {
    return _instance != null;
  }

  MethodHandle getMethodHandle() {
    MethodHandles.Lookup lookup = getLookup();

    try {
      Class<?> cls = Class.forName(_class);
      MethodType type = MethodType.fromMethodDescriptorString(_methodSignature, cls.getClassLoader());

      switch (_methodKind) {
        case MethodHandleInfo.REF_invokeStatic:
          return lookup.findStatic(cls, _methodName, type);
        case MethodHandleInfo.REF_invokeVirtual:
        case MethodHandleInfo.REF_invokeInterface:
          if (_instance != null) {
            return lookup.bind(_instance, _methodName, type);
          } else {
            return lookup.findVirtual(cls, _methodName, type);
          }
        case MethodHandleInfo.REF_newInvokeSpecial:
          return lookup.findConstructor(cls, type);
        default:
          throw new UnsupportedOperationException("Unknown method type");
      }
    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(_class, _methodName, _methodSignature, _methodKind, _instance);
  }

  @Override
  public boolean equals(Object obj) {
    // In principle we might try to allow for equality with arbitrary functions, trying to create a MethodReference for
    // them and then, if successful, testing that for equality.  However, even assuming this was useful, it would result
    // in hashCode() being inconsistent with this method.
    if (!(obj instanceof MethodReference)) {
      return false;
    }

    MethodReference other = (MethodReference) obj;

    return Objects.equals(this._class, other._class)
        && Objects.equals(this._methodName, other._methodName)
        && Objects.equals(this._methodSignature, other._methodSignature)
        && this._methodKind == other._methodKind
        && Objects.equals(this._instance, other._instance);
  }

  @Override
  public String toString() {
    return _class + "::" + _methodName + _methodSignature + " [Object instance: " + _instance
        + ", Method type: " + MethodHandleInfo.referenceKindToString(_methodKind) + "]";
  }

  @Override
  public String getName() {
    try {
      Class<?> cls = Class.forName(_class);
      MethodType type = MethodType.fromMethodDescriptorString(_methodSignature, cls.getClassLoader());
      return cls.getSimpleName() + "::" + _methodName + "(" + type.parameterList()
          .stream()
          .map(Class::getSimpleName)
          .collect(Collectors.joining(", ")) + ")";
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getShortName() {
    try {
      Class<?> cls = Class.forName(_class);
      MethodType type = MethodType.fromMethodDescriptorString(_methodSignature, cls.getClassLoader());
      return cls.getSimpleName() + "::" + _methodName + "(" + (type.parameterCount() > 0 ? "..." : "") + ")";
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
