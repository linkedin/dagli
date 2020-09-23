package com.linkedin.dagli.tester;

import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.view.TransformerView;
import java.util.Collection;


/**
 * Tests a {@link TransformerView}.
 *
 * @param <R> the type of result produced by the {@link TransformerView}
 * @param <N> the type of {@link PreparedTransformer} viewed by this {@link TransformerView} (its "input")
 */
public final class TransformerViewTestBuilder<R, N extends PreparedTransformer<?>, T extends TransformerView<R, N>>
    extends AbstractChildTestBuilder<N, R, T, TransformerViewTestBuilder<R, N, T>> {


  /**
   * Creates a new instance that will test the provided Dagli node.
   *
   * @param testSubject the primary test subject
   */
  public TransformerViewTestBuilder(T testSubject) {
    super(testSubject);
  }

  /**
   * Adds an input (a prepared transformer to be viewed by the tested {@link TransformerView}).
   *
   * @param preparedTransformer the prepared transformer to be viewed
   * @return this instance
   */
  public TransformerViewTestBuilder<R, N, T> input(N preparedTransformer) {
    return addInput(preparedTransformer);
  }

  /**
   * Adds multiple inputs (prepared transformers to be viewed by the tested {@link TransformerView}).  Equivalent to
   * calling {@link #input(PreparedTransformer)} for each element in <code>inputs</code>.
   *
   * @param inputs the collection of inputs to add
   * @return
   */
  public TransformerViewTestBuilder<R, N, T> allInputs(Collection<? extends N> inputs) {
    return addAllInputs(inputs);
  }

  @Override
  public void test() {
    super.test();
    checkInputsAndOutputsForAll((subject, viewed) -> subject.internalAPI().prepare(viewed));
    checkInputsAndOutputsFor(withPlaceholderInputs(_testSubject),
        (subject, viewed) -> subject.internalAPI().prepare(viewed));
  }
}
