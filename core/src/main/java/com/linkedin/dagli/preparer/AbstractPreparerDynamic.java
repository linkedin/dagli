package com.linkedin.dagli.preparer;

import com.linkedin.dagli.transformer.PreparedTransformer;

/**
 * Base class for preparers for dynamic-arity transformers.
 *
 * @param <R> the type of value produced by the transformer
 * @param <N> the type of the resultant prepared transformer
 */
public abstract class AbstractPreparerDynamic<R, N extends PreparedTransformer<R>> extends AbstractPreparer<R, N>
    implements PreparerDynamic<R, N> { }
