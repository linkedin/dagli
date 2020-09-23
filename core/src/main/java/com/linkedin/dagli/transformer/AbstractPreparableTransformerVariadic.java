package com.linkedin.dagli.transformer;

import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerVariadic;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.internal.PreparableTransformerVariadicInternalAPI;
import java.util.List;

/**
 * Base class for preparable transformers with variadic arity.
 *
 * Transformer base classes (like this one) provide a standard implementation for the corresponding transformer
 * interfaces; their use is highly recommended.
 *
 * @param <V> the type of input accepted by this transformer
 * @param <R> the type of value produced by this transformer
 * @param <N> the type of prepared transformer that will result from preparing this (preparable) transformer
 * @param <S> the ultimate derived type of the preparable transformer extending this class
 */
public abstract class AbstractPreparableTransformerVariadic<
    V,
    R,
    N extends PreparedTransformerVariadic<V, R>,
    S extends AbstractPreparableTransformerVariadic<V, R, N, S>>
  extends AbstractTransformerVariadic<V, R, PreparableTransformerVariadicInternalAPI<V, R, N, S>, S>
  implements PreparableTransformerVariadic<V, R, N> {

  private static final long serialVersionUID = 1;

  /**
   * Creates a new variadic transformer with the specified inputs
   *
   * @param inputs the inputs for the transformer
   */
  @SafeVarargs
  public AbstractPreparableTransformerVariadic(Producer<? extends V>... inputs) {
    super(inputs);
  }

  /**
   * Creates a new variadic transformer with the specified inputs
   *
   * @param inputs the inputs for the transformer
   */
  public AbstractPreparableTransformerVariadic(List<? extends Producer<? extends V>> inputs) {
    super(inputs);
  }

  /**
   * Gets a {@link com.linkedin.dagli.preparer.Preparer} that may be fed preparation data to obtained a
   * {@link PreparedTransformer}.
   *
   * @param context information about the preparation data and the environment in which the preparer is executing
   * @return a new {@link com.linkedin.dagli.preparer.Preparer} that will be use to prepare a corresponding
   * {@link PreparedTransformer}.
   */
  protected abstract PreparerVariadic<V, R, N> getPreparer(PreparerContext context);

  @Override
  protected PreparableTransformerVariadicInternalAPI<V, R, N, S> createInternalAPI() {
    return new InternalAPI();
  }

  /**
   * Returns whether the preparer returned by {@link #getPreparer(PreparerContext)} is idempotent to identical
   * inputs; i.e. preparing the transformer with a sequence of distinct examples results in the same prepared
   * transformer as preparing with duplicate examples included.
   *
   * <strong>Idempotent does not imply commutative</strong>: an idempotent preparer may still be affected by the
   * <i>order</i> of the (de-duplicated) inputs, e.g. whether the first value A is seen before or after the first value
   * B is allowed to change the result.
   *
   * For example, the {@code Max} transformer calculates the maximum value of all its inputs, and duplicated inputs will
   * not affect the result--it is thus idempotent-preparable.  In contrast, a hypothetical {@code Count} transformer
   * that simply counts the number of examples would <strong>not</strong> be idempotent, as the total number of examples
   * determines the final prepared value (a non-idempotent-preparable transformer may still be constant-result: our
   * {@code Count} transformer would be constant-result since it would output the same total count for each example).
   *
   * The default implementation returns false.
   *
   * <strong>The determination of idempotency must be made independently of this transformer's parents in the DAG.
   * </strong>  More concretely, replacing the parents of this transformer with arbitrary (valid) substitutes should not
   * affect the returned value.  If this is impossible, this method should return false.
   *
   * In those rare cases where the prepared transformers "for new data" and "for preparation data" are different,
   * <strong>both</strong> must be idempotent to duplicated examples if this method returns true.
   *
   * The benefit of idempotency is that it allows for optimizations when reducing and executing the DAG that may result
   * in substantial improvements to execution speed.
   *
   * @return true if the transformer's preparer is idempotent to duplicated examples, false otherwise
   */
  protected boolean hasIdempotentPreparer() {
    return false;
  }

  protected class InternalAPI
      extends AbstractTransformerVariadic<V, R, PreparableTransformerVariadicInternalAPI<V, R, N, S>, S>.InternalAPI
      implements PreparableTransformerVariadicInternalAPI<V, R, N, S> {
    @Override
    public PreparerVariadic<V, R, N> getPreparer(PreparerContext context) {
      return AbstractPreparableTransformerVariadic.this.getPreparer(context);
    }

    @Override
    public boolean hasIdempotentPreparer() {
      return AbstractPreparableTransformerVariadic.this.hasIdempotentPreparer();
    }

    @Override
    @SuppressWarnings("unchecked") // it's called "unsafe" for a reason
    public S withInputsUnsafe(List<? extends Producer<?>> newInputs) {
      return (S) AbstractPreparableTransformerVariadic.this.withInputs((List) newInputs);
    }

    @Override
    public List<? extends Producer<?>> getInputList() {
      return AbstractPreparableTransformerVariadic.this.getInputList();
    }
  }
}