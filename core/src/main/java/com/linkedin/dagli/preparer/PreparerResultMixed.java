package com.linkedin.dagli.preparer;

import com.linkedin.dagli.transformer.PreparedTransformer;
import java.util.Objects;


/**
 * Preparers sometimes need to return distinct prepared transformers, one to transform the data used to prepare them,
 * and one to transform new data (this is frequently done to avoid overfitting).  For the common case where the
 * same type of transformer can be used for both types of data, use {@link PreparerResult} instead of this class.
 *
 * @param <P> the type of the transformer that should be used for preparation data
 * @param <Q> the type of the transformer that should be used for new data
 */
public class PreparerResultMixed<P extends PreparedTransformer<?>, Q extends PreparedTransformer<?>> {

  /**
   * Builds a {@link PreparerResultMixed} object
   *
   * @param <P> the type of the transformer that should be used for preparation data
   * @param <Q> the type of the transformer that should be used for new data
   */
  public static class Builder<P extends PreparedTransformer<?>, Q extends PreparedTransformer<?>> {
    protected final P _preparedTransformerForPreparationData;
    protected final Q _preparedTransformerForNewData;

    /**
     * Creates a new Builder with the preparation and new data transformers set to null.
     */
    public Builder() {
      this(null, null);
    }

    private Builder(P preparedTransformerForPreparationData, Q preparedTransformerForNewData) {
      _preparedTransformerForNewData = preparedTransformerForNewData;
      _preparedTransformerForPreparationData = preparedTransformerForPreparationData;
    }

    /**
     * Creates a copy of this PreparerResultMixed with the specified transformer to be used on preparation data.
     *
     * @param transformer the transformer to be used on preparation data
     * @return a copy of this PreparerResultMixed with the specified transformer to be used on preparation data
     */
    public <P extends PreparedTransformer<?>> Builder<P, Q> withTransformerForPreparationData(P transformer) {
      return new Builder<P, Q>(transformer, _preparedTransformerForNewData);
    }

    /**
     * Creates a copy of this PreparerResultMixed with the specified transformer to be used on new data.
     *
     * @param transformer the transformer to be used on new data
     * @return a copy of this PreparerResultMixed with the specified transformer to be used on new data
     */
    public <Q extends PreparedTransformer<?>> Builder<P, Q> withTransformerForNewData(Q transformer) {
      return new Builder<P, Q>(_preparedTransformerForPreparationData, transformer);
    }

    /**
     * Builds a PreparerResultMixed
     *
     * @return the new PreparerResultMixed containing the transformers set via this builder
     */
    public PreparerResultMixed<P, Q> build() {
      return new PreparerResultMixed<P, Q>(_preparedTransformerForPreparationData, _preparedTransformerForNewData);
    }
  }

  private final P _preparedTransformerForPreparationData;
  private final Q _preparedTransformerForNewData;

  protected PreparerResultMixed(P preparedTransformerForPreparationData, Q preparedTransformerForNewData) {
    _preparedTransformerForPreparationData = Objects.requireNonNull(preparedTransformerForPreparationData);
    _preparedTransformerForNewData = Objects.requireNonNull(preparedTransformerForNewData);
  }

  /**
   * Gets the prepared transformer that should be applied to the preparation data.
   *
   * @return the prepared transformer that should be applied to the preparation data
   */
  public P getPreparedTransformerForPreparationData() {
    return _preparedTransformerForPreparationData;
  }

  /**
   * Gets the prepared transformer that should be applied to new data.
   *
   * @return the prepared transformer that should be applied to new data
   */
  public Q getPreparedTransformerForNewData() {
    return _preparedTransformerForNewData;
  }

  /**
   * @return true iff the prepared transformer for new and preparation data are the same as determined by
   *         {@link Object#equals(Object)}
   */
  public boolean hasSamePreparedTransformerForNewAndPreparationData() {
    return Objects.equals(_preparedTransformerForNewData, _preparedTransformerForPreparationData);
  }

  /**
   * Casts a {@link PreparerResultMixed} instance to a "supertype".  This cast is safe due to the read-only semantics
   * of {@link PreparerResultMixed}.
   *
   * @param preparerResultMixed the instance to cast
   * @param <P> the type of the prepared transformer for prepared data
   * @param <Q> the type of the prepared transformer for new data
   * @return the passed {@code preparerResultMixed}, upcast to the desired "supertype"
   */
  @SuppressWarnings("unchecked")
  public static <P extends PreparedTransformer<?>, Q extends PreparedTransformer<?>> PreparerResultMixed<P, Q> cast(
      PreparerResultMixed<? extends P, ? extends Q> preparerResultMixed) {
    return (PreparerResultMixed<P, Q>) preparerResultMixed;
  }
}
