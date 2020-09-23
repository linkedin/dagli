package com.linkedin.dagli.nn.layer;

import java.util.Arrays;
import java.util.List;


abstract class AbstractBinaryTransformerLayer<A, B, R, S extends AbstractBinaryTransformerLayer<A, B, R, S>>
    extends NNChildLayer<R, S> {
  private static final long serialVersionUID = 1;

  NNLayer<A, ? extends NonTerminalLayer> _firstInputLayer = null;
  NNLayer<B, ? extends NonTerminalLayer> _secondInputLayer = null;

  @Override
  public InternalAPI internalAPI() {
    return new InternalAPI();
  }

  /**
   * Methods provided exclusively for use by Dagli.
   *
   * Client code should not use these methods as they are subject to change at any time.
   */
  public class InternalAPI extends NNChildLayer<R, S>.InternalAPI {
    InternalAPI() { }

    /**
     * @return the first input layer for this node
     */
    public NNLayer<A, ? extends NonTerminalLayer> getFirstInputLayer() {
      return _firstInputLayer;
    }

    /**
     * @return the second input layer for this node
     */
    public NNLayer<B, ? extends NonTerminalLayer> getSecondInputLayer() {
      return _secondInputLayer;
    }
  }

  /**
   * Returns a copy of this layer that will accept the specified layer's activations/output values as its first
   * operand.
   *
   * @param inputLayer the input layer whose activations/outputs will serve as the first operand for this layer
   * @return a copy of this layer that will accept the specified layer's activations/outputs as its first operand
   */
  protected S withFirstInput(NNLayer<A, ? extends NonTerminalLayer> inputLayer) {
    return clone(c -> c._firstInputLayer = inputLayer);
  }

  /**
   * Returns a copy of this layer that will accept the specified layer's activations/output values as its second
   * operand.
   *
   * @param inputLayer the input layer whose activations/outputs will serve as the second operand for this layer
   * @return a copy of this layer that will accept the specified layer's activations/outputs as its second operand
   */
  protected S withSecondInput(NNLayer<B, ? extends NonTerminalLayer> inputLayer) {
    return clone(c -> c._secondInputLayer = inputLayer);
  }

  @Override
  List<? extends NNLayer<?, ? extends NonTerminalLayer>> getInputLayers() {
    return Arrays.asList(_firstInputLayer, _secondInputLayer);
  }

  /**
   * @return the first input layer for this node
   */
  NNLayer<A, ? extends NonTerminalLayer> getFirstInputLayer() {
    return _firstInputLayer;
  }

  /**
   * @return the second input layer for this node
   */
  NNLayer<B, ? extends NonTerminalLayer> getSecondInputLayer() {
    return _secondInputLayer;
  }
}
