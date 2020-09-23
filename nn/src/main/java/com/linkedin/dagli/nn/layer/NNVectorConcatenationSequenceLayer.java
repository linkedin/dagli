package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.visitor.VisitedBy;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.nn.result.NNResult;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.DynamicInputs;
import com.linkedin.dagli.util.invariant.Arguments;
import java.util.List;
import java.util.Map;


/**
 * Given two or more input layers providing sequences of vectors, outputs a sequence of concatenated vectors created
 * by concatenating the vectors at each point in the sequence in each of the inputs.
 *
 * The sequences must have the same length.
 *
 * E.g. if the two sequences are have shapes [sequence length, vector length #1] and [sequence length, vector length #2]
 * the resulting sequence will have shape [sequence length,
 */
@VisitedBy("NNLayerVisitor")
public class NNVectorConcatenationSequenceLayer
    extends AbstractVariadicVectorSequenceLayer<List<DenseVector>, NNVectorConcatenationSequenceLayer>
    implements NonTerminalLayer {
  private static final long serialVersionUID = 1;

  @Override
  void validate() {
    super.validate();
    Arguments.check(getInputLayers().size() >= 2, "A concatenation layer must have at least two inputs");
  }

  @Override
  Producer<List<DenseVector>> outputFromNNResult(Producer<? extends NNResult> nnResultProducer, int outputIndex) {
    return NNResult.InternalAPI.toVectorSequence(nnResultProducer, outputIndex);
  }

  @Override
  public <R> R accept(NNLayerVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  DynamicLayerConfig getDynamicConfig(Map<NNLayer<?, ?>, DynamicLayerConfig> ancestorConfigs,
      DynamicInputs dynamicInputs, DynamicInputs.ConstantInputs constantInputs) {
    // dim 0 is time (sequence length), dim 1 is vector length
    long concatenatedVectorLength =
        getInputLayers().stream().mapToLong(layer -> ancestorConfigs.get(layer).getOutputShape()[1]).sum();

    long sequenceLength = ancestorConfigs.values().stream().findAny().get().getOutputShape()[0];
    Arguments.check(getInputLayers().stream()
            .mapToLong(layer -> ancestorConfigs.get(layer).getOutputShape()[0])
            .allMatch(len -> len == sequenceLength),
        "The lengths of the sequence inputs to  NNVectorConcatenationSequenceLayer must all be the same");

    return DynamicLayerConfig.Builder.setOutputShape(new long[] { sequenceLength, concatenatedVectorLength }).build();
  }
}
