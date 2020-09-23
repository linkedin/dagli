package com.linkedin.dagli.preparer;

import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.transformer.PreparedTransformer;

/**
 * Base class for stream preparers for dynamic-arity transformers.
 *
 * @param <R> the type of value produced by the transformer
 * @param <N> the type of the resultant prepared transformer
 */
public abstract class AbstractStreamPreparerDynamic<R, N extends PreparedTransformer<R>>
    extends AbstractPreparerDynamic<R, N> {

  @Override
  public final PreparerMode getMode() {
    return PreparerMode.STREAM;
  }

  /**
   * Dagli-internal method.  Not intended for use by client code.
   *
   * Completes preparation of the resultant transformers and returns them.
   *
   * @return the prepared transformers
   */
  public abstract PreparerResultMixed<? extends PreparedTransformer<? extends R>, N> finish();

  @Override
  public final PreparerResultMixed<? extends PreparedTransformer<? extends R>, N> finishUnsafe(
      ObjectReader<Object[]> inputs) {
    // the inputs parameter is ignored because this is a STREAM mode transformer; Preparers in STREAM mode (per the API)
    // always ignore the value passed to their finish(...) method.
    return finish();
  }
}
