package com.linkedin.dagli.dl4j;

import org.deeplearning4j.nn.api.MaskState;
import org.deeplearning4j.nn.conf.layers.samediff.SameDiffLambdaVertex;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.common.primitives.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;


public class DotProductVertex extends SameDiffLambdaVertex {
  private static final long serialVersionUID = 1;

  @Override
  public SDVariable defineVertex(SameDiff sameDiff, VertexInputs inputs) {
    return inputs.getInput(0).dot(inputs.getInput(1), 1).reshape(inputs.getInput(0).getShape()[0], 1);
  }

  @Override
  public Pair<INDArray, MaskState> feedForwardMaskArrays(INDArray[] maskArrays, MaskState currentMaskState,
      int minibatchSize) {
    return null;
  }
}
