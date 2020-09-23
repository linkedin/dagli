package com.linkedin.dagli.preparer;

import com.linkedin.dagli.transformer.PreparedTransformer;


/**
 * Base class for batch preparers for dynamic-arity transformers.
 *
 * @param <R> the type of value produced by the transformer
 * @param <N> the type of the resultant prepared transformer
 */
public abstract class AbstractBatchPreparerDynamic<R, N extends PreparedTransformer<R>>
    extends AbstractPreparerDynamic<R, N> implements PreparerDynamic<R, N> {
  @Override
  public final PreparerMode getMode() {
    return PreparerMode.BATCH;
  }
}
