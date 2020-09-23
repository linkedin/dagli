package com.linkedin.dagli.dag;

import com.linkedin.dagli.annotation.Versioned;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.util.cloneable.AbstractCloneable;


/**
 * DAG executors prepare and apply DAGs.  Note that certain DAG executors (e.g. {@link FastPreparedDAGExecutor} may not
 * support DAG preparation (training), only application (inference).
 *
 * @param <S> the type of the derived DAGExecutor
 */
@Versioned
abstract class AbstractDAGExecutor<S extends AbstractDAGExecutor<S>> extends AbstractCloneable<S>
    implements PreparedDAGExecutor {
  private static final long serialVersionUID = 1L;

  @Override
  @SuppressWarnings("unchecked") // S is the derived type of this base class
  public S internalAPI() {
    return (S) this;
  }

  @Override
  public abstract int hashCode(); // force subclasses to override

  @Override
  public abstract boolean equals(Object obj); // force subclasses to override

  protected abstract <R, N extends PreparedDAGTransformer<R, N>, T extends PreparableDAGTransformer<R, N, T>>
  DAGExecutionResult<R, N> prepareAndApplyUnsafeImpl(T dag, ObjectReader<Object>[] inputValueLists);

  protected <R, N extends PreparedDAGTransformer<R, N>, T extends PreparableDAGTransformer<R, N, T>>
  PreparerResult<N> prepareUnsafeImpl(T dag, ObjectReader<Object>[] inputValueLists) {
    try (DAGExecutionResult<R, N> res = prepareAndApplyUnsafeImpl(dag, inputValueLists)) {
      return res.getPreparerResult();
    }
  }

  protected abstract <R, T extends PreparedDAGTransformer<R, T>> ObjectReader<?>[] applyUnsafeImpl(T dag,
      ObjectReader<Object>[] inputValueLists);

  // gets a new PreparerResult containing prepared DAGs with the same properties as the DAG they were prepared from
  private static <R, N extends PreparedDAGTransformer<R, N>, T extends PreparableDAGTransformer<R, N, T>>
  PreparerResult<N> mapPreparerResult(T dag, PreparerResult<N> result) {
    return result.map(prepared -> prepared.internalAPI().withSameProperties(dag));
  }

  /**
   * Prepares the DAG and applies the prepared DAG to the input values.
   *
   * @param dag the DAG to prepare and apply
   * @param inputValueLists an array of ObjectReaders, one for each of the DAG's placeholders
   * @param <R> the type of result produced by the DAG
   * @return a {@link DAGExecutionResult} containing both the prepared DAG and the results of applying that DAG on the
   *         inputValueLists
   */
  final <R, N extends PreparedDAGTransformer<R, N>, T extends PreparableDAGTransformer<R, N, T>>
  DAGExecutionResult<R, N> prepareAndApplyUnsafe(T dag, ObjectReader<Object>[] inputValueLists) {
    DAGExecutionResult<R, N> result = prepareAndApplyUnsafeImpl(dag, inputValueLists);
    return new DAGExecutionResult<>(mapPreparerResult(dag, result.getPreparerResult()), result.getOutputs());
  }

  /**
   * Applies a prepared DAG to the input values, returning an array of ObjectReaders, one for each of the outputs of the
   * DAG.
   *
   * @param dag the DAG to apply
   * @param inputValueLists an array of ObjectReaders, one for each of the DAG's placeholders
   * @param <R> the type of result returned by the DAG
   * @return an array of ObjectReaders containing the outputs, one per each of the DAG's outputs
   */
  final <R, T extends PreparedDAGTransformer<R, T>> ObjectReader<?>[] applyUnsafe(T dag,
      ObjectReader<Object>[] inputValueLists) {
    return applyUnsafeImpl(dag, inputValueLists);
  }

  /**
   * Prepares the DAG and returns the prepared DAG.  Unlike prepareAndApplyUnsafe(...), the executor is not obliged
   * to return the results of applying the prepared DAG to the inputs, which speeds execution.  Use this method when
   * only the prepared model (and not the application of the model to the inputs) is required.
   *
   * @param dag the DAG to prepare
   * @param inputValueLists an array of ObjectReaders, one for each of the DAG's placeholders
   * @param <R> the type of result returned by the DAG
   * @return the PreparerResult containing the prepared DAGs for the training data and for new data
   */
  @SuppressWarnings("unchecked") // preparing to type N is guaranteed by the API spec
  final <R, N extends PreparedDAGTransformer<R, N>, T extends PreparableDAGTransformer<R, N, T>>
  PreparerResult<N> prepareUnsafe(T dag, ObjectReader<Object>[] inputValueLists) {
    if (dag.internalAPI().getDAGStructure()._isPrepared) {
      return new PreparerResult<>(
          (N) DAGMakerUtil.makePreparedDAGTransformer(dag.internalAPI().getDAGStructure())
              .internalAPI()
              .withSameProperties(dag));
    }
    return mapPreparerResult(dag, prepareUnsafeImpl(dag, inputValueLists));
  }
}
