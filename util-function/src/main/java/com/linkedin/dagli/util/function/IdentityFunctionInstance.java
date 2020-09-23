package com.linkedin.dagli.util.function;

/**
 * Simple identity function singleton, provided because Function.identity() is not serializable.
 */
enum IdentityFunctionInstance implements UnaryFunction.Serializable<Object> {
  INSTANCE;

  @Override
  public Object apply(Object t) {
    return t;
  }
}
