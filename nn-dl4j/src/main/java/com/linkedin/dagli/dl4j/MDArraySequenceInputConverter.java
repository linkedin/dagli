package com.linkedin.dagli.dl4j;

import com.linkedin.dagli.math.mdarray.MDArray;
import com.linkedin.dagli.math.mdarray.MDArrays;
import com.linkedin.dagli.transformer.DynamicInputs;
import com.linkedin.dagli.util.closeable.Closeables;
import java.util.Iterator;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;


/**
 * Copies the provided per-example {@link MDArray} sequence into a minibatch.
 */
class MDArraySequenceInputConverter
    extends AbstractInputConverter<Iterable<? extends MDArray>, MDArraySequenceInputConverter> {
  private static final long serialVersionUID = 1;

  /**
   * Creates a new instance.
   *
   * @param inputAccessor the accessor to get the sequence of arrays from the raw Object[]
   * @param shapePerTimestep the shape of the array for each step in the sequence; the actual shape of the the input
   *                         array at each timestep does not need to match this, but it does need to have the same
   *                         number of elements (or more)
   * @param maxSequenceLength the maximum sequence length; any arrays beyond this limit will be ignored
   * @param dataType the data type of the minibatch
   */
  public MDArraySequenceInputConverter(DynamicInputs.Accessor<? extends Iterable<? extends MDArray>> inputAccessor,
      long maxSequenceLength, long[] shapePerTimestep, DataType dataType) {
    super(inputAccessor, MDArrays.concatenate(new long[]{maxSequenceLength}, shapePerTimestep),
        new long[]{maxSequenceLength}, dataType);
  }

  @Override
  public void writeValueToINDArrays(Iterable<? extends MDArray> sourceArraySequence, INDArray targetArray,
      INDArray mask, int exampleIndex) {
    long targetArrayOffset = exampleIndex * _exampleSubarrayElementCount;
    long maskArrayOffset = exampleIndex * _exampleMaskSubarrayElementCount;
    final long maskArrayOffsetEnd = (exampleIndex + 1) * _exampleMaskSubarrayElementCount;

    Iterator<? extends MDArray> iterator = sourceArraySequence.iterator();

    while (iterator.hasNext() && maskArrayOffset < maskArrayOffsetEnd) {
      DL4JUtil.copyMDArrayToINDArrayUnsafe(iterator.next(), 0, targetArray, targetArrayOffset,
          _exampleSubarrayElementCount);
      mask.putScalarUnsafe(maskArrayOffset, 1.0);

      targetArrayOffset += _exampleSubarrayElementCount;
      maskArrayOffset += _exampleMaskSubarrayElementCount;
    }

    Closeables.tryClose(iterator);
  }
}
