package com.linkedin.dagli.dl4j;

import org.deeplearning4j.nn.api.MaskState;
import org.deeplearning4j.nn.conf.graph.GraphVertex;
import org.deeplearning4j.nn.conf.layers.samediff.SameDiffLambdaVertex;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.common.primitives.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;


public class ReshapeMasklessVertex extends SameDiffLambdaVertex {
  private static final long serialVersionUID = 1;

  private final long[] _newShape;

  public ReshapeMasklessVertex(long[] newShape) {
    _newShape = newShape;
  }

  @Override
  public SDVariable defineVertex(SameDiff sameDiff, VertexInputs inputs) {
    return inputs.getInput(0).reshape(_newShape);
  }

  @Override
  public Pair<INDArray, MaskState> feedForwardMaskArrays(INDArray[] maskArrays, MaskState currentMaskState,
      int minibatchSize) {
    return null;
  }

  @Override
  public GraphVertex clone() {
    return new ReshapeMasklessVertex(_newShape);
  }
}
