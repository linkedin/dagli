package com.linkedin.dagli.util.function;

import java.util.Objects;

/**
 * Implements the negation of the result of a boolean function.
 *
 * @param <A> the type of the (variadic) parameters for this function
 */
class BooleanNegatedFunctionVariadic<A> implements BooleanFunctionVariadic<A> {
  private static final int CLASS_HASH = BooleanNegatedFunctionVariadic.class.hashCode();

  private final BooleanFunctionVariadic<A> _function;

  // no-arg constructor for Kryo
  private BooleanNegatedFunctionVariadic() {
    _function = null;
  }

  /**
   * Creates a new instance that will negate the result provided by the given, wrapped function.
   *
   * @param function the function to be wrapped
   */
  private BooleanNegatedFunctionVariadic(BooleanFunctionVariadic<A> function) {
    _function = Objects.requireNonNull(function);
  }

  /**
   * Returns a function that will have a result that is a negation of the function provided.  If the passed function
   * is itself of this type, its underlying (wrapped) function will be returned; otherwise, a new instance of this class
   * will be created to wrap the passed function.
   *
   * @param function the function whose result will be negated
   * @param <A> the type of the variadic arguments
   * @return a function (which may or may not be new) that negates the return value of the provided function
   */
  static <A> BooleanFunctionVariadic<A> negate(BooleanFunctionVariadic<A> function) {
    if (function instanceof BooleanNegatedFunctionVariadic) {
      return ((BooleanNegatedFunctionVariadic<A>) function)._function; // negation of a negation is the original function
    }
    return new BooleanNegatedFunctionVariadic<>(function);
  }

  @Override
  @SafeVarargs
  public final boolean apply(A... args) {
    return !_function.apply(args);
  }

  @Override
  public int hashCode() {
    return CLASS_HASH + _function.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof BooleanNegatedFunctionVariadic) {
      return this._function.equals(((BooleanNegatedFunctionVariadic) obj)._function);
    }
    return false;
  }

  static class Serializable<A> extends BooleanNegatedFunctionVariadic<A>
      implements BooleanFunctionVariadic.Serializable<A> {
    private static final long serialVersionUID = 1;

    // no-arg constructor for Kryo
    private Serializable() {  }

    /**
     * Creates a new instance that will negate the result provided by the given, wrapped function.
     *
     * @param function the function to be wrapped
     */
    private Serializable(BooleanFunctionVariadic.Serializable<A> function) {
      super(function);
    }

    /**
     * Returns a function that will have a result that is a negation of the function provided.  If the passed function
     * is itself of this type, its underlying (wrapped) function will be returned; otherwise, a new instance of this class
     * will be created to wrap the passed function.
     *
     * @param function the function whose result will be negated
     * @param <A> the type of the variadic arguments
     * @return a function (which may or may not be new) that negates the return value of the provided function
     */
    static <A> BooleanFunctionVariadic.Serializable<A> negate(BooleanFunctionVariadic.Serializable<A> function) {
      if (function instanceof BooleanNegatedFunctionVariadic.Serializable) {
        // negation of a negation is the original function
        return (BooleanFunctionVariadic.Serializable<A>) ((BooleanNegatedFunctionVariadic<A>) function)._function;
      }
      return new BooleanNegatedFunctionVariadic.Serializable<>(function);
    }
  }
}
