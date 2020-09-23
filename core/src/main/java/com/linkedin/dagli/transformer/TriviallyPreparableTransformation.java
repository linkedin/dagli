package com.linkedin.dagli.transformer;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.preparer.AbstractStreamPreparerDynamic;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerDynamic;
import com.linkedin.dagli.preparer.PreparerResultMixed;
import java.util.ArrayList;


/**
 * A trivially preparable transformer that simply prepares to an existing {@link PreparedTransformer}.
 *
 * This class is primarily useful in niche situations where a preparable transformer is expected but not really
 * necessary.
 *
 * @param <R> the type of result of the prepared transformer that will be generated
 * @param <N> the type of the prepared transformer that will be generated
 */
@ValueEquality
public class TriviallyPreparableTransformation<R, N extends PreparedTransformer<R>>
    extends AbstractPreparableTransformerDynamic<R, N, TriviallyPreparableTransformation<R, N>> {
  private static final long serialVersionUID = 1;

  private final N _preparedForNewData;
  private final PreparedTransformer<? extends R> _preparedForPreparationData;

  @Override
  protected boolean hasIdempotentPreparer() {
    return true; // all inputs to the preparable are ignored
  }

  /**
   * Creates a new instance that will prepare to the given prepared transformer.  The trivially preparable transformer
   * will "inherit" the parents of the provided {@code prepared} transformer.
   *
   * @param prepared the prepared transformer that will be "prepared" from this instance
   */
  public TriviallyPreparableTransformation(N prepared) {
    super(new ArrayList<>(prepared.internalAPI().getInputList()));
    _preparedForNewData = prepared;
    _preparedForPreparationData = prepared;
  }

  /**
   * Creates a new instance that will prepare to the given prepared transformer.  The trivially preparable transformer
   * will "inherit" the parents of the provided {@code preparedForPreparationData}.
   *
   * @param preparedForNewData the prepared transformer that will be "prepared" from this instance for "new" data
   * @param preparedForPreparationData the prepared transformer that will be "prepared" from this instance for
   *                                   "preparation" data
   */
  public TriviallyPreparableTransformation(PreparedTransformer<? extends R> preparedForPreparationData,
      N preparedForNewData) {
    super(new ArrayList<>(preparedForPreparationData.internalAPI().getInputList()));
    _preparedForNewData = preparedForNewData;
    _preparedForPreparationData = preparedForPreparationData;
  }

  /**
   * Creates a new instance that will prepare to the given prepared transformer.
   * @param preparerResult a {@link PreparerResultMixed} containing the prepared transformers from both "new" and
   *                       "preparation" data
   */
  public TriviallyPreparableTransformation(PreparerResultMixed<PreparedTransformer<? extends R>, N> preparerResult) {
    this(preparerResult.getPreparedTransformerForPreparationData(), preparerResult.getPreparedTransformerForNewData());
  }

  /**
   * @return the prepared transformer for "new" data that this instance pretends to prepare
   */
  public N getPreparedForNewData() {
    return _preparedForNewData;
  }

  /**
   * @return the prepared transformer for "preparation" data that this instance pretends to prepare
   */
  public PreparedTransformer<? extends R> getPreparedForPreparationData() {
    return _preparedForPreparationData;
  }

  @Override
  protected PreparerDynamic<R, N> getPreparer(PreparerContext context) {
    return new AbstractStreamPreparerDynamic<R, N>() {
      @Override
      public PreparerResultMixed<? extends PreparedTransformer<? extends R>, N> finish() {
        return new PreparerResultMixed.Builder<>().withTransformerForNewData(_preparedForNewData)
            .withTransformerForPreparationData(_preparedForPreparationData)
            .build();
      }

      @Override
      public void processUnsafe(Object[] values) { }
    };
  }
}
