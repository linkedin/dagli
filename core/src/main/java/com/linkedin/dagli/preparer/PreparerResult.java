package com.linkedin.dagli.preparer;

import com.linkedin.dagli.transformer.PreparedTransformer;
import java.util.function.Function;


/**
 * Derives from PreparerResultMixed and handles the very common case where the type of the transformer for both new
 * and preparation data are the same.
 *
 * @param <P> The type of the prepared transformer returned by this result.
 */
public class PreparerResult<P extends PreparedTransformer<?>> extends PreparerResultMixed<P, P> {
  public static class Builder<P extends PreparedTransformer<?>> {
    private final P _preparedTransformerForPreparationData;
    private final P _preparedTransformerForNewData;

    /**
     * Creates a new builder with the prepared transformers set to null.
     */
    public Builder() {
      this(null, null);
    }

    private Builder(P preparedTransformerForPreparationData, P preparedTransformerForNewData) {
      _preparedTransformerForNewData = preparedTransformerForNewData;
      _preparedTransformerForPreparationData = preparedTransformerForPreparationData;
    }

    /**
     * Creates a copy of this PreparerResult with the specified transformer to be used on preparation data.
     *
     * @param transformer the transformer to be used on preparation data
     * @return a copy of this PreparerResult with the specified transformer to be used on preparation data
     */
    public Builder<P> withTransformerForPreparationData(P transformer) {
      return new Builder<P>(transformer, _preparedTransformerForNewData);
    }

    /**
     * Creates a copy of this PreparerResult with the specified transformer to be used on new data.
     *
     * @param transformer the transformer to be used on new data
     * @return a copy of this PreparerResult with the specified transformer to be used on new data
     */
    public Builder<P> withTransformerForNewData(P transformer) {
      return new Builder<P>(_preparedTransformerForPreparationData, transformer);
    }

    /**
     * Builds a PreparerResult
     *
     * @return the new PreparerResult containing the transformers set via this builder
     */
    public PreparerResult<P> build() {
      return new PreparerResult<P>(_preparedTransformerForPreparationData, _preparedTransformerForNewData);
    }
  }

  // don't allow clients to call this directly--too easy to swap the argument order!
  private PreparerResult(P preparedTransformerForPreparationData, P preparedTransformerForNewData) {
    super(preparedTransformerForPreparationData, preparedTransformerForNewData);
  }

  /**
   * Creates a new PreparerResult where the same transformer should be used for both preparation and new data.
   *
   * @param preparedTransformerForAllData the prepared transformer to be contained
   */
  public PreparerResult(P preparedTransformerForAllData) {
    super(preparedTransformerForAllData, preparedTransformerForAllData);
  }

  /**
   * Maps the prepared transformers contained in this {@link PreparerResult} using a providing mapping function;
   * if the prepared transformers for new and preparation data are {@code equals(...)}, the mapping function is
   * called only once.
   *
   * @param mapper a mapping function that will map the prepared transformers to their new values
   * @param <N> the resulting type of the transformers
   * @return a new {@link PreparerResult} whose transformers are mapped from the transformers in this instance
   */
  public <N extends PreparedTransformer<?>> PreparerResult<N> map(Function<P, N> mapper) {
    N preparedForPrepData = mapper.apply(getPreparedTransformerForPreparationData());
    if (hasSamePreparedTransformerForNewAndPreparationData()) {
      return new PreparerResult<>(preparedForPrepData);
    } else {
      return new PreparerResult.Builder<N>()
          .withTransformerForPreparationData(preparedForPrepData)
          .withTransformerForNewData(mapper.apply(getPreparedTransformerForNewData()))
          .build();
    }
  }
}
