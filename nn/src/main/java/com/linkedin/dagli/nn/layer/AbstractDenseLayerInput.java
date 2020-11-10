package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.input.AbstractFeatureVectorInput;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.vector.DensifiedVector;
import java.util.Arrays;
import java.util.List;


/**
 * Base class for configurators for dense inputs to a neural network layer.
 *
 * @param <V> the type of inputs that may be aggregated (if applicable; otherwise Void)
 * @param <T> the type of the configured result object (e.g. a layer)
 * @param <S> the type of the derived-most class extended this abstract class
 */
public abstract class AbstractDenseLayerInput<V, T, S extends AbstractDenseLayerInput<V, T, S>>
    extends AbstractFeatureVectorInput<V, T, S> {

  @Override
  protected T fromDenseVector(Producer<? extends DenseVector> vector) {
    return fromLayer(new NNVectorInputLayer().withInput(vector));
  }

  @Override
  protected T fromVector(Producer<? extends Vector> vector) {
    return fromDenseVector(DensifiedVector.densifyIfSparse(vector));
  }

  /**
   * Configures the result to accept one or more input layers that will be concatenated together (the total width is the
   * sum of the constituent input layer widths).
   *
   * @param inputLayers the layers whose outputs will be concatenated to provide the input
   * @return a result that is configured to accept the concatenated outputs of the given layers
   */
  @SafeVarargs
  public final T fromLayers(NNLayer<DenseVector, ? extends NonTerminalLayer>... inputLayers) {
    return fromLayers(Arrays.asList(inputLayers));
  }

  /**
   * Configures the result to accept one or more input layers that will be concatenated together (the total width is the
   * sum of the constituent input layer widths).
   *
   * @param inputLayers the layers whose outputs will be concatenated to provide the input
   * @return a result that is configured to accept the concatenated outputs of the given layers
   */
  public T fromLayers(List<? extends NNLayer<DenseVector, ? extends NonTerminalLayer>> inputLayers) {
    return fromLayer(
        inputLayers.size() == 1 ? inputLayers.get(0) : new NNVectorConcatenationLayer().withInputs(inputLayers));
  }

  protected abstract T fromLayer(NNLayer<DenseVector, ? extends NonTerminalLayer> denseLayer);
}