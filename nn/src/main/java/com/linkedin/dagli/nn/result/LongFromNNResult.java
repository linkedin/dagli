package com.linkedin.dagli.nn.result;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;


/**
 * Gets a {@link Long} as the output from an {@link NNResult}.
 */
@ValueEquality
class LongFromNNResult extends AbstractPreparedTransformer1WithInput<NNResult, Long, LongFromNNResult> {
  private static final long serialVersionUID = 1;

  private final int _outputIndex;

  /**
   * Creates a new instance that gets the output from a {@link NNResult} corresponding to the given index.
   *
   * @param outputIndex the index of the ouput to be retrieved
   */
  LongFromNNResult(int outputIndex) {
    _outputIndex = outputIndex;
  }

  @Override
  public Long apply(NNResult nnResult) {
    return nnResult.getAsMDArray(_outputIndex).getAsLongUnsafe(0); // every MDArray has at least one element
  }
}
