package com.linkedin.dagli.preparer;

import com.linkedin.dagli.transformer.PreparedTransformer;


/**
 * Trivial "preparer" that simply returns a provided, prepared transformer.
 * @param <R> the type of result produced by the prepared transformer.
 * @param <N> the type of prepared transformer that will be returned
 */
public class TrivialPreparerDynamic<R, N extends PreparedTransformer<R>> extends AbstractStreamPreparerDynamic<R, N> {
  private final N _preparedForNewData;
  private final PreparedTransformer<? extends R> _preparedForPreparationData;

  /**
   * Creates a new instance that will "prepare" the provided transformer.
   *
   * @param prepared the transformer which will result from this preparer.
   */
  public TrivialPreparerDynamic(N prepared) {
    _preparedForNewData = prepared;
    _preparedForPreparationData = prepared;
  }

  /**
   * Creates a new instance that will "prepare" the provided transformers.
   *
   * @param preparedForPreparationData the transformer to be used for preparation data
   * @param preparedForNewData the transformer to be used for new data
   */
  public TrivialPreparerDynamic(PreparedTransformer<? extends R> preparedForPreparationData, N preparedForNewData) {
    _preparedForNewData = preparedForNewData;
    _preparedForPreparationData = preparedForPreparationData;
  }

  @Override
  public void processUnsafe(Object[] values) { }

  @Override
  public PreparerResultMixed<? extends PreparedTransformer<? extends R>, N>  finish() {
    return new PreparerResultMixed<>(_preparedForPreparationData, _preparedForNewData);
  }
}
