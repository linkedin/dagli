package com.linkedin.dagli.transformer;

import com.linkedin.dagli.transformer.internal.TransformerDynamicInternalAPI;


/**
 * Interface for a {@link Transformer} with arbitrary, dynamically-determined arguments.
 *
 * @param <R> the type of the result
 */
public interface TransformerDynamic<R> extends Transformer<R>, TransformerWithInputBound<Object, R> {
  @Override
  TransformerDynamicInternalAPI<R, ? extends TransformerDynamic<R>> internalAPI();
}
