package com.linkedin.dagli.transformer.internal;

import com.linkedin.dagli.objectio.ConcatenatedReader;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResultMixed;
import com.linkedin.dagli.preparer.PreparerVariadic;
import com.linkedin.dagli.transformer.PreparableTransformerVariadic;
import com.linkedin.dagli.transformer.PreparedTransformerVariadic;
import com.linkedin.dagli.util.array.ArraysEx;
import com.linkedin.dagli.util.closeable.Closeables;
import java.util.Iterator;
import java.util.List;

/**
 * Base interface for the internal API of preparable transformers with variadic arity.
 *
 * @param <V> the type of input accepted by the transformer
 * @param <R> the type of value produced by the transformer
 * @param <N> the type of prepared transformer obtained by preparing the preparable transformer
 * @param <S> the ultimate derived type of the preparable transformer
 */
public interface PreparableTransformerVariadicInternalAPI<V, R, N extends PreparedTransformerVariadic<V, R>, S extends PreparableTransformerVariadic<V, R, N>>
  extends TransformerVariadicInternalAPI<V, R, S>, PreparableTransformerInternalAPI<R, N, S> {

  PreparerVariadic<V, R, N> getPreparer(PreparerContext context);

  /**
   * Convenience method that prepares this transformer using the provided inputs.
   *
   * @param context the context in which the preparation occurs
   * @param valueIterables the input data
   * @return the prepared transformers
   */
  @SuppressWarnings("unchecked")
  default PreparerResultMixed<PreparedTransformerVariadic<V, R>, N> prepare(
      PreparerContext context, Iterable<? extends V>[] valueIterables) {
    PreparerVariadic<V, R, N> preparer = getPreparer(context);

    Iterator<? extends V>[] valueIterators = ArraysEx.mapArray(valueIterables, Iterator[]::new, Iterable::iterator);

    while (valueIterators[0].hasNext()) {
      // compiler may complain if Iterator::next is used rather than the lambda below:
      preparer.process((List<V>) java.util.Arrays.asList(ArraysEx.mapArray(valueIterators, Object[]::new, iter -> iter.next())));
    }

    for (Iterator<? extends V> valueIterator : valueIterators) {
      Closeables.tryClose(valueIterator);
    }

    return (PreparerResultMixed<PreparedTransformerVariadic<V, R>, N>) preparer.finishUnsafe(new ConcatenatedReader<>(Object[]::new,
        ArraysEx.mapArray(valueIterables, ObjectReader[]::new, ObjectReader::wrap)));
  }
}
