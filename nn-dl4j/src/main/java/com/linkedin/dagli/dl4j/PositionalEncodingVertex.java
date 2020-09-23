package com.linkedin.dagli.dl4j;

import org.deeplearning4j.nn.api.MaskState;
import org.deeplearning4j.nn.conf.graph.GraphVertex;
import org.deeplearning4j.nn.conf.layers.samediff.SameDiffLambdaVertex;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.common.primitives.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;


/**
 * Applies positional encoding to an input sequence of vectors as described by Section 3.5 of the paper
 * <a href="https://arxiv.org/abs/1706.03762">Attention is All You Need</a>.
 *
 *  position_encoding(t, 2 * i) = sin(t * 10000^{-2i/d})
 *  position_encoding(t, 2 * i + 1) = cos(t * 10000^{-2i/d})
 */
public class PositionalEncodingVertex extends SameDiffLambdaVertex {
  private static final long serialVersionUID = 1;

  private final long _sequenceLength;
  private final long _vectorLength;

  public PositionalEncodingVertex(long sequenceLength, long vectorLength) {
    _sequenceLength = sequenceLength;
    _vectorLength = vectorLength;
  }

  public GraphVertex clone() {
    return new PositionalEncodingVertex(_sequenceLength, _vectorLength);
  }

  @Override
  public SDVariable defineVertex(SameDiff sameDiff, VertexInputs inputs) {
    INDArray pe = Nd4j.zeros(_vectorLength, _sequenceLength);
    for (long i = 0; i < (_vectorLength + 1) / 2; i++) {
      double multiplier = Math.pow(10000, -2 * ((double) i) / _vectorLength);
      for (long t = 0; t < _sequenceLength; t++) {
        double inner = t * multiplier;
        pe.putScalar(2 * i, t, Math.sin(inner));
        if (2 * i + 1 < _vectorLength) {
          pe.putScalar(2 * i + 1, t, Math.cos(inner));
        }
      }
    }

    return inputs.getInput(0).add(sameDiff.constant(pe));
  }

  @Override
  public Pair<INDArray, MaskState> feedForwardMaskArrays(INDArray[] maskArrays, MaskState currentMaskState,
      int minibatchSize) {
    return new Pair<>(maskArrays[0], currentMaskState); // just pass through
  }
}
