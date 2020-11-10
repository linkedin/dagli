package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.math.vector.DenseVector;


/**
 * Base class for NN transformations that operate on one or more input vectors, such as pooling operations.
 *
 * @param <R> the canonical type of result produced by this layer
 * @param <S> the type of the class ultimately deriving from this one
 */
abstract class AbstractVariadicVectorLayer<R, S extends AbstractVariadicVectorLayer<R, S>>
    extends AbstractVariadicTransformerLayer<DenseVector, R, S> {
  private static final long serialVersionUID = 1;
}
