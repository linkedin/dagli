package com.linkedin.dagli.transformer.internal;

import com.linkedin.dagli.dag.DAGExecutor;
import com.linkedin.dagli.objectio.ConcatenatedReader;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.Preparer;
import com.linkedin.dagli.preparer.PreparerResultMixed;
import com.linkedin.dagli.transformer.PreparableTransformer;
import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.util.array.ArraysEx;
import com.linkedin.dagli.util.closeable.Closeables;
import java.util.Collection;
import java.util.Iterator;


/**
 * Base interface for the internal API of preparable transformers.
 *
 * @param <R> the type of value produced by the transformer
 * @param <N> the type of prepared transformer obtained by preparing the preparable transformer
 * @param <S> the ultimate derived type of the preparable transformer
 */
public interface PreparableTransformerInternalAPI<R, N extends PreparedTransformer<? extends R>, S extends PreparableTransformer<R, N>>
    extends TransformerInternalAPI<R, S> {
  /**
   * Gets a {@link Preparer} that may be fed preparation data to obtained a {@link PreparedTransformer}.
   *
   * @param context information about the preparation data and the environment in which the preparer is executing
   * @return a new {@link Preparer} that will be use to prepare a corresponding {@link PreparedTransformer}
   */
  Preparer<R, N> getPreparer(PreparerContext context);

  /**
   * Returns whether the preparer returned by {@link #getPreparer(PreparerContext)} is idempotent to identical
   * inputs; i.e. preparing the transformer with a sequence of distinct examples results in the same prepared
   * transformer as preparing with duplicate examples included.
   *
   * <strong>Idempotent does not imply commutative</strong>: an idempotent preparer may still be affected by the
   * <i>order</i> of the (de-duplicated) inputs, e.g. whether the first value A is seen before or after the first value
   * B is allowed to change the result.
   *
   * For example, the {@code Max} transformer calculates the maximum value of all its inputs, and duplicated inputs will
   * not affect the result--it is thus idempotent-preparable.  In contrast, a hypothetical {@code Count} transformer
   * that simply counts the number of examples would <strong>not</strong> be idempotent, as the total number of examples
   * determines the final prepared value (a non-idempotent-preparable transformer may still be constant-result: our
   * {@code Count} transformer would be constant-result since it would output the same total count for each example).
   *
   * <strong>The determination of idempotency must be made independently of this transformer's parents in the DAG.
   * </strong>  More concretely, replacing the parents of this transformer with arbitrary (valid) substitutes should not
   * affect the returned value.  If this is impossible, this method should return false.
   *
   * In those rare cases where the prepared transformers "for new data" and "for preparation data" are different,
   * <strong>both</strong> must be idempotent to duplicated examples if this method returns true.
   *
   * The benefit of idempotency is that it allows for optimizations when reducing and executing the DAG that may result
   * in substantial improvements to execution speed.
   *
   * @return true if the transformer's preparer is idempotent to duplicated examples, false otherwise
   */
  boolean hasIdempotentPreparer();

  /**
   * Dagli-internal method; not intended for client code or implementations.
   * Convenience function for preparing this transformer.
   *
   * Unsafe because the types of the provided inputs are not (necessarily) type-checked, even at run-time, which may
   * result in logic bugs.
   *
   * @param context the context in which the preparation is performed
   * @param vals parallel iterators that return the inputs that will be fed to the preparer
   * @return a {@link PreparerResultMixed} containing the resultant prepared transformers
   */
  @SuppressWarnings("unchecked")
  default PreparerResultMixed<? extends PreparedTransformer<? extends R>, N> prepareUnsafe(
      PreparerContext context, Iterable<?>[] vals) {
    Preparer<R, N> preparer = getPreparer(context);

    // this cast shouldn't be necessary, but some compilers will object if we don't "convert" ? -> Object:
    Iterable<Object>[] values = (Iterable<Object>[]) vals;

    Iterator<Object>[] valueIterators = ArraysEx.mapArray(values, Iterator[]::new, Iterable::iterator);

    while (valueIterators[0].hasNext()) {
      preparer.processUnsafe(ArraysEx.mapArray(valueIterators, Object[]::new, Iterator::next));
    }

    for (Iterator<?> valueIterator : valueIterators) {
      Closeables.tryClose(valueIterator);
    }

    ObjectReader<Object>[] objectReaders =
        ArraysEx.mapArray(values, ObjectReader[]::new, ObjectReader::wrap);
    return preparer.finishUnsafe(new ConcatenatedReader<>(Object[]::new, objectReaders));
  }

  /**
   * Dagli-internal method; not intended for client code or implementations.
   * Convenience function for preparing this transformer.
   *
   * Unsafe because the types of the provided inputs are not (necessarily) type-checked, even at run-time, which may
   * result in logic bugs.
   *
   * @param executor the executor used for preparation
   * @param values parallel collections that return the inputs that will be fed to the preparer
   * @return a {@link PreparerResultMixed} containing the resultant prepared transformers
   */
  default PreparerResultMixed<? extends PreparedTransformer<? extends R>, ? extends PreparedTransformer<? extends R>>
  prepareUnsafe(DAGExecutor executor, Collection<?>[] values) {
    int size = values[0].size();
    return prepareUnsafe(PreparerContext.builder(size).setExecutor(executor).build(), values);
  }
}
