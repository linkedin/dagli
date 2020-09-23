package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.visitor.VisitedBy;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.nn.result.NNResult;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.DynamicInputs;
import com.linkedin.dagli.util.invariant.Arguments;
import java.util.Map;


/**
 * A layer that <a href="https://en.wikipedia.org/wiki/Batch_normalization"><i>batch normalizes</i></a> its input.
 */
@VisitedBy("NNLayerVisitor")
public class NNBatchNormalizedLayer extends AbstractUnaryVectorLayer<DenseVector, NNBatchNormalizedLayer>
    implements NonTerminalLayer {
  private static final long serialVersionUID = 1;

  private double _decayRate = 0.1;

  /**
   * Returns a copy of this instance that will use the specified decay rate.
   *
   * Batch normalization attempts to estimate the global mean and variance of a layer's outputs from the observed
   * statistics over minibatches, updating these according to:
   * nextGlobalEstimate = (1 - decayRate) * currentGlobalEstimate + decayRate * currentMinibatchStatistics
   *
   * Higher decay rates give greater credence to more recently seen minibatches.  The default decay rate is 0.1.
   *
   * @param decayRate a (0, 1] decay rate to use
   * @return a copy of this instance that will use the specified decay rate.
   */
  public NNBatchNormalizedLayer withGlobalDecayRate(double decayRate) {
    Arguments.check((decayRate > 0 && decayRate <= 1), "Invalid decay rate");
    return clone(c -> c._decayRate = decayRate);
  }

  /**
   * @return the (0, 1] global decay rate used by this layer
   */
  public double getGlobalDecayRate() {
    return _decayRate;
  }

  @Override
  Producer<DenseVector> outputFromNNResult(Producer<? extends NNResult> nnResultProducer, int outputIndex) {
    return NNResult.InternalAPI.toVector(nnResultProducer, outputIndex);
  }

  @Override
  DynamicLayerConfig getDynamicConfig(Map<NNLayer<?, ?>, DynamicLayerConfig> ancestorConfigs,
      DynamicInputs dynamicInputs, DynamicInputs.ConstantInputs constantInputs) {
    return DynamicLayerConfig.Builder.setOutputShape(ancestorConfigs.get(getInputLayer()).getOutputShape()).build();
  }

  @Override
  public <R> R accept(NNLayerVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
