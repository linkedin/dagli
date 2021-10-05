package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.visitor.VisitedBy;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.nn.result.NNResult;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.DynamicInputs;
import com.linkedin.dagli.util.invariant.Arguments;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Outputs a sequence of (sub)vectors constructed by splitting an input vector into equal-sized subvectors.  The input
 * vector must be evenly divisible by the requested split size.  The default split size (subvector length) is 1.
 *
 * For example, if the input vector is of shape [features], then the output vector sequence will be of shape
 * [features / splitSize, splitSize].
 *
 * The mapping of the elements in the input vector to the elements in the outputted vector sequence is
 * specified by {@link #withSequenceLinearization(SequenceLinearization)}.
 *
 * <strong>Caution:</strong> {@link NNSplitVectorSequenceLayer} is almost never necessary and should be avoided when
 * possible, since its correctness depends on the ordering of the input vector adhering to the expected linearization
 * scheme.  For example, rather than splitting an input vector you can simply input a sequence of vectors to your
 * neural network.
 *
 * <strong>Known limitation in DL4J:</strong> Masking information (if applicable) is not preserved.  Using
 * {@link NNLastVectorInSequenceLayer} on the returned sequence, which considers the original mask provided to the
 * neural network, may result in an exception if that mask does not fit the sequence outputted by this layer.
 */
@VisitedBy("NNLayerVisitor")
public class NNSplitVectorSequenceLayer
    extends AbstractUnaryVectorLayer<List<DenseVector>, NNSplitVectorSequenceLayer> implements NonTerminalLayer {
  private static final long serialVersionUID = 1;

  private Producer<? extends Number> _splitSizeProvider = new Constant<>(1);
  private SequenceLinearization _sequenceLinearization = SequenceLinearization.DEFAULT;

  /**
   * Gets the linearization scheme this layer will expect its inputs to follow.  By default, this is
   * {@link SequenceLinearization#DEFAULT}, in which case the default linearization used by the underlying neural
   * network library is expected.
   *
   * @return the linearization scheme expected by this layer
   */
  public SequenceLinearization getSequenceLinearization() {
    return _sequenceLinearization;
  }

  /**
   * Returns a copy of this instance that will split its inputs in accordance with the specified linearization scheme.
   * <strong>Not all linearizations will necessarily be supported by a given underlying neural network library.</strong>
   * Unsupported linearizations will result in an exception when trying to train the network.
   *
   * @param sequenceLinearization the linearization scheme the input vector will be expected to adhere to
   * @return a copy of this instance that will expect the specified linearization scheme
   */
  public NNSplitVectorSequenceLayer withSequenceLinearization(SequenceLinearization sequenceLinearization) {
    return clone(c -> c._sequenceLinearization = sequenceLinearization);
  }

  @Override
  public <R> R accept(NNLayerVisitor<R> visitor) {
    return visitor.visit(this);
  }

  /**
   * Returns a copy of this instance that will use the specified split size.  The shape of the output sequence of this
   * layer will be [inputVectorLength / splitSize, splitSize]; the splitSize must evenly (with no remainder) divide the
   * input vector's length.
   *
   * @param splitSize the split size for this layer
   * @return a copy of this instance with the specified split size
   */
  public NNSplitVectorSequenceLayer withSplitSize(long splitSize) {
    Arguments.check(splitSize >= 1, "The split size must be a positive integer");
    return withSplitSize(new Constant<>(splitSize));
  }

  /**
   * Returns a copy of this instance that will use the specified split size as determined by a constant-result Dagli
   * producer (see {@link Producer#hasConstantResult()} for details) when the DAG is executed.  The shape of the output
   * sequence of this layer will be [inputVectorLength / splitSize, splitSize]; the splitSize must evenly (with no
   * remainder) divide the input vector's length.
   *
   * @param splitSizeProvider a constant-result producer that will provide a split size for this layer
   * @return a copy of this instance with the specified split size
   */
  public NNSplitVectorSequenceLayer withSplitSize(Producer<? extends Number> splitSizeProvider) {
    Arguments.check(splitSizeProvider.hasConstantResult(), "The split size provider must be constant-result");
    return clone(c -> c._splitSizeProvider = splitSizeProvider);
  }

  @Override
  List<? extends Producer<?>> getDynamicConfigurationInputProducers() {
    return Collections.singletonList(_splitSizeProvider);
  }

  @Override
  DynamicLayerConfig getDynamicConfig(Map<NNLayer<?, ?>, DynamicLayerConfig> ancestorConfigs,
      DynamicInputs dynamicInputs, DynamicInputs.ConstantInputs constantInputs) {
    long splitSize = constantInputs.get(_splitSizeProvider).longValue();
    long parentSize = ancestorConfigs.get(getInputLayer()).getOutputElementCount();

    if (parentSize % splitSize != 0) {
      throw new IllegalStateException(
          "Split size " + splitSize + " does not evenly divide the parent layer's length " + parentSize);
    }

    return DynamicLayerConfig.Builder.setOutputShape(new long[] {parentSize / splitSize, splitSize}).build();
  }

  @Override
  Producer<List<DenseVector>> outputFromNNResult(Producer<? extends NNResult> nnResultProducer, int outputIndex) {
    return NNResult.InternalAPI.toVectorSequence(nnResultProducer, outputIndex);
  }
}
