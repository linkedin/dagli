package com.linkedin.dagli.nn.result;

import com.linkedin.dagli.math.mdarray.MDArray;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.producer.Producer;
import it.unimi.dsi.fastutil.longs.LongList;
import java.io.Serializable;
import java.util.List;


/**
 * The result of inference on a neural network comprising of an ordered list of multi-dimensional arrays corresponding
 * to the network's outputs.
 *
 * Direct access to the outputs stored within {@link NNResult} is intentionally disabled, as such access
 * would be inherently brittle and could result in logic errors.  Neural networks instead supply methods such as
 * {@code asLayerOutput(...)} to provide convenient, robust and user-friendly access to the results of inference.
 */
public abstract class NNResult implements Serializable {
  private static final long serialVersionUID = 1;

  /**
   * Static methods intended exclusively for use within Dagli.  Clients should not use these as they are subject to
   * change at any time.
   */
  public static abstract class InternalAPI {
    protected InternalAPI() { }

    public static Producer<Long> toLong(Producer<? extends NNResult> nnResultProducer, int outputIndex) {
      return new LongFromNNResult(outputIndex).withInput(nnResultProducer);
    }

    public static Producer<LongList> toLongSequence(Producer<? extends NNResult> nnResultProducer, int outputIndex) {
      return new LongSequenceFromNNResult(outputIndex).withInput(nnResultProducer);
    }

    public static Producer<DenseVector> toVector(Producer<? extends NNResult> nnResultProducer, int outputIndex) {
      return new VectorFromNNResult(outputIndex).withInput(nnResultProducer);
    }

    public static Producer<List<DenseVector>> toVectorSequence(Producer<? extends NNResult> nnResultProducer, int outputIndex) {
      return new VectorSequenceFromNNResult(outputIndex).withInput(nnResultProducer);
    }

    public static Producer<MDArray> toMDArray(Producer<? extends NNResult> nnResultProducer, int outputIndex) {
      return new MDArrayFromNNResult(outputIndex).withInput(nnResultProducer);
    }
  }

  /**
   * @param outputIndex the index of the output
   * @return the specified output expressed as an MDArray
   */
  protected abstract MDArray getAsMDArray(int outputIndex);

  /**
   * @param outputIndex the index of the output
   * @return the specified output expressed as a {@link DenseVector}; if the output has two or more dimensions, it is
   *         flattened (with a linearization consistent with {@link MDArray#asVector()})
   */
  protected DenseVector getAsVector(int outputIndex) {
    return getAsMDArray(outputIndex).asVector();
  }
}
