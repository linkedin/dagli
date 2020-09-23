package com.linkedin.dagli.dl4j;

import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.transformer.DynamicInputs;
import com.linkedin.dagli.util.closeable.Closeables;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;


/**
 * Represents a sequence of vectors used as an input.
 */
class VectorSequenceInputConverter
    extends AbstractInputConverter<Iterable<? extends Vector>, VectorSequenceInputConverter> {
  private static final long serialVersionUID = 1;

  public VectorSequenceInputConverter(DynamicInputs.Accessor<? extends Iterable<? extends Vector>> inputAccessor,
      long maxSequenceLength, long maxVectorLength, DataType dataType) {
    super(inputAccessor, new long[]{maxSequenceLength, maxVectorLength}, new long[]{maxSequenceLength}, dataType);
  }

  @Override
  public void writeValueToINDArrays(Iterable<? extends Vector> sequence, INDArray array, INDArray mask,
      int exampleIndex) {
    long offset = exampleIndex * _exampleSubarrayElementCount;

    long maskOffset = exampleIndex * _exampleMaskSubarrayElementCount;
    final long maskOffsetAfterLast = maskOffset + _exampleMaskSubarrayElementCount;

    java.util.Iterator<? extends Vector> iterator = sequence.iterator();

    while (maskOffset < maskOffsetAfterLast && iterator.hasNext()) {
      DL4JUtil.copyVectorToINDArrayUnsafe(iterator.next(), _exampleSubarrayShape[1], array, offset);
      offset += _exampleSubarrayShape[1];

      mask.putScalarUnsafe(maskOffset, 1.0);
      maskOffset++;
    }

    Closeables.tryClose(iterator);
  }
}
