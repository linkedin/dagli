package com.linkedin.dagli.tester;

import com.linkedin.dagli.generator.Generator;
import com.linkedin.dagli.transformer.PreparableTransformer;
import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.view.TransformerView;


/**
 * Static convenience methods for testing Dagli nodes.
 */
public abstract class Tester {
  private Tester() { }

  /**
   * Convenience method that creates a test builder instance for the provided Dagli node.
   *
   * @param generator the node to test
   * @return a test builder that will test the provided node
   */
  public static <R, T extends Generator<R>> GeneratorTestBuilder<R, T> of(T generator) {
    return new GeneratorTestBuilder<>(generator);
  }

  /**
   * Convenience method that creates a test builder instance for the provided Dagli node.
   *
   * @param view the node to test
   * @return a test builder that will test the provided node
   */
  public static <R, N extends PreparedTransformer<R>, T extends TransformerView<R, N>>
  TransformerViewTestBuilder<R, N, T> of(T view) {
    return new TransformerViewTestBuilder<>(view);
  }

  /**
   * Convenience method that creates a test builder instance for the provided Dagli node.
   *
   * @param transformer the node to test
   * @return a test builder that will test the provided node
   */
  public static <R, T extends PreparedTransformer<R>> PreparedTransformerTestBuilder<R, T> of(T transformer) {
    return new PreparedTransformerTestBuilder<>(transformer);
  }

  /**
   * Convenience method that creates a test builder instance for the provided Dagli node.
   *
   * @param transformer the node to test
   * @return a test builder that will test the provided node
   */
  public static <R, N extends PreparedTransformer<R>, T extends PreparableTransformer<R, N>>
  PreparableTransformerTestBuilder<R, N, T> of(T transformer) {
    return new PreparableTransformerTestBuilder<>(transformer);
  }
}
