package com.linkedin.dagli.view.internal;

import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.producer.internal.ChildProducerInternalAPI;
import com.linkedin.dagli.transformer.PreparableTransformer;
import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.util.invariant.Arguments;
import com.linkedin.dagli.view.TransformerView;
import java.util.Collections;
import java.util.List;


/**
 * Defines the internal API used by {@link TransformerView}s.  Implementors of views should not normally implement this
 * interface themselves but should rather extend {@link com.linkedin.dagli.view.AbstractTransformerView}.
 *
 * @param <R> the type of value produced by the view
 * @param <N> the type of transformer being observed by the view
 * @param <S> the ultimate derived type of the view
 */
public interface TransformerViewInternalAPI<R, N extends PreparedTransformer<?>, S extends TransformerView<R, N>>
    extends ChildProducerInternalAPI<R, S> {
  @Override
  default List<? extends PreparableTransformer<?, ? extends N>> getInputList() {
    return Collections.singletonList(getViewed());
  }

  @Override
  @SuppressWarnings("unchecked") // the method is explicitly called "unsafe"
  default S withInputsUnsafe(List<? extends Producer<?>> newInputs) {
    Arguments.check(newInputs.size() == 1,
        "List of inputs provided to a TransformerView must contain exactly one PreparableTransformer");

    Arguments.check(newInputs.get(0) instanceof PreparableTransformer,
        "Input provided to TransformerView must be a PreparableTransformer");

    // the cast to (S) is required here because the compiler thinks the return type of withViewed(...) is not more
    // specific than the upper type bound of S (TransformerView), but this seems to be a compiler bug since it should
    // be able to (trivially) determine that its type is S.
    return (S) withViewed((PreparableTransformer) newInputs.get(0));
  }

  /**
   * Gets the preparable transformer being viewed.
   *
   * @return the viewed preparable transformer
   */
  PreparableTransformer<?, ? extends N> getViewed();

  /**
   * Creates a copy of the view that will view the specified transformer.
   *
   * The returned instance <strong>must</strong> be a new instance, as Dagli may rely on this invariant.
   *
   * @param transformer the transformer to be viewed
   * @return a copy of the view that will view the specified transformer.
   */
  S withViewed(PreparableTransformer<?, ? extends N> transformer);

  /**
   * Generates the result value that will be used when the DAG is applied to new data, after preparation is finished.
   *
   * @param preparedTransformerForNewData the prepared transformer created by the original, preparable transformer for
   *                                      new data
   * @return the return value that will be used when the DAG is applied to new data
   */
  R prepare(N preparedTransformerForNewData);

  /**
   * Generates the result value that will be used when the DAG is applied to preparation data.
   *
   * When Dagli prepares a DAG, preparable transformers can create different prepared transformers for use with
   * future, unseen data, and to transform the preparation (training) data; this can be done to, e.g. prevent
   * overfitting in certain scenarios.
   *
   * Because of this, Dagli also provides the ability to have a separately computed view of the transformer that was
   * prepared for preparation data, although implementations are very unlikely to need this in practice, since generally
   * either the prepared transformers for preparation and new data are the same, or the view result should only be based
   * on the transformer prepared to new data anyway.
   *
   * If the prepared transformers for new and preparation data are the same, this method need not be called and the
   * return of prepare(...) can be used instead.
   *
   * @param preparedForPreparationData the prepared transformer created by the original, preparable transformer for
   *                                   use on preparation data
   * @param preparedTransformerForNewData the prepared transformer created by the original, preparable transformer for
   *                                      use on new data
   * @return the return value that will be used when the DAG is applied to preparation data
   */
  R prepareForPreparationData(PreparedTransformer<?> preparedForPreparationData, N preparedTransformerForNewData);
}
