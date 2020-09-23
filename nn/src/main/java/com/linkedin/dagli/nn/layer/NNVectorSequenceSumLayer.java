package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.visitor.VisitedBy;


/**
 * Layer that outputs the sum of the the vector sequences provided as inputs.
 *
 * Each vector in the output sequence is the sum of the vectors at the corresponding timestep in the inputs.
 *
 * The dimensions (sequence length and length) of the output is the same as the dimensions of each of the inputs.
 *
 * <strong>Known limitation:</strong> at present, the dimensions of the input sequences (sequence length and the length
 * of each vector in the sequence) must be the same.
 */
@VisitedBy(value = "NNLayerVisitor")
public class NNVectorSequenceSumLayer extends AbstractElementWiseSequenceLayer<NNVectorSequenceSumLayer>
    implements NonTerminalLayer {
  private static final long serialVersionUID = 1;

  @Override
  public <R> R accept(NNLayerVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
