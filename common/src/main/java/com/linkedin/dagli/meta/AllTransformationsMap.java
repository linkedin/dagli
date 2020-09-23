package com.linkedin.dagli.meta;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformerDynamic;
import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.util.collection.Iterables;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.List;
import java.util.Map;


/**
 * Applies each transformer in a key-to-transformer map provided as its first input to the remaining inputs.
 * The result of these transformers is then returned as a map, with results indexed by the corresponding transformer's
 * key in the original map.
 *
 * This isn't something clients would normally want to do explicitly (hence why this class is not public).  However, it
 * is instead useful in conjunction with {@link com.linkedin.dagli.view.TransformerView}s, which can extract a map of
 * transformers from, e.g. a {@link PreparedByGroup} instance and then use this class to apply them all to given inputs.
 *
 * @param <R> the type of result returned by each of the list of transformers
 */
@ValueEquality
class AllTransformationsMap<R> extends AbstractPreparedTransformerDynamic<Map<Object, R>, AllTransformationsMap<R>> {
  // TODO: this method could be made more efficient by extended AbstractPreparedStatefulTransformerDynamic;
  // may be made moot with graph optimization, however
  private static final long serialVersionUID = 1;

  /**
   * Creates a new instance.
   *
   * @param transformersInput the input that will provide a list of transformers to be run against the other inputs
   * @param valueInputs inputs providing the values that will be fed to each of the transformers
   */
  AllTransformationsMap(Producer<? extends Map<Object, ? extends PreparedTransformer<? extends R>>> transformersInput,
      List<? extends Producer<?>> valueInputs) {
    super(Iterables.prepend(valueInputs, transformersInput));
  }

  @Override
  protected Map<Object, R> apply(List<?> values) {
    // the first input is always the transformer map
    Map<Object, ? extends PreparedTransformer<? extends R>> transformers =
        (Map<Object, ? extends PreparedTransformer<? extends R>>) values.get(0);

    // the values to pass to those transformers are the rest of our inputs
    Object[] passedValues = values.subList(1, values.size()).toArray();

    // results is an Object[] array containing elements of type R
    Object[] results = transformers.values()
        .stream()
        .map(preparedTransformer -> preparedTransformer.internalAPI()
            .applyUnsafe(preparedTransformer.internalAPI().createExecutionCache(1), passedValues))
        .toArray();

    // we use an Object2ObjectArrayMap because it's *very* cheap to construct (once you have the key & value arrays)
    return new Object2ObjectArrayMap<>(transformers.keySet().toArray(), results);
  }
}
