package com.linkedin.dagli.dl4j;

import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.transformer.DynamicInputs;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;


/**
 * Represents a vector used as an input.
 */
class VectorInputConverter extends AbstractInputConverter<Vector, VectorInputConverter> {
  private static final long serialVersionUID = 1;

  public VectorInputConverter(DynamicInputs.Accessor<? extends Vector> inputAccessor, long length,
      DataType dataType) {
    super(inputAccessor, new long[] { length }, null, dataType);
  }

  @Override
  public void writeValueToINDArrays(Vector vector, INDArray array, INDArray mask, int exampleIndex) {
    long offset = exampleIndex * _exampleSubarrayShape[0];
    DL4JUtil.copyVectorToINDArrayUnsafe(vector, _exampleSubarrayShape[0], array, offset);
  }
}
