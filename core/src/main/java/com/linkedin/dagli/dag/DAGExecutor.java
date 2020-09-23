package com.linkedin.dagli.dag;

/**
 * DAG executors prepare and apply DAGs.  Note that certain DAG executors (e.g. {@link FastPreparedDAGExecutor} may not
 * support DAG preparation (training), only application (inference).
 *
 * To use an executor, it must be attached to a DAG via the DAG's {@code withExecutor(...)} method, e.g.
 * {@link DAG1x1#withExecutor(DAGExecutor)}.  The resulting DAG may then be prepared or applied and the provided
 * executor will be used.
 */
public interface DAGExecutor extends PreparedDAGExecutor { }
