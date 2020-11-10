package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.math.vector.DenseVector;


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

  @Override
  public S withInput(NNLayer<DenseVector, ? extends NonTerminalLayer> inputLayer) {
    return super.withInput(inputLayer);
  }

  /**
   * @return a configurator for the input to this layer
   */
  public DenseLayerInput<S> withInput() {
    return new DenseLayerInput<>(this::withInput);
  }
}
