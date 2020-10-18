package com.linkedin.dagli.visualization;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.generator.ExampleIndex;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformerDynamic;
import com.linkedin.dagli.util.collection.Iterables;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongPredicate;


/**
 * Used by visualizations to capture all values from the DAG for rendering purposes.  This transformer is internal to
 * the visualization sublibrary and not accessible to client code (including derived visualizations).
 */
@ValueEquality
class ProducerToValueMap extends AbstractPreparedTransformerDynamic<Map<Producer<?>, Object>, ProducerToValueMap> {
  private static final long serialVersionUID = 1; // pro forma: in practice, the transformer will not be serialized

  private final List<Producer<?>> _producers;
  private final LongPredicate _renderExampleValuesPredicate;

  /**
   * Creates a new instance of the transformer.
   *
   * @param producers the producers whose values will be placed in the resulting map
   * @param renderExampleValuesPredicate a predicate that returns true when provided the example's index iff a map
   *                                     should be generated for that example; otherwise, the result will be null
   */
  ProducerToValueMap(List<? extends Producer<?>> producers, LongPredicate renderExampleValuesPredicate) {
    super(Iterables.append(producers, new ExampleIndex()));
    _producers = new ArrayList<>(producers);
    _renderExampleValuesPredicate = renderExampleValuesPredicate;
  }

  @Override
  protected Map<Producer<?>, Object> apply(List<?> values) {
    if (!_renderExampleValuesPredicate.test((Long) values.get(values.size() - 1))) {
      return null;
    }

    HashMap<Producer<?>, Object> result = new HashMap<>(_producers.size());
    for (int i = 0; i < _producers.size(); i++) {
      result.put(_producers.get(i), values.get(i));
    }
    return result;
  }
}
