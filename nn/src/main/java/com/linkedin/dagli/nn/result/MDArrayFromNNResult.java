package com.linkedin.dagli.nn.result;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.mdarray.MDArray;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;


/**
 * Gets a {@link MDArray} output from an {@link NNResult}.
 */
@ValueEquality
class MDArrayFromNNResult extends AbstractPreparedTransformer1WithInput<NNResult, MDArray, MDArrayFromNNResult> {
  private static final long serialVersionUID = 1;

  private final int _outputIndex;

  /**
   * Creates a new instance that gets the output from a {@link NNResult} corresponding to the given index.
   *
   * @param outputIndex the index of the ouput to be retrieved
   */
  MDArrayFromNNResult(int outputIndex) {
    _outputIndex = outputIndex;
  }

  @Override
  public MDArray apply(NNResult nnResult) {
    return nnResult.getAsMDArray(_outputIndex);
  }
}
