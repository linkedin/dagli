package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.visitor.VisitedBy;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.nn.result.NNResult;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.DynamicInputs;
import java.util.Map;


/**
 * Layer that calculates the dot product between the vectors provided by two input layers.  The widths (number of
 * activation values) of the input layers do not need to be the same.  The result is a single-element vector containing
 * the dot product.
 */
@VisitedBy(value = "NNLayerVisitor")
public class NNDotProductLayer extends AbstractBinaryVectorLayer<DenseVector, NNDotProductLayer> implements NonTerminalLayer {
  private static final long serialVersionUID = 1;

  @Override
  Producer<DenseVector> outputFromNNResult(Producer<? extends NNResult> nnResultProducer, int outputIndex) {
    return NNResult.InternalAPI.toVector(nnResultProducer, (outputIndex));
  }

  @Override
  public <R> R accept(NNLayerVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  DynamicLayerConfig getDynamicConfig(Map<NNLayer<?, ?>, DynamicLayerConfig> ancestorConfigs,
      DynamicInputs dynamicInputs, DynamicInputs.ConstantInputs constantInputs) {
    return DynamicLayerConfig.Builder.setOutputShape(new long[] { 1 }).build();
  }
}
