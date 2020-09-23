package com.linkedin.dagli.function;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.transformer.AbstractPreparedTransformerVariadic;
import com.linkedin.dagli.util.function.Function1;
import java.util.List;


/**
 * FunctionResult transformers apply a (serializable) function object, method reference, or lambda against their inputs.
 *
 * Versioning of the underlying function is not enforced (i.e. FunctionResult doesn't know if the implementation of a
 * method changes).
 *
 * If you're using serializable function objects or method references (e.g. String::length, str::startsWith, etc.), you
 * can stop reading here.
 *
 * For lambdas (anonymous functions, i.e. (args) -> { body }), there are strict limitations with respect to
 * serializability because they are implemented as anonymous classes.  Make sure you understand these, as otherwise your
 * DAG may be writable, but not readable, or an innocuous change may break deserialization after initially working fine!
 *
 * Because of this, you must use withFunctionUnsafe(...) to use a lambda function.  This is by design, to warn you that
 * your DAG will probably not be safely serializable as a result.
 *
 * Again, <strong>warning</strong>!  Lambda functions, which are implemented with anonymous classes, are inherently
 * unsafe to serialize!  Lambdas are fine if you don't care about serializing your DAG, but we highly discourage them in
 * any serialized setup, <strong>especially</strong> if it's production.  Three conditions must be met for lambdas to
 * serialize and deserialize successfully:
 * (1) the class in which they were created must exist in both serializing and deserializing programs.
 * (2) the ORDER in which the lambdas are defined must not change.  The names of the generated anonymous classes are
 * dependent upon the position in which the lambda appears in the file!
 * (3) the JVM should be consistent, as different JVMs are in principle free to generate different class names.
 */
@ValueEquality
public class FunctionResultVariadic<V, R> extends AbstractPreparedTransformerVariadic<V, R, FunctionResultVariadic<V, R>> {
  private static final long serialVersionUID = 1;

  private Function1.Serializable<List<? extends V>, R> _function = null;

  /**
   * Creates a new instance that processes inputs with the specified function.
   *
   * @param func a serializable function object or a method reference.  FunctionResult will attempt to detect lambda
   *        functions and throw an exception in such cases, but as this is in principle JDK dependent these checks
   *        cannot absolutely be guaranteed to catch such abuses.  If you MUST use a lambda, call
   *        withFunctionUnsafe(...).
   */
  public FunctionResultVariadic(Function1.Serializable<List<? extends V>, R> func) {
    this();
    _function = func.safelySerializable();
  }

  /**
   * Creates a new instance.
   */
  public FunctionResultVariadic() {
    super();
  }

  /**
   * Returns a copy of this instance that will process inputs with the specified function.
   *
   * @param func a serializable function object or a method reference.  FunctionResult will attempt to detect lambda
   *        functions and throw an exception in such cases, but as this is in principle JDK dependent these checks
   *        cannot absolutely be guaranteed to catch such abuses.  If you need to use a lambda, call
   *        withFunctionUnsafe(...).
   */
  public FunctionResultVariadic<V, R> withFunction(Function1.Serializable<List<? extends V>, R> func) {
    return clone(c -> c._function = func.safelySerializable());
  }

  /**
   * Returns a copy of this instance that will process inputs with the specified function.  This method is UNSAFE
   * because it permits arbitrary lambda functions, which are very difficult to safely serialize.  We strongly caution
   * against using this method if you need FunctionResult (and thus your DAG as a whole) to be reliably deserializable.
   *
   * @param func an arbitrary lambda function
   */
  public FunctionResultVariadic<V, R> withFunctionUnsafe(Function1.Serializable<List<? extends V>, R> func) {
    return clone(c -> {
      try {
        c._function = func.safelySerializable();
      } catch (RuntimeException e) {
        c._function = func;
      }
    });
  }

  @Override
  public R apply(List<? extends V> values) {
    return _function.apply(values);
  }
}
