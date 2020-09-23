package com.linkedin.dagli.transformer;

import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.internal.PreparedTransformerVariadicInternalAPI;
import com.linkedin.dagli.util.collection.FixedCapacityArrayList;
import com.linkedin.dagli.util.collection.Lists;
import com.linkedin.dagli.util.collection.UnmodifiableArrayList;
import java.util.ArrayList;
import java.util.List;


/**
 * Base class for prepared variadic transformers that extends {@link AbstractPreparedTransformerVariadic} to
 * allow for limited "state" across examples for greater computational efficiency.  Note that, as with all
 * {@link Producer}s, transformers deriving from this class are themselves immutable.
 *
 * Such "state" takes two forms, of which either or both may be used: the use of {@link #createExecutionCache(long)} to
 * create an "execution cache" object that will then be passed to the {@code apply(...)} method, and the
 * {@code applyAll(...)} method that transforms a number of "minibatched" examples together.
 *
 * Transformer base classes (like this one) provide a standard implementation for the corresponding transformer
 * interfaces; their use is strongly recommended over implementing the interfaces directly.
 *
 * @param <R> the type of value produced by this transformer
 * @param <Q> the type of the execution cache object used by this instance; if an execution cache object is not used
 *            (i.e. {@link #createExecutionCache(long)} always returns null) the {@link Void} type should be specified
 * @param <S> the ultimate derived type of the prepared transformer extending this class
 */
