package com.linkedin.dagli.view;

import com.linkedin.dagli.producer.ChildProducer;
import com.linkedin.dagli.producer.ProducerType;
import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.view.internal.TransformerViewInternalAPI;


/**
 * {@link TransformerView}s can be prepared to create a result that is derived from the prepared transformer created by
 * preparing a {@link com.linkedin.dagli.transformer.PreparableTransformer}.  This result is made available in the
 * preared DAG as a {@link com.linkedin.dagli.generator.Constant}.
 *
 * A common use for {@link TransformerView}s is to "pull out" the trained parameters from a statistical model and
 * provide them as inputs to other transformers in the DAG.  For example, after training a linear model, a
 * {@link TransformerView} can extract the K highest (absolute) weight features, which can then be used for feature
 * selection for the features passed to a subsequent model.
 *
 * @param <R> the type of value that will be provided when this View is prepared
 * @param <N> the type of the prepared transformer (for new data) that will be prepared from T
 */
public interface TransformerView<R, N extends PreparedTransformer<?>>
    extends ChildProducer<R>, ProducerType<R, TransformerView<R, N>> {
  @Override
  TransformerViewInternalAPI<R, N, ? extends TransformerView<R, N>> internalAPI();

  /**
   * Casts a producer to an effective "supertype" interface.  The semantics of the producer guarantee that the returned
   * type is valid for the instance.
   *
   * Note that although this and other {@code cast(...)} methods are safe, this safety extends only to the
   * interfaces for which they are implemented.  The covariance and contravariance relationships existing for these interfaces
   * do not necessarily hold for their derived classes.  For example, a {@code PreparedTransformer<String>} is also a
   * {@code PreparedTransformer<Object>}, but a {@code MyTransformer<String>} cannot necessarily be safely treated as a
   * {@code MyTransformer<Object>}.
   *
   * @param view the view to cast
   * @param <R> the type of result of the returned view
   * @param <N> the type of the prepared transformer observed by the returned view
   * @return the passed view, typed to a new "supertype" of the original
   */
  @SuppressWarnings("unchecked")
  static <R, N extends PreparedTransformer<?>> TransformerView<R, N> cast(
      TransformerView<? extends R, ? super N> view) {
    return (TransformerView<R, N>) view;
  }
}
