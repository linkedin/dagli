// AUTOGENERATED CODE.  DO NOT MODIFY DIRECTLY!  Instead, please modify the util/function/Function.ftl file.
// See the README in the module's src/template directory for details.
package com.linkedin.dagli.util.function;

import com.linkedin.dagli.util.exception.Exceptions;


@FunctionalInterface
public interface CharacterFunction0 extends FunctionBase {
  char apply();

  static CharacterFunction0 unchecked(Checked<?> checkedFunction) {
    return () -> {
      try {
        return checkedFunction.apply();
      } catch (Throwable e) {
        throw Exceptions.asRuntimeException(e);
      }
    };
  }

  @FunctionalInterface
  interface Checked<X extends Throwable> extends FunctionBase {
    char apply() throws X;
  }

  /**
   * Creates a new, safely-serializable function from a provided (serializable) function if it is method reference
   * (e.g. Object::toString),  or simply returns the passed function if it is a function object.  If this is something
   * not safely serializable (e.g. a lambda), an exception will be thrown.
   *
   * "Safely-serializable" means that a function can be deserialized in a way that is not inherently brittle.
   * We recommend only serializing functions when they are safely-serializable, but note that this is not a guarantee;
   * as with Serializable objects in general it's always possible to create something (safely-)serializable that will
   * not serialize, e.g. an instance method with a captured instance (e.g. new Object()::toString) where the captured
   * instance is not itself serializable.
   *
   * Function objects that wrap functions and implement CharacterFunction0.Serializable should override this method
   * when appropriate.  Generally such an implementation will simply create a new instance wrapping
   * wrappedFunction.safelySerializable() instead of wrappedFunction.
   *
   * Anonymous lambdas, such as "{@code a -> a + 5}", are *not* safely-serializable, even if they are technically
   * serializable, as they are extraordinarily fragile and will only deserialize correctly under these conditions:
   * (1) the class in which they were created must exist in both serializing and deserializing programs.
   * (2) the ORDER in which the lambdas are defined must not change.  The names of the generated anonymous classes are
   * dependent upon the position in which the lambda appears in the file!
   * (3) the JVM should be consistent, as different JVMs are in principle free to generate different class names.
   */
  static CharacterFunction0.Serializable safelySerializable(Serializable function) {
    return function.safelySerializable();
  }

  interface Serializable extends CharacterFunction0, java.io.Serializable {
    /**
     * Creates a new, safely-serializable function from this one if this is a method reference (e.g. Object::toString),
     * or simply returns this if this is a function object.  If this is something not safely serializable (e.g. a
     * lambda), an exception will be thrown.
     *
     * "Safely-serializable" means that a function can be deserialized in a way that is not inherently brittle.
     * We recommend only serializing functions when they are safely-serializable, but note that this is not a guarantee;
     * as with Serializable objects in general it's always possible to create something (safely-)serializable that will
     * not serialize, e.g. an instance method with a captured instance (e.g. new Object()::toString) where the captured
     * instance is not itself serializable.
     *
     * Function objects that wrap functions and implement CharacterFunction0.Serializable should override this method
     * when appropriate.  Generally such an implementation will simply create a new instance wrapping
     * wrappedFunction.safelySerializable() instead of wrappedFunction.
     *
     * Anonymous lambdas, such as "{@code a -> a + 5}", are *not* safely-serializable, even if they are technically
     * serializable, as they are extraordinarily fragile and will only deserialize correctly under these conditions:
     * (1) the class in which they were created must exist in both serializing and deserializing programs.
     * (2) the ORDER in which the lambdas are defined must not change.  The names of the generated anonymous classes are
     * dependent upon the position in which the lambda appears in the file!
     * (3) the JVM should be consistent, as different JVMs are in principle free to generate different class names.
     */
    default Serializable safelySerializable() {
      try {
        return new CharacterMethodReference0(this);
      } catch (java.lang.RuntimeException e) {
        if (e.getCause() instanceof java.lang.NoSuchMethodException) {
          // must be a function object
          return this;
        } else {
          // anonymous lambda or something went wrong
          throw e;
        }
      }
    }

  }
}