public abstract class AbstractPreparedStatefulTransformerVariadic<V, R, Q, S extends AbstractPreparedStatefulTransformerVariadic<V, R, Q, S>>
    extends AbstractPreparedTransformerVariadic<V, R, S> {
  private static final long serialVersionUID = 1;

  /**
   * Creates a new prepared transformer with the specified inputs.
   *
   * @param inputs the inputs to use
   */
  @SafeVarargs
  public AbstractPreparedStatefulTransformerVariadic(Producer<? extends V>... inputs) {
    super(inputs);
  }

  /**
   * Creates a new variadic transformer with the specified inputs
   *
   * @param inputs the inputs for the transformer
   */
  public AbstractPreparedStatefulTransformerVariadic(List<? extends Producer<? extends V>> inputs) {
    super(inputs);
  }

  @Override
  protected PreparedTransformerVariadicInternalAPI<V, R, S> createInternalAPI() {
    return new InternalAPI();
  }

  class InternalAPI extends AbstractPreparedTransformerVariadic<V, R, S>.InternalAPI {
    @Override
    @SuppressWarnings("unchecked")
    public R applyUnsafe(Object executionCache, Object[] values) {
      assert values.length >= getArity();
      return AbstractPreparedStatefulTransformerVariadic.this.apply((Q) executionCache,
          new UnmodifiableArrayList<>((V[]) values, getArity()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public R applyUnsafe(Object executionCache, List<?> values) {
      assert values.size() >= getArity();
      return AbstractPreparedStatefulTransformerVariadic.this.apply((Q) executionCache,
          (List<V>) (values.size() == getArity() ? values : values.subList(0, getArity())));
    }

    @Override
    @SuppressWarnings("unchecked")
    public R applyUnsafe(Object executionCache, Object[][] values, int exampleIndex) {
      assert values.length >= getArity();
      return AbstractPreparedStatefulTransformerVariadic.this.apply((Q) executionCache,
          new UnmodifiableExampleInputList<>((V[][]) values, exampleIndex, getArity()));
    }

    @Override
    public Q createExecutionCache(long exampleCountGuess) {
      return AbstractPreparedStatefulTransformerVariadic.this.createExecutionCache(exampleCountGuess);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void applyAllUnsafe(Object executionCache, int count, Object[][] values, Object[] results) {
      int arity = getArity();
      ArrayList<List<V>> hyperlist = new ArrayList<>(arity);

      for (int i = 0; i < arity; i++) {
        hyperlist.add(new UnmodifiableArrayList<>((V[]) values[i], count));
      }

      AbstractPreparedStatefulTransformerVariadic.this.applyAll((Q) executionCache, hyperlist,
          new FixedCapacityArrayList<>(results));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void applyAllUnsafe(Object executionCache, int count, List<? extends List<?>> values, List<? super R> results) {
      AbstractPreparedStatefulTransformerVariadic.this.applyAll((Q) executionCache,
          (List) Lists.limit(values, getArity(), count), results);
    }

    @Override
    public int getPreferredMinibatchSize() {
      return AbstractPreparedStatefulTransformerVariadic.this.getPreferredMinibatchSize();
    }
  }

  @Override
  public R apply(List<? extends V> values) {
    return apply(createExecutionCache(1), values);
  }

  /**
   * Transforms the given list of values.
   *
   * The provided values list should neither be modified nor stored (after the method returns): the backing data
   * structure may be reused by Dagli causing its elements to arbitrarily change.
   *
   * @param executionCache an execution cache object (or null), as returned by {@link #createExecutionCache(long)}
   * @param values the list of input values; should neither be modified nor stored
   * @return the result of the transformation
   */
  protected abstract R apply(Q executionCache, List<? extends V> values);

  /**
   * Applies the transformer to all provided examples.  Input values are provided as a list of lists of objects, where
   * {@code values.get(i)} corresponds to all the values for input #i for all examples.  For example, the 3rd input
   * value for the 100th example is given by {@code values.get(2}.get(99)}.  <strong>Note that {@code values.get(i} does
   * not correspond to a list of all the inputs for example #i.</strong>
   *
   * The number of examples passed can be any amount greater than 0; {@link #getPreferredMinibatchSize()} acts as a
   * "hint" to the DAG executor but will not necessarily be honored.
   *
   * This method must be thread-safe, able to be invoked concurrently on the same instance.
   *
   * Additionally, the lists contained in {@code values} must not be modified, and neither they nor the {@code results}
   * list should they be stored past the end of the method call since their underlying data structures may be reused
   * and modified by Dagli after this call is finished.
   *
   * Finally, the ordering of the examples passed to this method may differ from their original ordering (as supplied
   * to the encapsulating DAG).
   *
   * By default, this implementation transforms the provided examples via repeated calls to the {@code apply(...)}
   * method.  Transformers that benefit from minibatching of their examples should override this method.
   *
   * @param executionCache a transitory object used for sharing temporary data across invocations of this transformer,
   *                       as returned by {@link #createExecutionCache(long)}; may be null
   * @param values a list of input value lists corresponding to the examples being transformed; contained lists must not
   *               be modified or stored past the end of the method call
   * @param results a list to which should be added the results of applying this transformer to the given examples, in
   *                the same order the examples were provided; must not be stored past the end of the method call
   */
  protected void applyAll(Q executionCache, List<? extends List<? extends V>> values, List<? super R> results) {
    assert values.size() == getInputList().size();

    int exampleCount = values.get(0).size();
    ArrayList<V> exampleValueList = new ArrayList<>(values.size());

    for (int i = 0; i < exampleCount; i++) {
      exampleValueList.clear();
      for (List<? extends V> inputValueList : values) {
        exampleValueList.add(inputValueList.get(i));
      }
      results.add(apply(executionCache, exampleValueList));
    }
  }

  /**
   * Creates an execution cache object for this transformer (or returns null).
   *
   * The returned instance will be passed to subsequent {@code apply(...)} and {@code applyAll(...)} calls.  A guess of
   * the number of examples that will be encountered using this execution ache object is provided; transformers that do
   * not benefit from caching for this number of examples should return null.
   *
   * Execution cache objects may be concurrently used by multiple threads.  Aside from this, they do not need to meet
   * any other criteria (such as implementing {@link java.io.Serializable}.)  If the object implements
   * {@link AutoCloseable} it <strong>may</strong> be closed when it is no longer required by the DAG executor; however,
   * the correctness of an implementation should not depend on {@link AutoCloseable#close()} being called at a
   * particular time, or ever.
   *
   * Multiple execution cache objects may be created and used during a single DAG execution; correctness must therefore
   * not depend on, e.g. accruing state within the execution cache object.  Additionally, execution caches for the same
   * (reference-equals) transformer may potentially be shared across multiple DAG executions.
   *
   * The default implementation of this method simply returns null.
   *
   * @param exampleCountGuess the best available guess of how many examples will be processed using the returned
   *                          execution cache, or {@link Long#MAX_VALUE} if the potential number is unbounded
   * @return an execution cache instance to be passed to subsequent calls to the "apply" methods, or null
   */
  protected Q createExecutionCache(long exampleCountGuess) {
    return null;
  }

  /**
   * Gets the preferred minibatch size when using {@code applyAll(...)}.  The DAG executor is free to ignore this
   * preference and may provide much larger or smaller minibatches.
   *
   * The default implementation returns a value of 1, indicating that this transformer is indifferent to minibatching;
   * derived classes that benefit from minibatching should override this method to return a more appropriate value.
   *
   * @return the minibatch size preferred by the transformer
   */
  protected int getPreferredMinibatchSize() {
    return 1;
  }
}
