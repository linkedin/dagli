package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.visitor.VisitedBy;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.nn.result.NNResult;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.DynamicInputs;
import com.linkedin.dagli.util.invariant.Arguments;
import java.util.Map;


/**
 * Pooling operation that simply concatenates its input vectors.
 */
@VisitedBy("NNLayerVisitor")
public class NNVectorConcatenationLayer extends AbstractVariadicVectorLayer<DenseVector, NNVectorConcatenationLayer>
    implements NonTerminalLayer {
  private static final long serialVersionUID = 1;

  @Override
  Producer<DenseVector> outputFromNNResult(Producer<? extends NNResult> nnResultProducer, int outputIndex) {
    return NNResult.InternalAPI.toVector(nnResultProducer, outputIndex);
  }

  @Override
  void validate() {
    super.validate();
    Arguments.check(getInputLayers().size() >= 2, "A concatenation layer must have at least two inputs");
  }

  @Override
  public <R> R accept(NNLayerVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  DynamicLayerConfig getDynamicConfig(Map<NNLayer<?, ?>, DynamicLayerConfig> ancestorConfigs,
      DynamicInputs dynamicInputs, DynamicInputs.ConstantInputs constantInputs) {
    long concatenatedLength =
        getInputLayers().stream().mapToLong(layer -> ancestorConfigs.get(layer).getOutputShape()[0]).sum();
    return DynamicLayerConfig.Builder.setOutputShape(new long[] { concatenatedLength }).build();
  }
}
