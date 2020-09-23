package com.linkedin.dagli.transformer;

import com.linkedin.dagli.transformer.internal.PreparableTransformerDynamicInternalAPI;


/**
 * Interface for a {@link PreparableTransformer} with with arbitrary, dynamically-determined arguments.
 *
 * @param <R> the type of the result
 */
public interface PreparableTransformerDynamic<R, N extends PreparedTransformer<R>>
    extends TransformerDynamic<R>, PreparableTransformer<R, N> {

  @Override
  PreparableTransformerDynamicInternalAPI<R, N, ? extends PreparableTransformerDynamic<R, N>> internalAPI();

  /**
   * Wraps a preparable transformer accepting as single Object[] input as a dynamic transformer where each element of
   * the Object[] is now a separate input.
   *
   * @param preparable the transformer to wrap
   * @param arity the arity of the dynamic transformer; this must match the length of the expected Object[] inputs to
   *              the wrapped transformer
   * @param <R> the return type of the transformer
   * @return a preparable dynamic-arity transformer that accepts the specified number of inputs
   */
  static <R> PreparableTransformerDynamic<R, ? extends PreparedTransformerDynamic<R>> from(
      PreparableTransformer1<Object[], R, ? extends PreparedTransformer1<Object[], R>> preparable, int arity) {
    return new ArrayTransformerAsDynamic<R>(arity, preparable);
  }
}
