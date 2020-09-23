package com.linkedin.dagli.preparer;

import com.linkedin.dagli.annotation.struct.Struct;
import com.linkedin.dagli.dag.DAGExecutor;
import java.io.Serializable;


/**
 * A {@link PreparerContext} captures information about the environment in which a preparer is running and the data
 * it will be fed.  This is often useful for, e.g. preallocating space.
 *
 * Clients of Dagli will typically never need to create a {@link PreparerContext} themselves, although they will consume
 * it when implementing {@link com.linkedin.dagli.transformer.PreparableTransformer}s.
 */
@Struct("PreparerContext")
abstract class PreparerContextBase implements Serializable {
  private static final long serialVersionUID = 1L;

  private static final long DEFAULT_ESTIMATED_EXAMPLE_COUNT = 16;

  /**
   * The <i>estimated</i> number of examples in this execution; estimates may be arbitrarily inaccurate but
   * should not grossly exceed the true number of examples (i.e. this value will be suitable for pre-allocating
   * space in data structures).
   */
  long _estimatedExampleCount;

  /**
   * A lower bound on the number of examples in this execution, or {@code 0} if no better lower bound is known.
   */
  long _exampleCountLowerBound;

  /**
   * An upper bound on the number of examples in this execution, or {@link Long#MAX_VALUE} if no upper bound
   * can be established (e.g. an indefinitely long stream of exmaples).
   */
  long _exampleCountUpperBound;

  /**
   * The DAGExecutor that is being used to prepare the DAG.  This may be useful if, e.g. a {@link Preparer} wants to use
   * this same executor to prepare another DAG internally.
   */
  DAGExecutor _executor;

  /**
   * Convenience method that creates a partially-built builder for the common case where an exact example count is
   * known.
   *
   * @param exactExampleCount the exact example count
   * @return a partially-built {@link PreparerContext} builder
   */
  public static PreparerContext.Helper.Executor.Builder builder(long exactExampleCount) {
    return PreparerContext.Builder
        .setEstimatedExampleCount(exactExampleCount)
        .setExampleCountLowerBound(exactExampleCount)
        .setExampleCountUpperBound(exactExampleCount);
  }

  /**
   * @return true iff the number of examples used in this execution is known exactly
   */
  public boolean hasExactExampleCount() {
    return _exampleCountLowerBound == _exampleCountUpperBound;
  }

  /**
   * @return the <i>estimated</i> number of examples in this execution; estimates may be arbitrarily inaccurate but
   *         should not grossly exceed the true number of examples (i.e. this value will be suitable for pre-allocating
   *         space in data structures)
   */
  public long getEstimatedExampleCount() {
    return _estimatedExampleCount;
  }

  /**
   * @return an upper bound on the number of examples in this execution, or {@link Long#MAX_VALUE} if no upper bound
   *         can be established (e.g. an indefinitely long stream of exmaples)
   */
  public long getExampleCountUpperBound() {
    return _exampleCountUpperBound;
  }

  /**
   * @return a lower bound on the number of examples in this execution, or {@code 0} if no better lower bound is known
   */
  public long getExampleCountLowerBound() {
    return _exampleCountLowerBound;
  }

  /**
   * @return a copy of the {@link PreparerContext} that will have a small, default estimated example count (or the
   *         estimate will match getExampleCountUpperBound(), if that is lower)
   */
  public PreparerContext withDefaultEstimatedExampleCount() {
    return ((PreparerContext) this).withEstimatedExampleCount(
        Math.min(_exampleCountUpperBound, DEFAULT_ESTIMATED_EXAMPLE_COUNT));
  }

  /**
   * The DAGExecutor that is being used to prepare the DAG.  This may be useful if, e.g. a {@link Preparer} wants to use
   * this same executor to prepare another DAG internally.
   *
   * @return the executor being used to prepare the DAG
   */
  public DAGExecutor getExecutor() {
    return _executor;
  }
}
