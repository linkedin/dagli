package com.linkedin.dagli.preparer;

import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.transformer.PreparedTransformer;

/**
 * Base interface for preparers of dynamic-arity transformers.
 *
 * @param <R> the type of value produced by the transformer.
 * @param <N> the type of the resultant prepared transformer.
 */
public interface PreparerDynamic<R, N extends PreparedTransformer<? extends R>> extends Preparer<R, N> {
  @Override
  PreparerResultMixed<? extends PreparedTransformer<? extends R>, N> finishUnsafe(ObjectReader<Object[]> inputs);
}
