package com.linkedin.dagli.transformer;

import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.internal.TransformerVariadicInternalAPI;
import java.util.List;


/**
 * Interface for a {@link Transformer} with a variable number of arguments.
 *
 * @param <V> the type of the arguments
 * @param <R> the type of the result
 */
public interface TransformerVariadic<V, R> extends Transformer<R>, TransformerWithInputBound<V, R> {

  @Override
  TransformerVariadicInternalAPI<V, R, ? extends TransformerVariadic<V, R>> internalAPI();

  /**
   * Creates a copy of this transformer that uses the specified inputs.
   *
   * The returned instance <strong>must</strong> be a new instance, as Dagli may rely on this invariant.
   *
   * @param inputs list of the inputs the new transformer will use
   * @return a copy of this transformer that uses the specified inputs
   */
  TransformerVariadic<V, R> withInputs(List<? extends Producer<? extends V>> inputs);
}
