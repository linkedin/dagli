package com.linkedin.dagli.transformer;

import com.linkedin.dagli.objectio.biglist.BigListWriter;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.internal.PreparedTransformerVariadicInternalAPI;
import com.linkedin.dagli.util.collection.Iterables;
import com.linkedin.dagli.util.function.FunctionVariadic;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface for a {@link PreparedTransformer} with a variable number of arguments.
 *
 * @param <V> the type of the arguments
 * @param <R> the type of the result
 */
public interface PreparedTransformerVariadic<V, R> extends TransformerVariadic<V, R>, PreparedTransformer<R> {
  /**
   * Transforms the given list of values.
   *
   * The provided values list should neither be modified nor stored (after the method returns): the backing data
   * structure may be reused by Dagli causing its elements to arbitrarily change.
   *
   * @param values the list of input values; should neither be modified nor stored
   * @return the result of the transformation
   */
  R apply(List<? extends V> values);

  /**
   * Transforms multiple examples at once, provided as lists of lists of values.
   *
   * @param values the lists of value lists to be transformed
   * @return an {@link ObjectReader} containing the results of the transformation
   */
  default ObjectReader<R> applyAll(Iterable<? extends List<? extends V>> values) {
    long count = Iterables.size64(values);
    Object executionObject = internalAPI().createExecutionCache(count);

    int minibatchSize = (int) Math.min(count, Math.max(1024, internalAPI().getPreferredMinibatchSize()));
    ArrayList<R> resultBuffer = new ArrayList<>(minibatchSize);
    BigListWriter<R> result = new BigListWriter<>(count);

    ObjectReader.wrap(values).forEachBatch(minibatchSize, batch -> {
      internalAPI().applyAllUnsafe(executionObject, batch.size(), batch, resultBuffer);
      result.writeAll(resultBuffer);
      resultBuffer.clear();
    });

    return result.createReader();
  }

  @Override
  PreparedTransformerVariadic<V, R> withInputs(List<? extends Producer<? extends V>> inputs);

  @Override
  PreparedTransformerVariadicInternalAPI<V, R, ? extends PreparedTransformerVariadic<V, R>> internalAPI();

  /**
   * Returns a serializable function that calls this transformer with its arguments and returns its result.
   *
   * Note that, for lists of inputs, or for other prepared transformer arities, you can simply use transformer::apply.
   *
   * @return a serializable function that applies this transformer.
   */
  default FunctionVariadic.Serializable<V, R> toVariadicFunction() {
    return new PreparedTransformerVariadicFunction<V, R>(this);
  }
}
