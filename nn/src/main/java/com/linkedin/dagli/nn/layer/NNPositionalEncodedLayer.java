package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.visitor.VisitedBy;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.nn.result.NNResult;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.DynamicInputs;
import java.util.List;
import java.util.Map;


/**
 * A positional encoding layer adds a positional encoding to a sequence of vectors as given by Section 3.5 of the paper
 * <a href="https://arxiv.org/abs/1706.03762">Attention is All You Need</a>.  Using positional encoding is generally
 * a good idea if you are applying self-attention to a sequence where the ordering matters (e.g. text), as otherwise
 * the network has no way to differentiate the 1st timestep from the 100th if the vectors at these positions are
 * otherwise equal.
 *
 * If t is the 0-based "timestep" in the sequence and d is the number of elements in each vector in the sequence, the
 * value for each element of the positional encoding for t is given by:
 * position_encoding(t, 2 * i) = sin(t * 10000^{-2i/d})
 * position_encoding(t, 2 * i + 1) = cos(t * 10000^{-2i/d})
 *
 * This positional encoding is simply added to the original sequence; the output of this layer is the summed sequence.
 */
@VisitedBy("NNLayerVisitor")
public class NNPositionalEncodedLayer
    extends AbstractUnaryVectorSequenceLayer<List<DenseVector>, NNPositionalEncodedLayer> implements NonTerminalLayer {
  private static final long serialVersionUID = 1;

  @Override
  Producer<List<DenseVector>> outputFromNNResult(Producer<? extends NNResult> nnResultProducer, int outputIndex) {
    return NNResult.InternalAPI.toVectorSequence(nnResultProducer, outputIndex);
  }

  @Override
  public <T> T accept(NNLayerVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  DynamicLayerConfig getDynamicConfig(Map<NNLayer<?, ?>, DynamicLayerConfig> ancestorConfigs,
      DynamicInputs dynamicInputs, DynamicInputs.ConstantInputs constantInputs) {
    return DynamicLayerConfig.Builder.setOutputShape(ancestorConfigs.get(this.getInputLayer()).getOutputShape())
        .build();
  }
}
