package com.linkedin.dagli.dag;

/**
 * DAG executors prepare and apply DAGs.  Note that certain DAG executors (e.g. {@link FastPreparedDAGExecutor} may not
 * support DAG preparation (training), only application (inference).
 *
 * A {@link PreparedDAGExecutor} may only be used to apply prepared DAGs (e.g. {@link DAG1x1.Prepared}, not prepare
 * preparable DAGs like {@link DAG1x1}.
 *
 * To use an executor, it must be attached to a DAG via the DAG's {@code withExecutor(...)} method, e.g.
 * {@link DAG1x1#withExecutor(DAGExecutor)}.  The resulting DAG may then be prepared or applied and the provided
 * executor will be used.
 */
public interface PreparedDAGExecutor {
  /**
   * @return the internal API for this {@link PreparedDAGExecutor}; should not be used by client code
   */
  AbstractDAGExecutor<?> internalAPI();
}
