package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.visitor.VisitedBy;


/**
 * Layer that calculates the Hadamard (element-wise) product of the vectors provided by input layers.  The size of
 * the input layers do not need to be the same--the size of the output will be the smallest of any of the input layer
 * sizes.
 */
@VisitedBy(value = "NNLayerVisitor")
public class NNVectorHadamardProductLayer extends AbstractElementWiseLayer<NNVectorHadamardProductLayer>
    implements NonTerminalLayer {
  private static final long serialVersionUID = 1;

  @Override
  boolean isOutputSizeMinInputSize() {
    return true;
  }

  @Override
  public <R> R accept(NNLayerVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
