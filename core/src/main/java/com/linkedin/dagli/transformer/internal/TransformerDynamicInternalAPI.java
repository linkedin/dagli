package com.linkedin.dagli.transformer.internal;

import com.linkedin.dagli.transformer.TransformerDynamic;

/**
 * Base interface for the internal API of transformers with dynamic arity.
 *
 * @param <R> the type of value produced by the transformer
 * @param <S> the ultimate derived type of the transformer
 */
public interface TransformerDynamicInternalAPI<R, S extends TransformerDynamic<R>>
    extends TransformerInternalAPI<R, S>  { }
