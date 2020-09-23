package com.linkedin.dagli.preparer;

import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.transformer.PreparedTransformerVariadic;
import java.util.Arrays;
import java.util.List;

/**
 * Base interface for preparers of variadic-arity transformers.
 *
 * @param <R> the type of value produced by the transformer.
 * @param <N> the type of the resultant prepared transformer.
 */
public interface PreparerVariadic<V, R, N extends PreparedTransformerVariadic<V, R>> extends Preparer<R, N> {
  @Override
  default void processUnsafe(Object[] values) {
    // we don't own values, so we create a clone to prevent values from escaping
    process((List<V>) Arrays.asList(values.clone()));
  }

  /**
   * Processes a single example of preparation data.  To prepare a {@link Preparer}, process(...) will be called on
   * each and every preparation example before finish(...) is called to complete preparation.
   *
   * This method is not assumed to be thread-safe and will not be invoked concurrently on the same {@link Preparer}.
   *
   * @param values The input values.  The Preparer must <b>not</b> modify this list.
   */
  void process(List<V> values);

  @Override
  default PreparerResultMixed<? extends PreparedTransformerVariadic<? super V, ? extends R>, N> finishUnsafe(
      ObjectReader<Object[]> inputs) {
    return finish(inputs == null ? null : inputs.lazyMap(arr -> (List<V>) Arrays.asList(arr)));
  }

  /**
   * Finish preparation.
   *
   * @param inputs the preparation data, as previously seen by {@link PreparerVariadic#process(List)}
   * @return a {@link PreparerResultMixed} containing the resulting prepared transformers
   */
  PreparerResultMixed<? extends PreparedTransformerVariadic<? super V, ? extends R>, N> finish(
      ObjectReader<List<V>> inputs);
}

