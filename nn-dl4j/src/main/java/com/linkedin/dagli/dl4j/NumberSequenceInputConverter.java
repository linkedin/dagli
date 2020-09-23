package com.linkedin.dagli.dl4j;

import com.linkedin.dagli.transformer.DynamicInputs;
import com.linkedin.dagli.util.closeable.Closeables;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;


/**
 * Represents a sequence of numbers used as an input.  Regardless of data type, numbers are subject to an intermediate
 * conversion to double which may result in a loss of precision for long values.
 */
class NumberSequenceInputConverter
    extends AbstractInputConverter<Iterable<? extends Number>, NumberSequenceInputConverter> {
  private static final long serialVersionUID = 1;

  public NumberSequenceInputConverter(DynamicInputs.Accessor<? extends Iterable<? extends Number>> inputAccessor,
      long maxSequenceLength, DataType dataType) {
    super(inputAccessor, new long[] { maxSequenceLength }, new long[] { maxSequenceLength }, dataType);
  }

  @Override
  public void writeValueToINDArrays(Iterable<? extends Number> sequence, INDArray array, INDArray mask,
      int exampleIndex) {
    long offset = exampleIndex * _exampleSubarrayElementCount;
    final long offsetAfterLast = offset + _exampleSubarrayElementCount;

    java.util.Iterator<? extends Number> iterator = sequence.iterator();

    while (offset < offsetAfterLast && iterator.hasNext()) {
      array.putScalarUnsafe(offset, iterator.next().doubleValue());
      mask.putScalarUnsafe(offset, 1.0);

      offset++;
    }

    Closeables.tryClose(iterator);
  }
}
