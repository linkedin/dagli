package com.linkedin.dagli.transformer;

import com.linkedin.dagli.dag.SimpleDAGExecutor;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResultMixed;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.internal.PreparableTransformerVariadicInternalAPI;
import com.linkedin.dagli.util.collection.Iterables;
import java.util.List;


/**
 * Interface for a {@link PreparableTransformer} with a variable number of arguments.
 *
 * @param <V> the type of the arguments
 * @param <R> the type of the result
 */
public interface PreparableTransformerVariadic<V, R, N extends PreparedTransformerVariadic<V, R>>
    extends TransformerVariadic<V, R>, PreparableTransformer<R, N> {

  @Override
  PreparableTransformerVariadicInternalAPI<V, R, N, ? extends PreparableTransformerVariadic<V, R, N>> internalAPI();

  /**
   * Creates a copy of this transformer that uses the specified inputs.
   *
   * @param inputs list of the inputs the new transformer will use
   * @return a copy of this transformer that uses the specified inputs
   */
  PreparableTransformerVariadic<V, R, N> withInputs(List<? extends Producer<? extends V>> inputs);

  /**
   * Prepares a preparable transformer and returns the result (which includes the prepared transformer for both the
   * "preparation" data (in this case, the values passed to this method) and "new" data.
   *
   * @param preparable the transformer to prepare
   * @param inputValues the data used to prepare the transformer
   * @param <V> the type of input values consumed by the transformer
   * @param <R> the type of result produced by the transformer
   * @param <N> the type of the to-be-prepared transformer (for new data)
   * @return the prepared tranformers
   */
  @SafeVarargs
  static <V, R, N extends PreparedTransformerVariadic<V, R>>
  PreparerResultMixed<PreparedTransformerVariadic<V, R>, N> prepare(
      PreparableTransformerVariadic<V, R, N> preparable, Iterable<? extends V>... inputValues) {
    PreparerContext context =
        PreparerContext.builder(Iterables.size64(inputValues[0])).setExecutor(new SimpleDAGExecutor()).build();

    return preparable.internalAPI().prepare(context, inputValues);
  }
}
