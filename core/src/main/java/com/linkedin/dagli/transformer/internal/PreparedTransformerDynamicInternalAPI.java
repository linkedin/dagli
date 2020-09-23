package com.linkedin.dagli.transformer.internal;

import com.linkedin.dagli.transformer.PreparedTransformerDynamic;

/**
 * Base interface for the internal API of prepared transformers with dynamic arity.
 *
 * @param <R> the type of value produced by the transformer
 * @param <S> the ultimate derived type of the transformer
 */
public interface PreparedTransformerDynamicInternalAPI<R, S extends PreparedTransformerDynamic<R>>
  extends TransformerDynamicInternalAPI<R, S>, PreparedTransformerInternalAPI<R, S> {

}
