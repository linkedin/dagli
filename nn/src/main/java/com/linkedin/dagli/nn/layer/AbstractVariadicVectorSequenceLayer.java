package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.util.array.ArraysEx;
import java.util.List;


/**
 * Base class for NN transformations that operate on one or more input vector sequences, such as concatenations.
 *
 * @param <R> the canonical type of result produced by this layer
 * @param <S> the type of the class ultimately deriving from this one
 */
abstract class AbstractVariadicVectorSequenceLayer<R, S extends AbstractVariadicVectorSequenceLayer<R, S>>
    extends AbstractVariadicTransformerLayer<List<DenseVector>, R, S> {
  private static final long serialVersionUID = 1;

  /**
   * Returns a copy of this layer that will accept as <strong>additional</strong> inputs the provided sequences of
   * vectors; these vectors should be dense, although they do not necessarily need to be of type {@link DenseVector}.
   *
   * @param inputs {@link Producer}s that will provide sequences of vectors serving as an additional inputs to this
   *              layer
   * @return a copy of this layer that will accept the provided inputs
   */
  @SafeVarargs
  public final S withAdditionalInputsFromVectorSequences(Producer<? extends Iterable<? extends Vector>>... inputs) {
    return withAdditionalInputs(ArraysEx.mapArray(inputs, NNVectorSequenceInputLayer[]::new,
        input -> new NNVectorSequenceInputLayer().withInput(input)));
  }

  @SafeVarargs
  @Override
  public final S withAdditionalInputs(NNLayer<List<DenseVector>, ? extends NonTerminalLayer>... layers) {
    return super.withAdditionalInputs(layers);
  }
}
