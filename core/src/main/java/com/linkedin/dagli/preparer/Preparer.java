package com.linkedin.dagli.preparer;

import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.transformer.PreparedTransformer;


/**
 * Preparers prepare {@link PreparedTransformer}s by consuming input data with their process() method and then returning
 * two resultant prepared transformers (one for the preparation data, one for new data) from their finish() method.
 * These two prepared transformers are often the same, but will be different when the data used in preparation must
 * be handled differently than new data, e.g. to avoid overfitting.  They are returned within as a
 * {@link PreparerResultMixed} object.
 *
 * Note to implementors: you should implement one of the Batch/StreamPreparerX (where X is the number of inputs your
 * transformer consumes) interfaces, not this one.  Better yet, you can extend the corresponding abstract classes.
 *
 * @param <R> the type of result the prepared transformers will return
 * @param <N> the type of the prepared transformer that will be returned for use with new data;
 *            note that a preparer may return a different type of transformer for use with preparation/training data,
 *            but this need not be the same type.
 */
public interface Preparer<R, N extends PreparedTransformer<? extends R>> {
  /**
   * Gets the {@link PreparerMode} that determines how this preparer will be prepared, specifically, what values will be
   * passed to the {@link Preparer#finishUnsafe(ObjectReader)} method.
   *
   * The mode of two different instances of a given {@link Preparer} class may be different.  However, there are
   * restrictions (even though these will be naturally adhered to by any reasonable implementation anyway):
   * (1) The mode <b>must not change</b> over the lifetime of a {@link Preparer} instance (that is, if an instance
   *     reports its mode as {@link PreparerMode#STREAM}, it cannot, e.g. change its mode to {@link PreparerMode#BATCH}
   *     later!)
   * (2) Two preparers created from the same transformer (i.e. via
   *     {@link com.linkedin.dagli.transformer.internal.PreparableTransformerInternalAPI#getPreparer(PreparerContext)})
   *     must use the same mode.
   *
   * @return the mode determining how this preparer will be prepared
   */
  PreparerMode getMode();

  /**
   * Dagli-internal method.  Not intended for use by client code.
   *
   * Processes the provided input values.  Note that the values[] array may be reused and modified afterwards by the
   * caller.  Because of this you must not allow values[] to "escape" this method (e.g. by storing a reference to it),
   * although of course you may store a clone of the array, etc.
   *
   * This method is not assumed to be thread-safe and will not be invoked concurrently on the same Preparer.
   *
   * Unsafe because passed values are not necessarily type-checked, which may result in logic bugs.
   * @param values The input values.  The Preparer must NOT take ownership of this array; the caller may reuse it.
   *
   */
  void processUnsafe(Object[] values);

  /**
   * Dagli-internal method.  Not intended for use by client code.
   *
   * Completes preparation of the resultant transformers and returns them.
   *
   * The actual value passed to this method depends on the mode of the preparer, as returned by
   * {@link Preparer#getMode()}:
   * If the mode is {@link PreparerMode#BATCH}, the inputs parameter is a ObjectReader with all the input values that
   * were previously streamed to this Preparer via processUnsafe().  This allows the preparer to make additional passes
   * over the data as needed.
   *
   * If the mode is {@link PreparerMode#STREAM}, the inputs parameter will instead be null and the preparer will not be
   * able to make additional passes over the data.
   *
   * It's important to note that, for StreamPreparers, the inputs arguments may be null!  For other Preparers, inputs is
   * a convenient (and efficient) way to make additional passes over the data without having to cache it yourself.
   *
   * @return the prepared transformers
   * @param inputs ObjectReader containing arrays of input values.  For certain subtypes of
   *               Preparer (i.e. StreamPreparer) this argument may be null.
   */
  PreparerResultMixed<? extends PreparedTransformer<? extends R>, N> finishUnsafe(
      ObjectReader<Object[]> inputs);
}
