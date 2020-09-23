package com.linkedin.dagli.preparer;

/**
 * The mode of a preparer determines how it will be prepared.
 */
public enum PreparerMode {
  /**
   * In batch mode, all the preparation data is streamed to the preparer via the
   * {@link Preparer#processUnsafe(Object[])} method.  Once this is done, all preparation data is once again passed to
   * the preparer via the {@link Preparer#finishUnsafe(com.linkedin.dagli.objectio.ObjectReader)} method for any
   * additional passes that the preparer may require.
   */
  BATCH,

  /**
   * In stream mode, the preparer sees the preparation data <b>once</b>, which it is streamed to the preparer via the
   * {@link Preparer#processUnsafe(Object[])} method.  Once this is done, the
   * {@link Preparer#finishUnsafe(com.linkedin.dagli.objectio.ObjectReader)} is called, passing a <b>null value</b>
   * instead of an {@link com.linkedin.dagli.objectio.ObjectReader} instance (a streaming preparer is not able to make
   * additional passes over the data like a batch preparer can).
   *
   * It is strongly recommended that a preparer use this mode in preference to {@link PreparerMode#BATCH} if possible
   * as it permits more efficient preparation of the graph, especially when the
   * {@link com.linkedin.dagli.dag.DAGExecutor} can avoid caching the preparation data for the additional pass(es)
   * required when in {@link PreparerMode#BATCH} mode.
   */
  STREAM
}
