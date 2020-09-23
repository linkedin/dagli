package com.linkedin.dagli.nn.result;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.mdarray.MDArray;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Gets a {@link java.util.List} of {@link DenseVector}s as an output from an {@link NNResult}.
 *
 * For variable-length sequences, there's presently no way to determine the end-of-sequence, so the sequence length is
 * taken as fixed.  Although technically implementation-dependent, "extra" vectors beyond the actual end-of-sequence are
 * likely to be 0-vectors (no non-zero elements).
 */
@ValueEquality
class VectorSequenceFromNNResult extends
    AbstractPreparedTransformer1WithInput<NNResult, List<DenseVector>, VectorSequenceFromNNResult> {
  private static final long serialVersionUID = 1;

  private final int _outputIndex;

  /**
   * Creates a new instance that gets the output from a {@link NNResult} corresponding to the given index.
   *
   * @param outputIndex the index of the ouput to be retrieved
   */
  VectorSequenceFromNNResult(int outputIndex) {
    _outputIndex = outputIndex;
  }

  @Override
  public List<DenseVector> apply(NNResult nnResult) {
    MDArray mdArray = nnResult.getAsMDArray(_outputIndex);
    return IntStream.range(0, Math.toIntExact(mdArray.shape()[0]))
        .mapToObj(i -> mdArray.subarrayAt(i).asVector())
        .collect(Collectors.toList());
  }
}
