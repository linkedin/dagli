package com.linkedin.dagli.preparer;

import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.transformer.PreparedTransformerVariadic;
import java.util.List;


/**
 * Base class for stream preparers for variadic-arity transformers.
 *
 * @param <V> the type of input consumed by the transformer
 * @param <R> the type of value produced by the transformer
 * @param <N> the type of the resultant prepared transformer
 */
public abstract class AbstractStreamPreparerVariadic<V, R, N extends PreparedTransformerVariadic<V, R>>
    extends AbstractPreparerVariadic<V, R, N> {

  /**
   * Dagli-internal method.  Not intended for use by client code.
   *
   * Completes preparation of the resultant transformers and returns them.
   *
   * @return the prepared transformers
   */
  public abstract PreparerResultMixed<? extends PreparedTransformerVariadic<? super V, ? extends R>, N> finish();

  @Override
  public final PreparerMode getMode() {
    return PreparerMode.STREAM;
  }

  @Override
  public final PreparerResultMixed<? extends PreparedTransformerVariadic<? super V, ? extends R>, N> finish(ObjectReader<List<V>> inputs) {
    // the inputs parameter is ignored because this is a STREAM mode transformer; Preparers in STREAM mode (per the API)
    // always ignore the value passed to their finish(...) method.
    return finish();
  }
}
