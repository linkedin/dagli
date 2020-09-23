package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.visitor.VisitedBy;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.nn.activation.ActivationFunction;
import com.linkedin.dagli.nn.activation.HyperbolicTangent;
import com.linkedin.dagli.nn.result.NNResult;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.DynamicInputs;
import com.linkedin.dagli.util.invariant.Arguments;
import java.util.Map;
import java.util.Objects;


/**
 * Applies an activation function to each value in its vector of inputs.
 */
@VisitedBy(value = "NNLayerVisitor")
public class NNActivationLayer extends AbstractUnaryVectorLayer<DenseVector, NNActivationLayer>
    implements NonTerminalLayer {
  private static final long serialVersionUID = 1;

  private ActivationFunction _activation = new HyperbolicTangent();
  private double _dropoutProbability = Double.NaN;

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
  public NNActivationLayer withDropoutProbability(double probability) {
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

  @Override
  Producer<DenseVector> outputFromNNResult(Producer<? extends NNResult> nnResultProducer, int outputIndex) {
    return NNResult.InternalAPI.toVector(nnResultProducer, outputIndex);
  }

  @Override
  DynamicLayerConfig getDynamicConfig(Map<NNLayer<?, ?>, DynamicLayerConfig> ancestorConfigs,
      DynamicInputs dynamicInputs, DynamicInputs.ConstantInputs constantInputs) {
    // our shape is the same as that of our input layer
    return DynamicLayerConfig.Builder.setOutputShape(ancestorConfigs.get(getInputLayer()).getOutputShape()).build();
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
   * Returns a copy of this instance that will use the specified activation function the transform its input values.
   *
   * The default activation function is {@link HyperbolicTangent} (tanh).
   *
   * @param activation the activation function to be used
   * @return a copy of this instance that will use the specified activation function
   */
  public NNActivationLayer withActivationFunction(ActivationFunction activation) {
    Objects.requireNonNull(activation);
    return clone(c -> c._activation = activation);
  }
}
