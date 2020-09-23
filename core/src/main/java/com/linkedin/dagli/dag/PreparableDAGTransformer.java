package com.linkedin.dagli.dag;

import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.PreparableTransformer;
import com.linkedin.dagli.transformer.internal.PreparableTransformerInternalAPI;
import java.util.List;


/**
 * Subtype for Preparable DAGs
 *
 * @param <R> the type of result of the DAG
 * @param <S> the type of the DAG
 */
public interface PreparableDAGTransformer<R, N extends PreparedDAGTransformer<R, N>, S extends PreparableDAGTransformer<R, N, S> & PreparableTransformer<R, N>>
    extends DAGTransformer<R, S>, PreparableTransformer<R, N> {
  interface InternalAPI<R, N extends PreparedDAGTransformer<R, N>, S extends PreparableDAGTransformer<R, N, S> & PreparableTransformer<R, N>>
      extends DAGTransformer.InternalAPI<R, S>, PreparableTransformerInternalAPI<R, N, S> {
    @Override
    DAGExecutor getDAGExecutor();

    /**
     * Returns a copy of this DAG that has the same properties (other than the DAG structure itself) as another DAG.
     *
     * @param other the other DAG's whose properties should be copied
     * @return a copy of this DAG that has the same properties (other than the DAG structure itself) as another DAG
     */
    default S withSameProperties(PreparableDAGTransformer<?, ?, ?> other) {
      return getInstance()
          .withExecutor(other.internalAPI().getDAGExecutor())
          .withReduction(other.internalAPI().getReductionLevel());
    }

    /**
     * Given a list of placeholders and outputs, returns the prepared DAG corresponding to this preparable DAG.
     *
     * @param placeholders the placeholders of the prepared DAG
     * @param outputs the outputs of the prepared dAG
     * @return a prepared DAG of the appropriate type (given that this DAG was its progenitor)
     */
    @SuppressWarnings("unchecked")
    default PreparedDAGTransformer<R, ?> createPreparedDAG(List<? extends Placeholder<?>> placeholders,
        List<? extends Producer<?>> outputs) {
      if (getInstance() instanceof DynamicDAG) {
        return (PreparedDAGTransformer<R, ?>) new DynamicDAG.Prepared<>((DynamicDAG<?>) getInstance(), placeholders,
            outputs);
      } else {
        return DAGUtil.createPreparedDAG(placeholders, outputs);
      }
    }
  }

  InternalAPI<R, N, S> internalAPI();

  /**
   * Returns a copy of this DAG that will use the given {@link DAGExecutor} that will be used to execute this DAG.
   *
   * @param executor the {@link DAGExecutor} to use
   * @return a copy of this instance that will use the provided executor
   */
  S withExecutor(DAGExecutor executor);
}
