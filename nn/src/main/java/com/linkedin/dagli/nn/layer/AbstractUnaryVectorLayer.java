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
abstract class AbstractUnaryVectorLayer<R, S extends AbstractUnaryVectorLayer<R, S>>
    extends AbstractTransformerLayer<DenseVector, R, S> {
  private static final long serialVersionUID = 1;

  /**
   * Returns a copy of this layer that will accept the inputs from the activations/output values of a list of
   * other layers (their outputs will be concatenated before being passed to this layer).
   *
   * @param layers the input layers whose activations/outputs will serve as inputs to this layer
   * @return a copy of this layer that will accept the specified layer's activations/outputs as inputs
   */
  @SafeVarargs
  public final S withInputs(NNLayer<DenseVector, ? extends NonTerminalLayer>... layers) {
    return withInput(new NNVectorConcatenationLayer().withAdditionalInputs(layers));
  }

  /**
   * Returns a copy of this layer that will accept as its input a vector of values that are assumed to be "dense-like".
   *
   * Specifically, <strong>vector elements with indices less than 0 or {@code >= maxLength} will be silently ignored by
   * the neural network.</strong>
   *
   * If the input vectors have negative or very high indices, use {@link #withInputFromDensifiedVectors(Producer[])}
   * instead, which will "densify" them and ensure that no values are truncated.  This method is intended for vectors
   * that are already dense-like (e.g. one/multi-hot vectors for labels).
   *
   * @param input a {@link Producer} that will provide a vector input to this layer
   * @param maxLength any vector elements with indices equal or greater to this value will be ignored
   * @return a copy of this layer that will accept the provided input
   */
  public S withInputFromTruncatedVector(long maxLength, Producer<? extends Vector> input) {
    return withInput(new NNVectorInputLayer().withMaxWidth(maxLength).withInput(input));
  }

  /**
   * Returns a copy of this layer that will accept as its input the provided dense vector.
   *
   * @param inputs a list of {@link Producer}s that will provide the vectors serving as input to this layer
   * @return a copy of this layer that will accept the provided inputs
   */
  public S withInputFromDenseVector(Producer<? extends DenseVector> inputs) {
    return withInputFromTruncatedVector(Long.MAX_VALUE, inputs);
  }

  /**
   * Returns a copy of this layer that will accept as its input a "densification" of the provided vectors, which is
   * automatically accomplished via the {@link DensifiedVector} transformer.
   *
   * @param inputs a list of {@link Producer}s that will provide the vectors serving as input to this layer
   * @return a copy of this layer that will accept the provided inputs
   */
  @SafeVarargs
  public final S withInputFromDensifiedVectors(Producer<? extends Vector>... inputs) {
    return withInputFromDenseVector(new DensifiedVector().withInputs(inputs));
  }

  @Override
  public S withInput(NNLayer<DenseVector, ? extends NonTerminalLayer> inputLayer) {
    return super.withInput(inputLayer);
  }
}
