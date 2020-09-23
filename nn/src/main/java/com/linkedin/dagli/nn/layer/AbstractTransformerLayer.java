package com.linkedin.dagli.nn.layer;

import java.util.Collections;
import java.util.List;


/**
 * Base class for layers that accept a single input layer.
 *
 * @param <A> the canonical input type accepted by this layer
 * @param <R> the canonical result type of this layer
 * @param <S> the ultimate derived class descending from this layer
 */
abstract class AbstractTransformerLayer<A, R, S extends AbstractTransformerLayer<A, R, S>>
    extends NNChildLayer<R, S> {
  private static final long serialVersionUID = 1;

  NNLayer<A, ? extends NonTerminalLayer> _inputLayer = null;

  @Override
  public InternalAPI internalAPI() {
    return new InternalAPI();
  }

  public class InternalAPI extends NNChildLayer<R, S>.InternalAPI {
    /**
     * @return the (sole) input layer for this node
     */
    public NNLayer<A, ? extends NonTerminalLayer> getInputLayer() {
      return AbstractTransformerLayer.this.getInputLayer();
    }
  }

  /**
   * Returns a copy of this layer that will accept the specified layer's activations/output values as inputs.
   *
   * @param inputLayer the input layer whose activations/outputs will serve as inputs to this layer
   * @return a copy of this layer that will accept the specified layer's activations/outputs as inputs
   */
  protected S withInput(NNLayer<A, ? extends NonTerminalLayer> inputLayer) {
    return clone(c -> c._inputLayer = inputLayer);
  }

  @Override
  List<? extends NNLayer<?, ? extends NonTerminalLayer>> getInputLayers() {
    return Collections.singletonList(getInputLayer());
  }

  /**
   * @return the (sole) input layer for this node
   */
  NNLayer<A, ? extends NonTerminalLayer> getInputLayer() {
    return _inputLayer;
  }
}
