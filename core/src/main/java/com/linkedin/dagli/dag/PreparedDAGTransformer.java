package com.linkedin.dagli.dag;

import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.transformer.internal.PreparedTransformerInternalAPI;


/**
 * Subtype for Prepared DAGs
 *
 * @param <R> the type of result of the DAG
 * @param <S> the type of the DAG
 */
public interface PreparedDAGTransformer<R, S extends PreparedDAGTransformer<R, S> & PreparedTransformer<R>>
    extends DAGTransformer<R, S>, PreparedTransformer<R> {
  interface InternalAPI<R, S extends PreparedDAGTransformer<R, S> & PreparedTransformer<R>>
      extends DAGTransformer.InternalAPI<R, S>, PreparedTransformerInternalAPI<R, S> {
    /**
     * Returns a copy of this DAG that has the same properties (other than the DAG structure itself) as another DAG.
     *
     * @param other the other DAG's whose properties should be copied
     * @return a copy of this DAG that has the same properties (other than the DAG structure itself) as another DAG
     */
    default S withSameProperties(DAGTransformer<?, ?> other) {
      return getInstance().withExecutor(other.internalAPI().getDAGExecutor())
          .withReduction(other.internalAPI().getReductionLevel());
    }
  }

  InternalAPI<R, S> internalAPI();

  /**
   * Returns a copy of this DAG that will use the given {@link DAGExecutor} that will be used to execute this DAG.
   *
   * @param executor the {@link DAGExecutor} to use
   * @return a copy of this instance that will use the provided executor
   */
  S withExecutor(PreparedDAGExecutor executor);
}
