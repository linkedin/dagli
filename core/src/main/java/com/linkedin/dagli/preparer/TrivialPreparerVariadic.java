package com.linkedin.dagli.preparer;

import com.linkedin.dagli.transformer.PreparedTransformerVariadic;
import java.util.List;

/**
 * Trivial "preparer" that simply returns a provided, prepared transformer.
 * @param <V> the nominal type of the values provided as an input to the preparer (these will be ignored)
 * @param <R> the type of result produced by the prepared transformer.
 * @param <N> the type of prepared transformer that will be returned
 */
public class TrivialPreparerVariadic<V, R, N extends PreparedTransformerVariadic<V, R>>
    extends AbstractStreamPreparerVariadic<V, R, N> {
  private final N _preparedForNewData;
  private final PreparedTransformerVariadic<? super V, ? extends R> _preparedForPreparationData;

  /**
   * Creates a new instance that will "prepare" the provided transformer.
   *
   * @param prepared the transformer which will result from this preparer.
   */
  public TrivialPreparerVariadic(N prepared) {
    _preparedForNewData = prepared;
    _preparedForPreparationData = prepared;
  }

  /**
   * Creates a new instance that will "prepare" the provided transformers.
   *
   * @param preparedForPreparationData the transformer to be used for preparation data
   * @param preparedForNewData the transformer to be used for new data
   */
  public TrivialPreparerVariadic(PreparedTransformerVariadic<? super V, ? extends R> preparedForPreparationData,
      N preparedForNewData) {
    _preparedForNewData = preparedForNewData;
    _preparedForPreparationData = preparedForPreparationData;
  }

  @Override
  public void process(List<V> values) { }

  @Override
  public PreparerResultMixed<? extends PreparedTransformerVariadic<? super V, ? extends R>, N>  finish() {
    return new PreparerResultMixed<>(_preparedForPreparationData, _preparedForNewData);
  }
}
