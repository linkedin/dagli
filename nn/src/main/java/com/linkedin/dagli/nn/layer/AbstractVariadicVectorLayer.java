package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.vector.DensifiedVector;


/**
 * Base class for NN transformations that operate on one or more input vectors, such as pooling operations.
 *
 * @param <R> the canonical type of result produced by this layer
 * @param <S> the type of the class ultimately deriving from this one
 */
abstract class AbstractVariadicVectorLayer<R, S extends AbstractVariadicVectorLayer<R, S>>
    extends AbstractVariadicTransformerLayer<DenseVector, R, S> {
  private static final long serialVersionUID = 1;

  /**
   * Returns a copy of this layer that will accept as an additional input a vector of values that are assumed to be
   * "dense-like".
   *
   * Specifically, <strong>vector elements with indices less than 0 or {@code >= maxWidth} will be silently ignored by
   * the neural network.</strong>
   *
   * If the input vectors have negative or very high indices, use
   * {@link #withAdditionalInputFromDensifiedVectors(Producer[])} instead, which will "densify" them and ensure that no
   * values are truncated.  This method is intended for vectors that are already dense-like (e.g. one/multi-hot vectors
   * for labels).
   *
   * @param input a {@link Producer} that will provide a vector input to this layer
   * @param maxWidth any vector elements with indices equal or greater to this value will be ignored
   * @return a copy of this layer that will accept the provided input
   */
  public S withAdditionalInputFromTruncatedVector(long maxWidth, Producer<? extends Vector> input) {
    return withAdditionalInputs(new NNVectorInputLayer().withMaxWidth(maxWidth).withInput(input));
  }

  /**
   * Returns a copy of this layer that will accept as an additional input the provided dense vector.
   *
   * @param input a {@link Producer} that will provide the vectors serving as input to this layer
   * @return a copy of this layer that will accept the provided input
   */
  public final S withAdditionalInputFromDenseVector(Producer<? extends DenseVector> input) {
    return withAdditionalInputFromTruncatedVector(Long.MAX_VALUE, input);
  }

  /**
   * Returns a copy of this layer that will accept as an additional input a "densification" of the provided vectors,
   * which is automatically accomplished via the {@link DensifiedVector} transformer.
   *
   * @param inputs a list of {@link Producer}s that will provide the vectors serving as input to this layer
   * @return a copy of this layer that will accept the provided inputs
   */
  @SafeVarargs
  public final S withAdditionalInputFromDensifiedVectors(Producer<? extends Vector>... inputs) {
    return withAdditionalInputFromDenseVector(new DensifiedVector().withInputs(inputs));
  }

  @SafeVarargs
  @Override
  public final S withAdditionalInputs(NNLayer<DenseVector, ? extends NonTerminalLayer>... layers) {
    return super.withAdditionalInputs(layers);
  }
}
