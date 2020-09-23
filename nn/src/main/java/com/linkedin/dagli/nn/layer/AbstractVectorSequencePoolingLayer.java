package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.nn.result.NNResult;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.DynamicInputs;
import java.util.Map;


/**
 * Base class for layers that pool a sequence of vectors from the input layer to create a single vector output.
 *
 * The size of the output vector is the size of each vector in the input sequence.
 */
abstract class AbstractVectorSequencePoolingLayer<S extends AbstractVectorSequencePoolingLayer<S>>
    extends AbstractUnaryVectorSequenceLayer<DenseVector, S>
    implements NonTerminalLayer {
  private static final long serialVersionUID = 1;

  @Override
  Producer<DenseVector> outputFromNNResult(Producer<? extends NNResult> nnResultProducer, int outputIndex) {
    return NNResult.InternalAPI.toVector(nnResultProducer, outputIndex);
  }

  @Override
  DynamicLayerConfig getDynamicConfig(Map<NNLayer<?, ?>, DynamicLayerConfig> ancestorConfigs,
      DynamicInputs dynamicInputs, DynamicInputs.ConstantInputs constantInputs) {
    long units = ancestorConfigs.get(getInputLayer()).getOutputShape()[1]; // dim 0 is time, dim 1 is vector length
    return DynamicLayerConfig.Builder.setOutputShape(new long[] { units }).build();
  }
}
