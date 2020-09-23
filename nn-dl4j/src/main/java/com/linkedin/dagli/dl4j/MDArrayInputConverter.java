package com.linkedin.dagli.dl4j;

import com.linkedin.dagli.math.mdarray.MDArray;
import com.linkedin.dagli.transformer.DynamicInputs;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;


/**
 * Copies the provided per-example {@link MDArray}s into a minibatch.
 */
class MDArrayInputConverter
    extends AbstractInputConverter<MDArray, MDArrayInputConverter> {
  private static final long serialVersionUID = 1;

  public MDArrayInputConverter(DynamicInputs.Accessor<? extends MDArray> inputAccessor,
      long[] shape, DataType dataType) {
    super(inputAccessor, shape.clone(), null, dataType);
  }

  @Override
  public void writeValueToINDArrays(MDArray sourceArray, INDArray targetArray, INDArray mask, int exampleIndex) {
    DL4JUtil.copyMDArrayToINDArrayUnsafe(sourceArray, 0, targetArray, exampleIndex * _exampleSubarrayElementCount,
        _exampleSubarrayElementCount);
  }
}
