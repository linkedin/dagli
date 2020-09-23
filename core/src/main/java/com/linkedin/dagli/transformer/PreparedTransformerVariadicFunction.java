package com.linkedin.dagli.transformer;

import com.linkedin.dagli.util.function.FunctionVariadic;
import java.util.Arrays;


/**
 * Wraps a variadic (prepared) transformer as a {@link FunctionVariadic}.
 *
 * @param <V> the type of input consumed by the transformer
 * @param <R> the type of result produced by the transformer
 */
final class PreparedTransformerVariadicFunction<V, R> implements FunctionVariadic.Serializable<V, R> {
  private final static int serialVersionUID = 1;

  private final PreparedTransformerVariadic<V, R> _transformer;

  PreparedTransformerVariadicFunction(PreparedTransformerVariadic<V, R> transformer) {
    _transformer = transformer;
  }

  @Override
  @SafeVarargs
  public final R apply(V... args) {
    return _transformer.apply(Arrays.asList(args));
  }
}
