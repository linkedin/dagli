package com.linkedin.dagli.util.function;

import java.util.function.UnaryOperator;
import com.linkedin.dagli.util.exception.Exceptions;

@FunctionalInterface
public interface UnaryFunction<T> extends Function1<T, T>, UnaryOperator<T> {
  @Override
  T apply(T valA);

  /**
   * Returns a unary identity function.
   *
   * @param <T> the type of the argument and result of the function
   * @return a unary identity function
   */
  static <T> UnaryFunction.Serializable<T> identity() {
    return (UnaryFunction.Serializable<T>) IdentityFunctionInstance.INSTANCE;
  }

  /**
   * Gets an unchecked copy of this function.  Checked exceptions thrown while applying the unchecked copy will be
   * caught, wrapped as runtime exceptions, and rethrown.
   *
   * @param checkedFunction the function whose checked exceptions should be treated as runtime exceptions
   * @param <T> the type of argument and result of the checked function
   * @return a function that calls the checked function and converts any thrown checked exceptions to runtime exceptions
   */
  static <T> UnaryFunction<T> unchecked(Checked<T, ?> checkedFunction) {
    return val -> {
      try {
        return checkedFunction.apply(val);
      } catch (Throwable e) {
        throw Exceptions.asRuntimeException(e);
      }
    };
  }

  /**
   * A UnaryFunction that throws a checked exception.
   *
   * @param <T> the type of argument and result of the unary function
   * @param <E> the type of exception; strictly speaking, E could be a runtime exception, but in that case there would
   *            be no reason to use this interface!
   */
  interface Checked<T, E extends Throwable> extends Function1.Checked<T, T, E> {
    @Override
    T apply(T valA) throws E;
  }

  /**
   * A {@link java.io.Serializable} unary function.
   * @param <T> the type of argument (and result) of the function.
   */
  interface Serializable<T> extends UnaryFunction<T>, Function1.Serializable<T, T>, java.io.Serializable { }
}