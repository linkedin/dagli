package com.linkedin.dagli.dl4j;

import org.deeplearning4j.nn.api.MaskState;
import org.deeplearning4j.nn.conf.graph.GraphVertex;
import org.deeplearning4j.nn.conf.layers.samediff.SameDiffLambdaVertex;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.common.primitives.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;


/**
 * DL4J vertex that reshapes its input, transforming it into a different shape of multidimensional array with the same
 * number of elements.
 */
public class ReshapeMasklessVertex extends SameDiffLambdaVertex {
  private static final long serialVersionUID = 1;

  private final long[] _newShape;

  /**
   * Default constructor for deserialization.
   */
  private ReshapeMasklessVertex() {
    _newShape = null;
  }

  /**
   * Creates a new DL4J vertex that will reshape its input to the shape provided.
   *
   * @param newShape the shape of the output; should have the same number of elements as the vertex's input
   */
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
