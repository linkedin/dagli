package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.visitor.VisitedBy;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.nn.result.NNResult;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.DynamicInputs;
import java.util.Map;


/**
 * Outputs the linearization of all vectors in an input sequence of vectors, which will be a single (long) vector.
 *
 * The resulting order of elements from the source vectors can be specified using
 * {@link #withSequenceLinearization(SequenceLinearization)}; however, not all linearization schemes will necessarily be
 * supported by the underlying neural network library (a runtime exception will occur if an unsupported linearization
 * is requested.)  For example, at the time of this writing, DL4J does not support
 * {@link SequenceLinearization#BY_TIMESTEP}.
 *
 * If the input shape is [timesteps, features] then the output shape is [timesteps * features] (where
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

  private SequenceLinearization _sequenceLinearization = SequenceLinearization.DEFAULT;

  /**
   * Gets the linearization scheme used by this layer.  By default, this is {@link SequenceLinearization#DEFAULT}, in
   * which case the linearization is determined by the underlying neural network library.
   *
   * @return the linearization scheme used by this layer
   */
  public SequenceLinearization getSequenceLinearization() {
    return _sequenceLinearization;
  }

  /**
   * Returns a copy of this instance that will use the specified linearization scheme.  <strong>Not all linearizations
   * will necessarily be supported by a given underlying neural network library.</strong>  Unsupported linearizations
   * will result in an exception when trying to train the network; this can be reliably avoided by not requiring a
   * specific linearization scheme and instead leaving it at its default setting, {@link SequenceLinearization#DEFAULT}.
   *
   * @param sequenceLinearization the method to be used to linearize the input sequence of vectors
   * @return a copy of this instance that will use the specified linearization scheme
   */
  public NNLinearizedVectorSequenceLayer withSequenceLinearization(SequenceLinearization sequenceLinearization) {
    return clone(c -> c._sequenceLinearization = sequenceLinearization);
  }

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
