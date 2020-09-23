package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.vector.DensifiedVector;


/**
 * Abstract class for layers accepting "dense" inputs (i.e. a vector of numbers).  Adds convenience methods allowing
 * them to nominally accept a variety of input types by automatically creating the needed ancestor nodes.  This allows
 * clients to avoid having to instantiate the input layers explicitly themselves.
 *
 * @param <S> the type of the ultimate derived class descending from this base class
 */
abstract class AbstractBinaryVectorLayer<R, S extends AbstractBinaryVectorLayer<R, S>> extends
    AbstractBinaryTransformerLayer<DenseVector, DenseVector, R, S> {
  private static final long serialVersionUID = 1;

  /**
   * Returns a copy of this layer that will accept as its input a vector of values that are assumed to be "dense-like".
   *
   * Specifically, <strong>vector elements with indices less than 0 or {@code >= maxWidth} will be silently ignored by
   * the neural network.</strong>
   *
   * If the input vectors have negative or very high indices, use {@link #withFirstInputFromDensifiedVector(Producer)}
   * instead, which will "densify" them and ensure that no values are truncated.  This method is intended for vectors
   * that are already dense-like (e.g. one/multi-hot vectors for labels).
   *
   * @param input a {@link Producer} that will provide a vector input to this layer
   * @param maxWidth any vector elements with indices equal or greater to this value will be ignored
   * @return a copy of this layer that will accept the provided input
   */
  public S withFirstInputFromTruncatedVector(long maxWidth, Producer<? extends Vector> input) {
    return withFirstInput(new NNVectorInputLayer().withMaxWidth(maxWidth).withInput(input));
  }

  /**
   * Returns a copy of this layer that will accept as its input a vector of values that are assumed to be "dense-like".
   *
   * Specifically, <strong>vector elements with indices less than 0 or {@code >= maxWidth} will be silently ignored by
   * the neural network.</strong>
   *
   * If the input vectors have negative or very high indices, use {@link #withSecondInputFromDensifiedVector(Producer)}
   * instead, which will "densify" them and ensure that no values are truncated.  This method is intended for vectors
   * that are already dense-like (e.g. one/multi-hot vectors for labels).
   *
   * @param input a {@link Producer} that will provide a vector input to this layer
   * @param maxWidth any vector elements with indices equal or greater to this value will be ignored
   * @return a copy of this layer that will accept the provided input
   */
  public S withSecondInputFromTruncatedVector(long maxWidth, Producer<? extends Vector> input) {
    return withSecondInput(new NNVectorInputLayer().withMaxWidth(maxWidth).withInput(input));
  }

  /**
   * Returns a copy of this layer that will accept as its input a dense vector of values.
   *
   * @param input a {@link Producer} that will provide a vector input to this layer
   * @return a copy of this layer that will accept the provided input
   */
  public S withFirstInputFromDenseVector(Producer<? extends DenseVector> input) {
    return withFirstInputFromTruncatedVector(Long.MAX_VALUE, input);
  }

  /**
   * Returns a copy of this layer that will accept as its input a dense vector of values.
   *
   * @param input a {@link Producer} that will provide a vector input to this layer
   * @return a copy of this layer that will accept the provided input
   */
  public S withSecondInputFromDenseVector(Producer<? extends DenseVector> input) {
    return withSecondInputFromTruncatedVector(Long.MAX_VALUE, input);
  }

  /**
   * Returns a copy of this layer that will accept as its first input a "densification" of the provided vectors, which
   * is automatically accomplished via the {@link DensifiedVector} transformer.
   *
   * @param inputs a list of {@link Producer}s that will provide the vectors serving as input to this layer
   * @return a copy of this layer that will accept the provided inputs
   */
  @SafeVarargs
  public final S withFirstInputFromDensifiedVectors(Producer<? extends Vector>... inputs) {
    return withFirstInputFromDenseVector(new DensifiedVector().withInputs(inputs));
  }

  /**
   * Returns a copy of this layer that will accept as its second input a "densification" of the provided vectors, which
   * is automatically accomplished via the {@link DensifiedVector} transformer.
   *
   * @param inputs a list of {@link Producer}s that will provide the vectors serving as input to this layer
   * @return a copy of this layer that will accept the provided inputs
   */
  @SafeVarargs
  public final S withSecondInputFromDensifiedVectors(Producer<? extends Vector>... inputs) {
    return withFirstInputFromDenseVector(new DensifiedVector().withInputs(inputs));
  }

  @Override
  public S withFirstInput(NNLayer<DenseVector, ? extends NonTerminalLayer> inputLayer) {
    return super.withFirstInput(inputLayer);
  }

  @Override
  public S withSecondInput(NNLayer<DenseVector, ? extends NonTerminalLayer> inputLayer) {
    return super.withSecondInput(inputLayer);
  }
}
