package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.visitor.VisitedBy;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.nn.result.NNResult;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.DynamicInputs;
import java.util.Map;


/**
 * Outputs the concatenation of all vectors in an input sequence of vectors, which will be a single (long) vector.
 *
 * For example, if the input shape is [timesteps, features] then the output shape is [timesteps * features] (where
 * timesteps is the number of vectors in the sequence, and features is the length of each vector).
 *
 * Masking (if applicable) is applied to the linearized vector (i.e. elements from vectors in the sequence that are
 * masked out will be set to 0), but does not continue to "pass through" the network and will not be present when/if the
 * linearized vector is split back into a sequence.
 */
@VisitedBy("NNLayerVisitor")
public class NNLinearizedVectorSequenceLayer
    extends AbstractUnaryVectorSequenceLayer<DenseVector, NNLinearizedVectorSequenceLayer> implements NonTerminalLayer {
  private static final long serialVersionUID = 1;

  @Override
  public <R> R accept(NNLayerVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  DynamicLayerConfig getDynamicConfig(Map<NNLayer<?, ?>, DynamicLayerConfig> ancestorConfigs,
      DynamicInputs dynamicInputs, DynamicInputs.ConstantInputs constantInputs) {
    long size = ancestorConfigs.get(getInputLayer()).getOutputElementCount();
    return DynamicLayerConfig.Builder.setOutputShape(new long[] { size }).build();
  }

  @Override
  Producer<DenseVector> outputFromNNResult(Producer<? extends NNResult> nnResultProducer, int outputIndex) {
    return NNResult.InternalAPI.toVector(nnResultProducer, outputIndex);
  }
}
