package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.nn.result.NNResult;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.DynamicInputs;
import java.util.Map;
import java.util.stream.LongStream;


/**
 * Layer that calculates the Hadamard (element-wise) product of the vectors provided by input layers.  The size of
 * the input layers do not need to be the same--the size of the output will be the smallest of any of the input layer
 * sizes.
 */
public abstract class AbstractElementWiseLayer<S extends AbstractElementWiseLayer<S>>
    extends AbstractVariadicVectorLayer<DenseVector, S> implements NonTerminalLayer {
  private static final long serialVersionUID = 1;

  @Override
  Producer<DenseVector> outputFromNNResult(Producer<? extends NNResult> nnResultProducer, int outputIndex) {
    return NNResult.InternalAPI.toVector(nnResultProducer, outputIndex);
  }

  /**
   * @return true if the size of the output is the <i>minimum</i> of the sizes of the input vectors; false if it is,
   *         conversely, the maximum
   */
  abstract boolean isOutputSizeMinInputSize();

  @Override
  DynamicLayerConfig getDynamicConfig(Map<NNLayer<?, ?>, DynamicLayerConfig> ancestorConfigs,
      DynamicInputs dynamicInputs, DynamicInputs.ConstantInputs constantInputs) {
    LongStream sizeStream = getInputLayers().stream()
        .map(ancestorConfigs::get)
        .mapToLong(DynamicLayerConfigBase::getOutputElementCount);

    return DynamicLayerConfig.Builder.setOutputShape(new long[]{
        (isOutputSizeMinInputSize() ? sizeStream.min() : sizeStream.max()).orElseThrow(
            () -> new IllegalStateException("Child layer has no parent"))}).build();
  }
}
