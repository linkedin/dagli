package com.linkedin.dagli.transformer.internal;

import com.linkedin.dagli.transformer.TransformerVariadic;

/**
 * Base interface for the internal API of transformers with variadic arity.
 *
 * @param <V> the type of input accepted by the transformer
 * @param <R> the type of value produced by the transformer
 * @param <S> the ultimate derived type of the transformer
 */
public interface TransformerVariadicInternalAPI<V, R, S extends TransformerVariadic<V, R>>
  extends TransformerInternalAPI<R, S> {

}
