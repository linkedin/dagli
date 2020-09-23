package com.linkedin.dagli.view;

import com.linkedin.dagli.annotation.equality.IgnoredByValueEquality;
import com.linkedin.dagli.producer.AbstractChildProducer;
import com.linkedin.dagli.transformer.PreparableTransformer;
import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.view.internal.TransformerViewInternalAPI;


/**
 * Base class for transformer views (see {@link TransformerView} for details).  Implementations of transformer views are
 * encouraged to extend this class rather than implementing the {@link TransformerView} interface directly.
 *
 * @param <R> the type of value produced by the view
 * @param <N> the type of (prepared) transformer observed by the view
 * @param <S> the ulimate derived type of the view extending this class.
 */
@IgnoredByValueEquality
public abstract class AbstractTransformerView<R,
                                              N extends PreparedTransformer<?>,
                                              S extends AbstractTransformerView<R, N, S>>
  extends AbstractChildProducer<R, TransformerViewInternalAPI<R, N, S>, S>
  implements TransformerView<R, N> {

  private static final long serialVersionUID = 1;

  /**
   * The transformer being observed by the view.
   */
  protected PreparableTransformer<?, ? extends N> _viewedTransformer;

  /**
   * Creates a new view of the specified transformer
   * @param viewedTransformer the transformer that, when prepared, will be used to calculate the view
   */
  public AbstractTransformerView(PreparableTransformer<?, ? extends N> viewedTransformer) {
    super();
    _viewedTransformer = viewedTransformer;
  }

  protected PreparableTransformer<?, ? extends N> getViewedTransformer() {
    return _viewedTransformer;
  }

  /**
   * Returns a copy of this view that will be applied to the transformer resulting from the preparation of the given
   * {@link PreparedTransformer}.
   *
   * The returned instance <strong>must</strong> be a new instance, as Dagli may rely on this invariant.
   *
   * @param viewedTransformer the transformer to view
   * @return a copy of this instance that will view the specified transformer
   */
  protected S withViewedTransformer(PreparableTransformer<?, ? extends N> viewedTransformer) {
    return clone(c -> c._viewedTransformer = viewedTransformer);
  }

  /**
   * During DAG preparation (training), the observed preparable transformer will be prepared, generating two
   * transformers: one for "new data" that will be used for inference on future inputs, and one for "preparation data"
   * that will be used to transform the same inputs that were used to prepare (train) the transformer.
   *
   * The view sees both of these generated transformers, and produces a value that will be used when the DAG is run on
   * new, future inputs and a value used when transforming the preparation inputs, respectively.
   *
   * This method observes the prepared transformer that is to be used for new (future) examples and returns the value
   * that the view should produce for such examples.
   *
   * @param preparedTransformerForNewData the prepared transformer being viewed
   * @return the value this view should produce as its result
   */
  protected abstract R prepare(N preparedTransformerForNewData);

  /**
   * During DAG preparation (training), the observed preparable transformer will be prepared, generating two
   * transformers: one for "new data" that will be used for inference on future inputs, and one for "preparation data"
   * that will be used to transform the same inputs that were used to prepare (train) the transformer.
   *
   * The view sees both of these generated transformers, and produces a value that will be used when the DAG is run on
   * new, future inputs and a value used when transforming the preparation inputs, respectively.
   *
   * This method observes both the prepared transformer that is to be used for preparation data and new (future) data
   * and returns the value that the view should produce for <b>preparation</b> data.  The default implementation is
   * to use the same value as returned by prepare() on the latter transformer and ignore the transformer
   * generated for use on preparation data entirely.
   *
   * @param preparedForPreparationData the prepared transformer, intended to transform the inputs used to prepare it,
   *                                   being viewed
   * @param preparedTransformerForNewData the prepared transformer, intended to transform new, future inputs, being
   *                                      viewed
   * @return the value this view should produce as its result when processing the examples used during preparation
   */
  protected R prepareForPreparationData(PreparedTransformer<?> preparedForPreparationData,
      N preparedTransformerForNewData) {
    return prepare(preparedTransformerForNewData);
  }

  @Override
  protected TransformerViewInternalAPI<R, N, S> createInternalAPI() {
    return new InternalAPI();
  }

  protected class InternalAPI extends AbstractChildProducer<R, TransformerViewInternalAPI<R, N, S>, S>.InternalAPI
    implements TransformerViewInternalAPI<R, N, S> {
    @Override
    public PreparableTransformer<?, ? extends N> getViewed() {
      return getViewedTransformer();
    }

    @Override
    public S withViewed(PreparableTransformer<?, ? extends N> transformer) {
      return withViewedTransformer(transformer);
    }

    @Override
    public R prepare(N preparedTransformerForNewData) {
      return AbstractTransformerView.this.prepare(preparedTransformerForNewData);
    }

    @Override
    public R prepareForPreparationData(PreparedTransformer<?> preparedForPreparationData, N preparedTransformerForNewData) {
      return AbstractTransformerView.this.prepareForPreparationData(preparedForPreparationData,
          preparedTransformerForNewData);
    }

    @Override
    public boolean hasAlwaysConstantResult() {
      return true; // TransformerViews always return a constant result in any given DAG execution
    }
  }
}
