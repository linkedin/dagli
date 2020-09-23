package com.linkedin.dagli.dl4j;

import com.linkedin.dagli.transformer.DynamicInputs;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;


/**
 * Represents a number used as an input.
 */
class NumberInputConverter extends AbstractInputConverter<Number, NumberInputConverter> {
  private static final long serialVersionUID = 1;

  public NumberInputConverter(DynamicInputs.Accessor<? extends Number> inputAccessor, DataType dataType) {
    super(inputAccessor, new long[] { 1 }, null, dataType);
  }

  @Override
  public void writeValueToINDArrays(Number value, INDArray array, INDArray mask, int exampleIndex) {
    // this may result in a loss of precision for longs, but at present there is no "putScalar" for longs
    array.putScalarUnsafe(exampleIndex, value.doubleValue());
  }
}
