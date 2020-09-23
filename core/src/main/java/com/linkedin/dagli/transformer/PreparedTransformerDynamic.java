package com.linkedin.dagli.transformer;

import com.linkedin.dagli.transformer.internal.PreparedTransformerDynamicInternalAPI;


/**
 * Interface for a {@link PreparedTransformer} with arbitrary, dynamically-determined arguments.
 *
 * @param <R> the type of the result
 */
public interface PreparedTransformerDynamic<R> extends TransformerDynamic<R>, PreparedTransformer<R> {
  @Override
  PreparedTransformerDynamicInternalAPI<R, ? extends PreparedTransformerDynamic<R>> internalAPI();

  /**
   * Wraps a prepared transformer accepting as single Object[] input as a dynamic transformer where each element of
   * the Object[] is now a separate input.
   *
   * @param prepared the transformer to wrap
   * @param arity the arity of the dynamic transformer; this must match the length of the expected Object[] inputs to
   *              the wrapped transformer
   * @param <R> the return type of the transformer
   * @return a prepared dynamic-arity transformer that accepts the specified number of inputs
   */
  static <R> PreparedTransformerDynamic<R> from(PreparedTransformer1<Object[], ? extends R> prepared, int arity) {
    return new ArrayTransformerAsDynamic.Prepared<R>(arity, prepared);
  }
}
