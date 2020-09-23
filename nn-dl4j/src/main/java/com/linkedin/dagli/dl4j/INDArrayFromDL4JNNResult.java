package com.linkedin.dagli.dl4j;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import org.nd4j.linalg.api.ndarray.INDArray;


/**
 * Gets a {@link INDArray} as an output from an {@link DL4JResult}.
 */
@ValueEquality
class INDArrayFromDL4JNNResult
    extends AbstractPreparedTransformer1WithInput<DL4JResult, INDArray, INDArrayFromDL4JNNResult> {
  private static final long serialVersionUID = 1;

  private final int _outputIndex;

  /**
   * Creates a new instance that gets the output from a {@link DL4JResult} corresponding to the given index.
   *
   * @param outputIndex the index of the ouput to be retrieved
   */
  INDArrayFromDL4JNNResult(int outputIndex) {
    _outputIndex = outputIndex;
  }

  @Override
  public INDArray apply(DL4JResult nnResult) {
    return nnResult.getAsINDArray(_outputIndex);
  }
}
