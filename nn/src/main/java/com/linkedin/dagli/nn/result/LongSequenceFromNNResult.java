package com.linkedin.dagli.nn.result;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.mdarray.MDArray;
import com.linkedin.dagli.math.mdarray.MDArrays;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.util.stream.LongStream;


/**
 * Gets a {@link java.util.List} of {@link Long}s as an output from an {@link NNResult}.
 *
 * For variable-length sequences, there's presently no way to determine the end-of-sequence, so the sequence length is
 * taken as fixed.  Although technically implementation-dependent, "extra" values beyond the actual end-of-sequence are
 * likely to be 0.
 */
@ValueEquality
class LongSequenceFromNNResult extends
    AbstractPreparedTransformer1WithInput<NNResult, LongList, LongSequenceFromNNResult> {
  private static final long serialVersionUID = 1;

  private final int _outputIndex;

  /**
   * Creates a new instance that gets the output from a {@link NNResult} corresponding to the given index.
   *
   * @param outputIndex the index of the ouput to be retrieved
   */
  LongSequenceFromNNResult(int outputIndex) {
    _outputIndex = outputIndex;
  }

  @Override
  public LongList apply(NNResult nnResult) {
    MDArray mdArray = nnResult.getAsMDArray(_outputIndex);
    long size = MDArrays.elementCount(mdArray.shape());

    return new LongArrayList(LongStream.range(0, size).map(mdArray::getAsLongUnsafe).toArray());
  }
}
