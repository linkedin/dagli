package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.math.vector.DenseVector;
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

}
