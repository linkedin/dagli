package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.visitor.VisitedBy;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.nn.activation.ActivationFunction;
import com.linkedin.dagli.nn.activation.HyperbolicTangent;
import com.linkedin.dagli.nn.activation.Sigmoid;
import com.linkedin.dagli.nn.result.NNResult;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.DynamicInputs;
import com.linkedin.dagli.util.invariant.Arguments;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * A <a href="https://en.wikipedia.org/wiki/Long_short-term_memory">LSTM (Long Short-Term Memory)</a> layer.  An LSTM
 * is a type of Recurrent Neural Network (RNN) that transforms a sequence of vectors into another sequence of
 * vectors where the result vector at each timestep captures "contextualized" information about the meaning of the
 * corresponding input vector given the previous (forward pass) or subsequent (backward pass) timesteps.
 *
 * LSTMs tend to be better at avoiding the vanishing gradient problem (and thus learning more distal dependencies) than
 * traditional/simple RNNs, but given a long enough sequence this will still be an issue.
 *
 * Stacking LSTM layers (to capture longer-distance or more abstract relationships among the inputs) is not uncommon;
 * this can be conveniently done via {@link #stack(int...)}.
 */
@VisitedBy("NNLayerVisitor")
public class NNLSTMLayer extends AbstractUnaryVectorSequenceLayer<List<DenseVector>, NNLSTMLayer>
    implements NonTerminalLayer {
  private static final long serialVersionUID = 1;

  private Producer<? extends Number> _unitCountProvider = new Constant<>(-1);
  private ActivationFunction _activation = new HyperbolicTangent();
  private ActivationFunction _recurrentActivation = new Sigmoid();
  private double _dropoutProbability = Double.NaN;
  private Bidirectionality _bidirectionality = Bidirectionality.FORWARD_ONLY;

  /**
   * @return the {@link Bidirectionality} mode of this RNN layer; the default is forward-only (no bidirectionality)
   */
  public Bidirectionality getBidirectionality() {
    return _bidirectionality;
  }

  /**
   * Returns a copy of this instance that will use the specified {@link Bidirectionality} mode to produce the layer's
   * output from a mixture of the outputs of a forward and backward pass.  The default bidirectionality mode is
   * {@link Bidirectionality#FORWARD_ONLY}, in which the LSTM layer will use the output from a forward pass only.
   *
   * Bidrectionality is especially important in longer sequences when (only) the last timestep's output is being passed
   * to subsequent layers, since a forward pass would tend to forget the beginning of the sequence by this point.  More
   * generally, it also allows the output for the input at a given timestep to depend on both what came before and
   * after, which can be important in, e.g. text classification (where a word's meaning depends on the words around it
   * in both directions).
   *
   * Forward and backward passes are independent and do not share parameters.
   *
   * @param bidirectionality the bidirectionality mode to use
   * @return a copy of this instance that will use the specified {@link Bidirectionality} mode to produce the layer's
   *         output from a mixture of the outputs of a forward and backward pass
   */
  public NNLSTMLayer withBidirectionality(Bidirectionality bidirectionality) {
    return clone(c -> c._bidirectionality = bidirectionality);
  }

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
  public NNLSTMLayer withDropoutProbability(double probability) {
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
  Producer<List<DenseVector>> outputFromNNResult(Producer<? extends NNResult> nnResultProducer, int outputIndex) {
    return NNResult.InternalAPI.toVectorSequence(nnResultProducer, (outputIndex));
  }

  /**
   * @return the recurrent activation function used by this layer
   */
  public ActivationFunction getRecurrentActivation() {
    return _recurrentActivation;
  }

  /**
   * @return a copy of this layer that will use the specified activation function at each recurrent step (note that
   * this is distinct from the layer's overall activation function); {@link Sigmoid} is the default
   *
   * @param recurrentActivation the recurrent activation function to use
   */
  public NNLSTMLayer withRecurrentActivation(ActivationFunction recurrentActivation) {
    return clone(c -> c._recurrentActivation = recurrentActivation);
  }

  /**
   * @return a constant-result producer providing the width (number of units) in this layer
   */
  Producer<? extends Number> getUnitCountInputProducer() {
    return _unitCountProvider;
  }

  @Override
  List<? extends Producer<?>> getDynamicConfigurationInputProducers() {
    return Collections.singletonList(_unitCountProvider);
  }

  @Override
  DynamicLayerConfig getDynamicConfig(Map<NNLayer<?, ?>, DynamicLayerConfig> ancestorConfigs,
      DynamicInputs dynamicInputs, DynamicInputs.ConstantInputs constantInputs) {
    long[] parentShape = ancestorConfigs.get(getInputLayer()).getOutputShape();

    long unitCount = constantInputs.get(getUnitCountInputProducer()).longValue();
    if (unitCount == -1) {
      unitCount = parentShape[1];
    }
    return DynamicLayerConfig.Builder
        .setOutputShape(
            new long[]{parentShape[0], _bidirectionality == Bidirectionality.CONCATENATED ? 2 * unitCount : unitCount})
        .build();
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
  public NNLSTMLayer withActivationFunction(ActivationFunction activation) {
    return clone(c -> c._activation = activation);
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
  public NNLSTMLayer stack(int... unitCounts) {
    Arguments.check(unitCounts.length > 0, "At least one unit count for at least one new layer must be provided");
    NNLSTMLayer previous = this.withUnitCount(unitCounts[0]).withInput(this.getInputLayer());

    for (int i = 1; i < unitCounts.length; i++) {
      previous = previous.withInput(previous).withUnitCount(unitCounts[i]);
    }

    return previous;
  }

  /**
   * Returns a copy of this instance that will have the specified number of LSTM units (the "width" of the layer).
   * For each example the layer will produce an output matrix of dimensions of either [length of time sequence,
   * 2 * number of units] if {@link Bidirectionality#CONCATENATED} bidirectionality is specified, or [length of time
   * sequence, number of units] otherwise.
   *
   * By default the number of units of this layer will be set to match the number of values in each timestep's input
   * vector.  For example, if this layer has a {@link NNSequentialEmbeddingLayer} as its parent, the unit count will be
   * the same as the embedding length
   *
   * @param count the number of LSTM units in this layer, or -1 for "the same as the number of input vector values in
   *              each timestep"
   * @return a copy of this instance with the specified number of LSTM units
   */
  public NNLSTMLayer withUnitCount(int count) {
    Arguments.check(count == -1 || count >= 1, "Count must either be a positive integer or -1");
    return withUnitCount(new Constant<>(count));
  }

  /**
   * Returns a copy of this instance that will have the specified number of LSTM units as determined by a
   * constant-result Dagli node (see {@link Producer#hasConstantResult()} for details) when the DAG is executed.  For
   * each example the layer will produce an output matrix of dimensions of either [length of time sequence, 2 * number
   * of units] if {@link Bidirectionality#CONCATENATED} bidirectionality is specified, or [length of time sequence,
   * number of units] otherwise.
   *
   * By default the number of units of this layer will be set to match the number of values in each timestep's input
   * vector.  For example, if this layer has a {@link NNSequentialEmbeddingLayer} as its parent, the unit count will be
   * the same as the embedding length
   *
   * @param countProvider a constant-result producer that will provide the number of LSTM units in this layer, or -1 for
   *                      "the same as the number of input vector values in each timestep"
   * @return a copy of this instance with the specified number of LSTM units
   */
  public NNLSTMLayer withUnitCount(Producer<? extends Number> countProvider) {
    Arguments.check(countProvider.hasConstantResult(), "The unit count provider must be constant-result");
    return clone(c -> c._unitCountProvider = countProvider);
  }
}
