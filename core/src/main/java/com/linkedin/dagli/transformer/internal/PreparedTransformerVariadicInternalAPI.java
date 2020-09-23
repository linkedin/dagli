package com.linkedin.dagli.transformer.internal;

import com.linkedin.dagli.transformer.PreparedTransformerVariadic;

/**
 * Base interface for the internal API of prepared transformers with variadic arity.
 *
 * @param <V> the type of input accepted by the transformer
 * @param <R> the type of value produced by the transformer
 * @param <S> the ultimate derived type of the transformer
 */
public interface PreparedTransformerVariadicInternalAPI<V, R, S extends PreparedTransformerVariadic<V, R>>
  extends TransformerVariadicInternalAPI<V, R, S>, PreparedTransformerInternalAPI<R, S> {

}
