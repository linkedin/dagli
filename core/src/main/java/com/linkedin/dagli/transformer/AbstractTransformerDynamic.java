package com.linkedin.dagli.transformer;

import com.linkedin.dagli.annotation.equality.IgnoredByValueEquality;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.internal.TransformerDynamicInternalAPI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Base class for dynamic-arity transformers.  Note that derived transformer implementations should not extend this
 * class directly.  Instead, extend the base class corresponding to the type (prepared/preparable) of the transformer
 * you're creating, e.g. {@link AbstractPreparedTransformerDynamic}.
 *
 * @param <R> the type of value produced by this transformer
 * @param <I> the type of the internal API object used by this transformer
 * @param <S> the ultimate derived type of the transformer extending this class
 */
@IgnoredByValueEquality
abstract class AbstractTransformerDynamic<R, I extends TransformerDynamicInternalAPI<R, S>, S extends AbstractTransformerDynamic<R, I, S>>
    extends AbstractTransformer<R, I, S> implements TransformerDynamic<R> {

  private static final long serialVersionUID = 1;

  protected List<Producer<?>> _inputs;

  /**
   * Creates a new transformer with no inputs.
   */
  public AbstractTransformerDynamic() {
    this(Collections.emptyList());
  }

  /**
   * Creates a new transformer with the specified inputs.
   *
   * @param inputs the inputs for the transformer
   */
  public AbstractTransformerDynamic(Producer<?>... inputs) {
    this(Arrays.asList(inputs));
  }

  /**
   * Creates a new transformer with the specified inputs.
   *
   * @param inputs the inputs for the transformer
   */
  public AbstractTransformerDynamic(List<? extends Producer<?>> inputs) {
    _inputs = new ArrayList<>(inputs);
  }

  /**
   * Gets a list of all the inputs to this transformer.
   *
   * @return a list of the transformer's inputs
   */
  @Override
  protected List<? extends Producer<?>> getInputList() {
    return _inputs;
  }

  /**
   * Creates a new transformer that uses the specified inputs but is otherwise a copy of this one.
   *
   * The returned instance <strong>must</strong> be a new instance, as Dagli may rely on this invariant.
   *
   * It is "unsafe" because the inputs provided are not (necessarily) type-checked, even at runtime, which may result
   * in logic bugs.
   *
   * @param newInputs the new inputs that will be used by the copy of the transformer
   * @return a copy of the transformer that uses the specified inputs
   */
  protected S withInputsUnsafe(List<? extends Producer<?>> newInputs) {
    return clone(r -> r._inputs = new ArrayList<>(newInputs));
  }

  protected abstract class InternalAPI extends AbstractTransformer<R, I, S>.InternalAPI
      implements TransformerDynamicInternalAPI<R, S> { }
}
