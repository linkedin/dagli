package com.linkedin.dagli.nn.result;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;


/**
 * Gets a {@link DenseVector} output from an {@link NNResult}.
 */
@ValueEquality
class VectorFromNNResult extends AbstractPreparedTransformer1WithInput<NNResult, DenseVector, VectorFromNNResult> {
  private static final long serialVersionUID = 1;

  private final int _outputIndex;

  /**
   * Creates a new instance that gets the output from a {@link NNResult} corresponding to the given index.
   *
   * @param outputIndex the index of the ouput to be retrieved
   */
  VectorFromNNResult(int outputIndex) {
    _outputIndex = outputIndex;
  }

  @Override
  public DenseVector apply(NNResult nnResult) {
    return nnResult.getAsVector(_outputIndex);
  }
}
