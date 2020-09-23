package com.linkedin.dagli.transformer;

import com.linkedin.dagli.annotation.equality.IgnoredByValueEquality;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.internal.TransformerVariadicInternalAPI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Base class for variadic-arity transformers.  Note that derived transformer implementations should not extend this
 * class directly.  Instead, extend the base class corresponding to the type (prepared/preparable) of the transformer
 * you're creating, e.g. {@link AbstractPreparedTransformerVariadic}.
 *
 * @param <V> the type of input consumed by this transformer
 * @param <R> the type of value produced by this transformer
 * @param <I> the type of the internal API object used by this transformer
 * @param <S> the ultimate derived type of the transformer extending this class
 */
@IgnoredByValueEquality
abstract class AbstractTransformerVariadic<V,
                                           R,
                                           I extends TransformerVariadicInternalAPI<V, R, S>,
                                           S extends AbstractTransformerVariadic<V, R, I, S>>
    extends AbstractTransformer<R, I, S>
    implements TransformerVariadic<V, R> {

  private static final long serialVersionUID = 1;

  protected List<Producer<? extends V>> _inputs;

  @Override
  protected List<Producer<? extends V>> getInputList() {
    return _inputs;
  }

  /**
   * Creates a new variadic transformer with the specified inputs
   *
   * @param inputs the inputs for the transformer
   */
  @SafeVarargs
  public AbstractTransformerVariadic(Producer<? extends V>... inputs) {
    this(Arrays.asList(inputs));
  }

  /**
   * Creates a new variadic transformer with the specified inputs
   *
   * @param inputs the inputs for the transformer
   */
  public AbstractTransformerVariadic(List<? extends Producer<? extends V>> inputs) {
    _inputs = new ArrayList<>(inputs);
  }

  @Override
  public S withInputs(List<? extends Producer<? extends V>> inputs) {
    return clone(c -> c._inputs = new ArrayList<>(inputs));
  }

  /**
   * Creates a new transformer that uses the specified inputs but is otherwise a copy of this one.
   *
   * @param inputs the new inputs that will be used by the copy of the transformer
   * @return a copy of the transformer that uses the specified inputs
   */
  @SafeVarargs
  public final S withInputs(Producer<? extends V>... inputs) {
    return withInputs(Arrays.asList(inputs));
  }

  protected abstract class InternalAPI extends AbstractTransformer<R, I, S>.InternalAPI
      implements TransformerVariadicInternalAPI<V, R, S> { }
}