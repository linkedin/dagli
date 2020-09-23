package com.linkedin.dagli.transformer;

import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.internal.PreparedTransformerVariadicInternalAPI;
import com.linkedin.dagli.util.collection.UnmodifiableArrayList;
import java.util.List;

/**
 * Base class for prepared transformers with variadic arity.
 *
 * Transformer base classes (like this one) provide a standard implementation for the corresponding transformer
 * interfaces; their use is highly recommended.
 *
 * @param <R> the type of value produced by this transformer
 * @param <S> the ultimate derived type of the prepared transformer extending this class
 */
public abstract class AbstractPreparedTransformerVariadic<V, R, S extends AbstractPreparedTransformerVariadic<V, R, S>>
    extends AbstractTransformerVariadic<V, R, PreparedTransformerVariadicInternalAPI<V, R, S>, S>
    implements PreparedTransformerVariadic<V, R> {

  private static final long serialVersionUID = 1;

  /**
   * Creates a new prepared transformer with the specified inputs.
   *
   * @param inputs the inputs to use
   */
  @SafeVarargs
  public AbstractPreparedTransformerVariadic(Producer<? extends V>... inputs) {
    super(inputs);
  }

  /**
   * Creates a new variadic transformer with the specified inputs
   *
   * @param inputs the inputs for the transformer
   */
  public AbstractPreparedTransformerVariadic(List<? extends Producer<? extends V>> inputs) {
    super(inputs);
  }

  @Override
  protected PreparedTransformerVariadicInternalAPI<V, R, S> createInternalAPI() {
    return new InternalAPI();
  }

  protected class InternalAPI
      extends AbstractTransformerVariadic<V, R, PreparedTransformerVariadicInternalAPI<V, R, S>, S>.InternalAPI
      implements PreparedTransformerVariadicInternalAPI<V, R, S> {
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
    @SuppressWarnings("unchecked")
    public R applyUnsafe(Object executionCache, Object[] values) {
      assert values.length >= getArity();
      return AbstractPreparedTransformerVariadic.this.apply(new UnmodifiableArrayList<>((V[]) values, getArity()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public R applyUnsafe(Object executionCache, List<?> values) {
      assert values.size() >= getArity();
      return AbstractPreparedTransformerVariadic.this.apply(
          (List<V>) (values.size() == getArity() ? values : values.subList(0, getArity())));
    }

    @Override
    @SuppressWarnings("unchecked")
    public R applyUnsafe(Object executionCache, Object[][] values, int exampleIndex) {
      assert values.length >= getArity();
      return AbstractPreparedTransformerVariadic.this.apply(
          new UnmodifiableExampleInputList<>((V[][]) values, exampleIndex, getArity()));
    }

    @Override
    public List<? extends Producer<?>> getInputList() {
      return AbstractPreparedTransformerVariadic.this.getInputList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public S withInputsUnsafe(List<? extends Producer<?>> newInputs) {
      return (S) AbstractPreparedTransformerVariadic.this.withInputs((List) newInputs);
    }
  }
}
