package com.linkedin.dagli.transformer;

import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.internal.PreparedTransformerDynamicInternalAPI;
import com.linkedin.dagli.util.collection.UnmodifiableArrayList;
import java.util.List;

/**
 * Base class for prepared transformers with dynamic arity.
 *
 * Transformer base classes (like this one) provide a standard implementation for the corresponding transformer
 * interfaces; their use is highly recommended.
 *
 * @param <R> the type of value produced by this transformer
 * @param <S> the ultimate derived type of the prepared transformer extending this class
 */
public abstract class AbstractPreparedTransformerDynamic<R, S extends AbstractPreparedTransformerDynamic<R, S>>
    extends AbstractTransformerDynamic<R, PreparedTransformerDynamicInternalAPI<R, S>, S>
    implements PreparedTransformerDynamic<R> {

  private static final long serialVersionUID = 1;

  /**
   * Creates a new prepared transformer with the specified inputs.
   *
   * @param inputs the inputs for the transformer
   */
  public AbstractPreparedTransformerDynamic(Producer<?>... inputs) {
    super(inputs);
  }

  /**
   * Creates a new prepared transformer with the specified inputs.
   *
   @param inputs the inputs for the transformer
   */
  public AbstractPreparedTransformerDynamic(List<? extends Producer<?>> inputs) {
    super(inputs);
  }

  /**
   * Applies the transformer to the provided input values.
   *
   * This method is assumed to be thread-safe; i.e. it may safely be called concurrently on the same instance.
   *
   * <strong>The implementing method must not allow the values list to globally escape (i.e. stored after the method
   * returns.)</strong>
   * This is because, for efficiency, the underlying data structure behind the list may be reused on subsequent method
   * calls.  If you were to store a reference to the list, its value may change later, creating unpleasant logic bugs in
   * your code.
   *
   * @param values the inputs to the transformer
   * @return the result of applying this transformer to the supplied values
   */
  protected abstract R apply(List<?> values);

  @Override
  protected PreparedTransformerDynamicInternalAPI<R, S> createInternalAPI() {
    return new InternalAPI();
  }

  protected class InternalAPI
      extends AbstractTransformerDynamic<R, PreparedTransformerDynamicInternalAPI<R, S>, S>.InternalAPI
      implements PreparedTransformerDynamicInternalAPI<R, S> {
    private int _arity = 0;

    int getArity() {
      if (_arity == 0) {
        _arity = getInputList().size();
        if (_arity == 0) {
          throw new IllegalStateException("Attempting operation with invalid transformer that has no parents");
        }
      }

      return _arity;
    }

    @Override
    public R applyUnsafe(Object executionCache, Object[] values) {
      return AbstractPreparedTransformerDynamic.this.apply(new UnmodifiableArrayList<>(values, getArity()));
    }

    @Override
    public R applyUnsafe(Object executionCache, List<?> values) {
      return AbstractPreparedTransformerDynamic.this.apply(
          values.size() == getArity() ? values : values.subList(0, getArity()));
    }

    @Override
    public R applyUnsafe(Object executionCache, Object[][] values, int exampleIndex) {
      return AbstractPreparedTransformerDynamic.this.apply(
          new UnmodifiableExampleInputList<>(values, exampleIndex, getArity()));
    }

    @Override
    public List<? extends Producer<?>> getInputList() {
      return AbstractPreparedTransformerDynamic.this.getInputList();
    }

    @Override
    public S withInputsUnsafe(List<? extends Producer<?>> newInputs) {
      return AbstractPreparedTransformerDynamic.this.withInputsUnsafe(newInputs);
    }
  }
}
