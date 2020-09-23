package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.visitor.VisitedBy;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.nn.activation.ActivationFunction;
import com.linkedin.dagli.nn.activation.HyperbolicTangent;
import com.linkedin.dagli.nn.result.NNResult;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.DynamicInputs;
import com.linkedin.dagli.util.invariant.Arguments;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * A standard fully-connected perceptron layer applied independently to each vector in an inputted sequence of vectors,
 * producing an output sequence of dimensions [length of input sequence, number of dense layer units/neurons].
 *
 * Only one perceptron layer (and thus one set of weights) is used; there is <strong>not</strong> a different perceptron
 * layer corresponding to each timestep.
 */
@VisitedBy("NNLayerVisitor")
public class NNSequentialDenseLayer extends AbstractUnaryVectorSequenceLayer<List<DenseVector>, NNSequentialDenseLayer>
    implements NonTerminalLayer {
  private static final long serialVersionUID = 1;

  private Producer<? extends Number> _unitCountProvider = new Constant<>(-1);
  private ActivationFunction _activation = new HyperbolicTangent();
  private double _dropoutProbability = Double.NaN;

// (Layer normalization is commented-out for now because it is currently broken in DL4J)
//  private boolean _hasLayerNormalization = false;
//
//  /**
//   * @return true if this layer will use Layer Normalization to normalize its output, false otherwise.
//   */
//  public boolean getLayerNormalization() {
//    return _hasLayerNormalization;
//  }
//
//  /**
//   * Returns a copy of this instance that will (or will not) use Layer Normalization, as described by
//   * <a href="https://arxiv.org/pdf/1607.06450.pdf">this paper</a>.
//   *
//   * Layer Normalization is disabled by default.
//   *
//   * @param useLayerNormalization true to use layer normalization, false otherwise
//   * @return a copy of this instance that will (or will not) use Layer Normalization
//   */
//  public NNMappedDenseLayer withLayerNormalization(boolean useLayerNormalization) {
//    return clone(c -> c._hasLayerNormalization = useLayerNormalization);
//  }

  /**
   * Returns a copy of this instance that will use the specified dropout rate.
   *
   * Each time an example is processed by the network, each element (number) in the input values passed to this layer
   * will have this (independent) probability of being "dropped" (set to 0), which can help mitigate overfitting.
   *
   * The default value, NaN, means "use the neural network's global dropout rate" (which itself defaults to 0).
   * Otherwise, values may range from 0 (no dropout) to 1 (drop everything, which is definitely not a good idea).
   *
   * @param probability a [0, 1] dropout probability, or NaN to use the global network rate
   * @return a copy of this instance that will use the specified dropout rate.
   */
  public NNSequentialDenseLayer withDropoutProbability(double probability) {
    Arguments.check(Double.isNaN(probability) || (probability >= 0 && probability <= 1), "Invalid probability");
    return clone(c -> c._dropoutProbability = probability);
  }

  /**
   * @return the [0, 1] dropout probability for this layer, or NaN if this layer should use the neural network's global
   *         dropout rate
   */
  public double getDropoutProbability() {
    return _dropoutProbability;
  }

  Producer<? extends Number> getUnitCountInputProducer() {
    return _unitCountProvider;
  }

  @Override
  Producer<List<DenseVector>> outputFromNNResult(Producer<? extends NNResult> nnResultProducer, int outputIndex) {
    return NNResult.InternalAPI.toVectorSequence(nnResultProducer, outputIndex);
  }

  @Override
  List<? extends Producer<?>> getDynamicConfigurationInputProducers() {
    return Collections.singletonList(_unitCountProvider);
  }

  @Override
  DynamicLayerConfig getDynamicConfig(Map<NNLayer<?, ?>, DynamicLayerConfig> ancestorConfigs,
      DynamicInputs dynamicInputs, DynamicInputs.ConstantInputs constantInputs) {
    long[] parentShape = ancestorConfigs.get(getInputLayer()).getOutputShape();
    long width = constantInputs.get(getUnitCountInputProducer()).longValue();
    if (width == -1) {
      width = parentShape[1];
    }
    return DynamicLayerConfig.Builder.setOutputShape(new long[] { parentShape[0], width }).build();
  }

  @Override
  public <R> R accept(NNLayerVisitor<R> visitor) {
    return visitor.visit(this);
  }

  /**
   * @return the activation function for this layer
   */
  public ActivationFunction getActivation() {
    return _activation;
  }

  /**
   * Returns a copy of this instance that will use the specified activation function the transform the neurons
   * output values (which are then passed to the next layer).  The default activation function is
   * {@link HyperbolicTangent} (tanh).
   *
   * @param activation the activation function to be used
   * @return a copy of this instance that will use the specified activation function
   */
  public NNSequentialDenseLayer withActivationFunction(ActivationFunction activation) {
    Objects.requireNonNull(activation);
    return clone(c -> c._activation = activation);
  }

  /**
   * Returns a copy of this instance that will have the specified "width" (number of neurons).  This is also the number
   * of output values produced by this layer.
   *
   * By default the width of this layer will be set to match the number of input values.  For example, if this layer
   * has another {@link NNSequentialDenseLayer} as its parent, its width will (by default) match the parent layer's
   * width.
   *
   * @param width the number of neurons in this layer, or -1 for "the same as the number of input values"
   * @return a copy of this instance with the specified width
   */
  public NNSequentialDenseLayer withUnitCount(int width) {
    Arguments.check(width == -1 || width >= 1, "Width must either be a positive integer or -1");
    return clone(c -> c._unitCountProvider = new Constant<>(width));
  }

  /**
   * Creates a stack of layers like this one.  The first layer will accept as input this layer's input, and all stacked
   * layers will have the same properties, except the specified unit (neural) counts.  The returned layer is the last in
   * the stack (with no other layer in the stack accepting it as input).
   *
   * @param unitCounts the unit counts for the stacked layers (at least one must be provided); the first
   *                   unit count corresponds to the first new stacked layer (the one accepting this layer's input)
   * @return the last created layer in the new stack of dense layers
   */
  public NNSequentialDenseLayer stack(int... unitCounts) {
    Arguments.check(unitCounts.length > 0, "At least one unit count for at least one new layer must be provided");
    NNSequentialDenseLayer previous = this.withUnitCount(unitCounts[0]).withInput(this.getInputLayer());

    for (int i = 1; i < unitCounts.length; i++) {
      previous = previous.withInput(previous).withUnitCount(unitCounts[i]);
    }

    return previous;
  }

  /**
   * Returns a copy of this instance that will have the specified "width" (number of neurons) as determined by a
   * constant-result Dagli node (see {@link Producer#hasConstantResult()} for details) when the
   * DAG is executed.  This is also the number of output values produced by this layer.
   *
   * @param widthProvider a constant-result producer that will provide a width for this layer
   * @return a copy of this instance with the specified width
   */
  public NNSequentialDenseLayer withUnitCount(Producer<? extends Number> widthProvider) {
    Arguments.check(widthProvider.hasConstantResult(), "The width provider must be constant-result");
    return clone(c -> c._unitCountProvider = widthProvider);
  }
}
