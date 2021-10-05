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
    // DL4J expects a shape of [vector length, sequence length]:
    super(inputAccessor, new long[]{maxVectorLength, maxSequenceLength}, new long[]{maxSequenceLength}, dataType);
  }

  @Override
  public void writeValueToINDArrays(Iterable<? extends Vector> sequence, INDArray array, INDArray mask,
      int exampleIndex) {
    long offset = exampleIndex * _exampleSubarrayElementCount;

    long maskOffset = exampleIndex * _exampleMaskSubarrayElementCount;
    final long maskOffsetAfterLast = maskOffset + _exampleMaskSubarrayElementCount;

    java.util.Iterator<? extends Vector> iterator = sequence.iterator();

    // indices in "array" are: [example index, vector index, sequence index]
    // TODO: we could dramatically speed up this logic by using a column-major ("Fortran") array format
    long timeStep = 0; // current timestep
    while (maskOffset < maskOffsetAfterLast && iterator.hasNext()) {
      final long currentTimeStep = timeStep++;

      // vector elements are copied only if their indices are in the range [0, vector length)
      iterator.next().forEach((index, value) -> {
        if (index >= 0 && index < _exampleSubarrayShape[0]) {
          array.putScalar(exampleIndex, index, currentTimeStep, value);
        }
      });

      mask.putScalarUnsafe(maskOffset, 1.0);
      maskOffset++;
    }

    Closeables.tryClose(iterator);
  }
}
