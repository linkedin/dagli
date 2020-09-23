package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.nn.result.NNResult;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.DynamicInputs;
import com.linkedin.dagli.util.invariant.Arguments;
import java.util.List;
import java.util.Map;


/**
 * Layer that calculates the Hadamard (element-wise) product of the vectors provided by input layers.  The size of
 * the input layers do not need to be the same--the size of the output will be the smallest of any of the input layer
 * sizes.
 */
abstract class AbstractElementWiseSequenceLayer<S extends AbstractElementWiseSequenceLayer<S>>
    extends AbstractVariadicVectorSequenceLayer<List<DenseVector>, S> implements NonTerminalLayer {
  private static final long serialVersionUID = 1;

  @Override
  Producer<List<DenseVector>> outputFromNNResult(Producer<? extends NNResult> nnResultProducer, int outputIndex) {
    return NNResult.InternalAPI.toVectorSequence(nnResultProducer, outputIndex);
  }

  @Override
  DynamicLayerConfig getDynamicConfig(Map<NNLayer<?, ?>, DynamicLayerConfig> ancestorConfigs,
      DynamicInputs dynamicInputs, DynamicInputs.ConstantInputs constantInputs) {
    Arguments.check(getInputLayers().stream()
        .map(ancestorConfigs::get)
        .mapToLong(config -> config.getOutputShape()[0])
        .distinct()
        .count() == 1, "Not all inputs have the same sequence length");
    Arguments.check(getInputLayers().stream()
        .map(ancestorConfigs::get)
        .mapToLong(config -> config.getOutputShape()[1])
        .distinct()
        .count() == 1, "Not all inputs have the same vector length");

    return DynamicLayerConfig.Builder.setOutputShape(ancestorConfigs.get(getInputLayers().get(0)).getOutputShape()).build();
  }
}
