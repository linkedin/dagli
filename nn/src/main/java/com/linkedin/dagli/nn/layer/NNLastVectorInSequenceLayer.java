package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.visitor.VisitedBy;


/**
 * Outputs the last vector from an input sequence of vectors (this is a frequently-used alternative to pooling the
 * sequence).
 *
 * The size of the output vector is the same size as the vectors in the input sequence.
 */
@VisitedBy("NNLayerVisitor")
public class NNLastVectorInSequenceLayer extends AbstractVectorSequencePoolingLayer<NNLastVectorInSequenceLayer> {
  private static final long serialVersionUID = 1;

  @Override
  public <R> R accept(NNLayerVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
