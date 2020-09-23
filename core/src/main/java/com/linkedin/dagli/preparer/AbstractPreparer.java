package com.linkedin.dagli.preparer;

import com.linkedin.dagli.transformer.PreparedTransformer;


/**
 * Base class for abstract preparers.  Do not derive from this class directly; instead, derive from one of its
 * subclasses, e.g. {@link AbstractPreparerDynamic}, {@link AbstractBatchPreparer4},
 * {@link AbstractStreamPreparerVariadic}, etc.
 *
 * @param <R> the type of result produced by the prepared transformer
 * @param <N> the type of the prepared transformer
 */
abstract class AbstractPreparer<R, N extends PreparedTransformer<R>> implements Preparer<R, N> { }
