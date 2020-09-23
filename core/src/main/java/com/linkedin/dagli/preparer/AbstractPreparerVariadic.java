package com.linkedin.dagli.preparer;

import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.transformer.PreparedTransformerVariadic;


/**
 * Base class for preparers for variadic-arity transformers.
 *
 * @param <V> the type of input to the transformer
 * @param <R> the type of value produced by the transformer
 * @param <N> the type of the resultant prepared transformer
 */
public abstract class AbstractPreparerVariadic<V, R, N extends PreparedTransformerVariadic<V, R>>
    extends AbstractPreparer<R, N> implements PreparerVariadic<V, R, N> {
  @Override
  public final void processUnsafe(Object[] values) {
    PreparerVariadic.super.processUnsafe(values);
  }

  @Override
  public final PreparerResultMixed<? extends PreparedTransformerVariadic<? super V, ? extends R>, N> finishUnsafe(
      ObjectReader<Object[]> inputs) {
    return PreparerVariadic.super.finishUnsafe(inputs);
  }
}
