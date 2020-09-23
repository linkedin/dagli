package com.linkedin.dagli.transformer.internal;

import com.linkedin.dagli.dag.DAGExecutor;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerDynamic;
import com.linkedin.dagli.preparer.PreparerResultMixed;
import com.linkedin.dagli.transformer.PreparableTransformerDynamic;
import com.linkedin.dagli.transformer.PreparedTransformer;
import java.util.Collection;


/**
 * Base interface for the internal API of preparable transformers with dynamic arity.
 *
 * @param <R> the type of value produced by the transformer
 * @param <N> the type of prepared transformer obtained by preparing the preparable transformer
 * @param <S> the ultimate derived type of the preparable transformer
 */
public interface PreparableTransformerDynamicInternalAPI<R, N extends PreparedTransformer<R>, S extends PreparableTransformerDynamic<R, N>>
  extends TransformerDynamicInternalAPI<R, S>, PreparableTransformerInternalAPI<R, N, S> {

  PreparerDynamic<R, N> getPreparer(PreparerContext context);

  /**
   * Convenience method that prepares this transformer using the provided inputs.
   *
   * @param context the context in which the preparation occurs
   * @param iterables the input data
   * @return the prepared transformers
   */
  default PreparerResultMixed<? extends PreparedTransformer<? extends R>, N> prepareUnsafe(
      PreparerContext context, Iterable<?>[] iterables) {
    return PreparableTransformerInternalAPI.super.prepareUnsafe(context, iterables);
  }
  /**
   * Dagli-internal method; not intended for client code or implementations.
   * Convenience function for preparing this transformer.
   *
   * Unsafe because the types of the provided inputs are not (necessarily) type-checked, even at run-time, which may
   * result in logic bugs.
   *
   * @param executor the executor used for preparation
   * @param values parallel collections that return the inputs that will be fed to the preparer
   * @return a {@link PreparerResultMixed} containing the resultant prepared transformers
   */
  default PreparerResultMixed<? extends PreparedTransformer<? extends R>, N> prepareUnsafe(
      DAGExecutor executor, Collection<?>[] values) {
    return prepareUnsafe(PreparerContext.builder(values[0].size()).setExecutor(executor).build(), values);
  }
}
