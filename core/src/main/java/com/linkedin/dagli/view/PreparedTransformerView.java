package com.linkedin.dagli.view;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.transformer.PreparableTransformer;
import com.linkedin.dagli.transformer.PreparedTransformer;


/**
 * Provides a simple view of a preparable transformer that is just the prepared transformer itself.
 *
 * For example, an instance created as
 * {@code new PreparedTransformerView<>(someXGBoostClassificationTransformer)}
 *
 * where <code>someXGBoostClassificationTransformer</code> is an XGBoostClassification will produce a value (that can
 * be consumed by its downstream children in the DAG) of the trained XGBoostClassification.Prepared XGBoost model.
 *
 * This can be convenient when you need most or all of the information contained in the prepared transformer as an input
 * value to another transformer that is a child of this view, rather than the normal practice of creating a custom view
 * that passes only the specific information that the child requires.
 *
 * We do not recommend using this class if you only need a small portion of the information contained within the viewed
 * transformer as it may be less efficient than creating a custom view class.
 *
 * @param <P> the type of the prepared transformer that will be prepared from the viewed preparable transformer, and
 *            will also be the result of this view
 */
@ValueEquality
public class PreparedTransformerView<P extends PreparedTransformer<?>> extends AbstractTransformerView<P, P, PreparedTransformerView<P>> {
  private static final long serialVersionUID = 1;

  /**
   * Creates a new view of the specified transformer that, once this transformer is prepared, will "produce" the
   * prepared transformer so that it may be consumed as an input value by its child nodes.
   * @param viewedTransformer the transformer whose prepared transformer will be viewed by this instance
   */
  public PreparedTransformerView(PreparableTransformer<?, ? extends P> viewedTransformer) {
    super(viewedTransformer);
  }

  @Override
  protected P prepare(P preparedTransformerForNewData) {
    return preparedTransformerForNewData;
  }
}
