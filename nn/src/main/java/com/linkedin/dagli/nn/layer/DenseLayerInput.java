package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.math.vector.DenseVector;
import java.util.function.Function;


/**
 * Input configurator for dense layer inputs.
 *
 * @param <T> the type of layer whose input is to be configured
 */
public class DenseLayerInput<T>
    extends AbstractDenseLayerInput<Void, T, DenseLayerInput<T>> {
  private final Function<NNLayer<DenseVector, ? extends NonTerminalLayer>, T> _withInputFunction;

  /**
   * Creates a new instance that will use the specified with-input-layer method to create its result.
   * @param withInputFunction a method accepting a dense layer and returning the configured object
   */
  public DenseLayerInput(Function<NNLayer<DenseVector, ? extends NonTerminalLayer>, T> withInputFunction) {
    _withInputFunction = withInputFunction;
  }

  @Override
  protected T fromLayer(NNLayer<DenseVector, ? extends NonTerminalLayer> denseLayer) {
    return _withInputFunction.apply(denseLayer);
  }

  /**
   * Gets an "aggregated" configurator that concatenates multiple input values into a single layer input.
   *
   * After all input values have been specified, call {@link Aggregated#done()} to obtain the configured transformer.
   *
   * @return an "aggregated" configurator that concatenates multiple input values into a single layer input
   */
  public Aggregated concatenating() {
    return new Aggregated();
  }

  /**
   * Input configurator that aggregates multiple inputs (until {@link #done()} is called).
   */
  public class Aggregated extends AbstractDenseLayerInput<NNLayer<DenseVector, ? extends NonTerminalLayer>, Aggregated, Aggregated> {

    @Override
    protected Aggregated fromLayer(NNLayer<DenseVector, ? extends NonTerminalLayer> denseLayer) {
      return withAddedInput(denseLayer);
    }

    /**
     * Called when all inputs have been added.
     *
     * @return the resulting object that will accept the configured aggregated input
     */
    public T done() {
      return DenseLayerInput.this.fromLayers(getInputs());
    }
  }
}
