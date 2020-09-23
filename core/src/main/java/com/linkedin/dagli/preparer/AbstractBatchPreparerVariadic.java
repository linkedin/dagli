package com.linkedin.dagli.preparer;

import com.linkedin.dagli.transformer.PreparedTransformerVariadic;


/**
 * Base class for batch preparers for variadic-arity transformers.
 *
 * @param <V> the type of input consumed by the transformer
 * @param <R> the type of value produced by the transformer
 * @param <N> the type of the resultant prepared transformer
 */
public abstract class AbstractBatchPreparerVariadic<V, R, N extends PreparedTransformerVariadic<V, R>>
    extends AbstractPreparerVariadic<V, R, N> {

  @Override
  public final PreparerMode getMode() {
    return PreparerMode.BATCH;
  }
}
