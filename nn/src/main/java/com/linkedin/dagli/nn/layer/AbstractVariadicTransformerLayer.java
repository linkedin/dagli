package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.util.collection.Iterables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Base class for NN transformations that operate on one or more input layers, such as pooling operations.
 *
 * @param <I> the type of inputs this layer accepts
 * @param <R> the canonical type of result produced by this layer
 * @param <S> the type of the class ultimately deriving from this one
 */
abstract class AbstractVariadicTransformerLayer<I, R, S extends AbstractVariadicTransformerLayer<I, R, S>>
    extends NNChildLayer<R, S> {
  private static final long serialVersionUID = 1;

  private List<NNLayer<I, ? extends NonTerminalLayer>> _inputLayers = Collections.emptyList();

  /**
   * @param layers the additional input layers to be added
   * @return a copy of this layer that will accept the provided input layers <strong>in addition to</strong> any
   *         existing existing input layers
   */
  protected S withAdditionalInputs(NNLayer<I, ? extends NonTerminalLayer>[] layers) {
    return clone(
        c -> ((AbstractVariadicTransformerLayer<I, ?, ?>) c)._inputLayers = Iterables.append(getInputLayers(), layers));
  }

  /**
   * @param layers the input layers to this instance
   * @return a copy of this layer that will accept the provided input layers, replacing any previously-specified input
   *         layers
   */
  @SafeVarargs
  public final S withInputs(NNLayer<I, ? extends NonTerminalLayer>... layers) {
    return clone(
        c -> ((AbstractVariadicTransformerLayer<I, ?, ?>) c)._inputLayers = new ArrayList<>(Arrays.asList(layers)));
  }

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

    @Override
    public List<? extends NNLayer<I, ? extends NonTerminalLayer>> getInputLayers() {
      return AbstractVariadicTransformerLayer.this.getInputLayers();
    }
  }

  @Override
  List<? extends NNLayer<I, ? extends NonTerminalLayer>> getInputLayers() {
    return _inputLayers;
  }
}
