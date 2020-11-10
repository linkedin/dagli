package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.math.vector.DenseVector;


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
  @Override
  public S withFirstInput(NNLayer<DenseVector, ? extends NonTerminalLayer> inputLayer) {
    return super.withFirstInput(inputLayer);
  }

  @Override
  public S withSecondInput(NNLayer<DenseVector, ? extends NonTerminalLayer> inputLayer) {
    return super.withSecondInput(inputLayer);
  }

  /**
   * @return a configurator for the first input to this layer
   */
  public DenseLayerInput<S> withFirstInput() {
    return new DenseLayerInput<>(this::withFirstInput);
  }

  /**
   * @return a configurator for the second input to this layer
   */
  public DenseLayerInput<S> withSecondInput() {
    return new DenseLayerInput<>(this::withFirstInput);
  }
}
