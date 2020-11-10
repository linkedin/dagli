package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.visitor.VisitedBy;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.nn.loss.LossFunction;
import com.linkedin.dagli.nn.loss.SquaredErrorLoss;
import com.linkedin.dagli.nn.result.NNResult;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.DynamicInputs;
import java.util.Map;


/**
 * Represents a regression loss "layer" used to train the neural network.  The target vector, the "true label", is
 * provided by a {@link Producer} in the encapsulating DAG.
 *
 * The target vectors must be dense, and any of their elements with indices equal or greater than the width of the
 * {@link NNRegression}'s parent layer are ignored.  If the size of the dense target vectors is not known a priori, you
 * can set the width of the parent layer (e.g. via {@link NNDenseLayer#withUnitCount(Producer)} to the value calculated
 * by {@code new Max<Long>().withInput(new DenseVectorCapacity().withInput(yourTargetVectorProducer))}.
 */
@VisitedBy("NNLayerVisitor")
public class NNRegression extends AbstractVectorLossLayer<DenseVector, NNRegression> {
  private static final long serialVersionUID = 1;

  private LossFunction _lossFunction = new SquaredErrorLoss(); // it's hip to be square

  @Override
  Producer<DenseVector> outputFromNNResult(Producer<? extends NNResult> nnResultProducer, int outputIndex) {
    return NNResult.InternalAPI.toVector(nnResultProducer, (outputIndex));
  }

  @Override
  public <R> R accept(NNLayerVisitor<R> visitor) {
    return visitor.visit(this);
  }

  /**
   * Returns a copy of this instance that will use the provided vectors as their target vectors, which (in combination
   * with the loss function) are used to train the model.  The highest non-zero element's index should be less than
   * the parent layer's width (e.g. if the parent layer has a width of 6, no non-zero element in the target vectors may
   * have an index greater than 5).
   *
   * @param labelInput the producer that will provide the target vectors used to train the neural network
   * @return a copy of this instance that will obtain its target vectors from the specified source
   */
  public NNRegression withLabelInput(Producer<? extends Vector> labelInput) {
    return clone(c -> c._supervisionProvider = labelInput);
  }

  /**
   * Returns a copy of this layer that will calculate the loss for the prediction provided by the specified input layer.
   *
   * @param inputLayer the layer that will provide a vector of input values predicted by the neural network, which will
   *                   then be compared against the regression labels to determine the loss for each example
   * @return a copy of this layer that will accept the specified input layer
   */
  public NNRegression withPredictionInput(NNLayer<DenseVector, ? extends NonTerminalLayer> inputLayer) {
    return withInput(inputLayer);
  }

  /**
   * @return a configurator that will configure the prediction input to this layer
   */
  public DenseLayerInput<NNRegression> withPredictionInput() {
    return new DenseLayerInput<>(this::withPredictionInput);
  }

  /**
   * Returns a copy of this instance that will use the specified loss function.  The default loss function is
   * {@link SquaredErrorLoss}.
   *
   * @param lossFunction the loss function to be used
   * @return a copy of this instance that will use the specified loss function
   */
  public NNRegression withLossFunction(LossFunction lossFunction) {
    return clone(c -> c._lossFunction = lossFunction);
  }

  /**
   * @return the loss function for this layer
   */
  public LossFunction getLossFunction() {
    return _lossFunction;
  }

  @Override
  DynamicLayerConfig getDynamicConfig(Map<NNLayer<?, ?>, DynamicLayerConfig> ancestorConfigs,
      DynamicInputs dynamicInputs, DynamicInputs.ConstantInputs constantInputs) {
    return DynamicLayerConfig.Builder
        .setOutputShape(ancestorConfigs.get(_inputLayer).getOutputShape().clone()).build();
  }

}
