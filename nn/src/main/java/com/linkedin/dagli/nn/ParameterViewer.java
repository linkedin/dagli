package com.linkedin.dagli.nn;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.mdarray.MDArray;
import com.linkedin.dagli.transformer.PreparableTransformer;
import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.view.AbstractTransformerView;
import java.util.Map;


/**
 * Viewer that retrieves parameters from a transformer implementing {@link ParameterStore}.
 */
@ValueEquality
public class ParameterViewer<K, N extends PreparedTransformer<?> & ParameterStore<K>>
    extends AbstractTransformerView<Map<String, ? extends MDArray>, N, ParameterViewer<K, N>> {
  private static final long serialVersionUID = 1;

  private final K _key;

  // no-arg constructor for Kryo
  private ParameterViewer() {
    this(null, null);
  }

  /**
   * Creates a new view of the specified transformer.
   *
   * @param viewedTransformer the viewed transformer
   * @param key the key corresponding to the paramater set to be retrieved
   */
  public ParameterViewer(PreparableTransformer<?, ? extends N> viewedTransformer,
      K key) {
    super(viewedTransformer);
    _key = key;
  }

  @Override
  protected Map<String, ? extends MDArray> prepare(N preparedTransformerForNewData) {
    return  preparedTransformerForNewData.getParameters(_key);
  }
}
