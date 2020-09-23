package com.linkedin.dagli.transformer.internal;

import com.linkedin.dagli.transformer.PreparedTransformer;
import java.util.List;


/**
 * Base interface for the internal API of prepared transformers.
 *
 * All methods are intended for internal use by the framework and should not be used in client code.
 *
 * There is a high amount of redundancy in the {@code apply(...)} methods provided by this class; this is to allow for
 * maximally efficient execution of transformers in a variety of contexts without, e.g. being forced to instantiate new
 * arrays or lists on each call.  This helps make highly-granular transformers (that do only a small amount of work)
 * more palatable as there is less overhead per example.
 *
 * @param <R> the type of value produced by the transformer
 * @param <S> the ultimate derived type of the transformer
 */
public interface PreparedTransformerInternalAPI<R, S extends PreparedTransformer<R>> extends TransformerInternalAPI<R, S> {
  /**
   * Applies the transformer to the provided values, which must have at least as many values as expected and supported
   * by the transformer.
   *
   * This method is assumed to be thread-safe; i.e. it may safely be called concurrently on the same instance.
   *
   * <strong>the length of the {@code values} array may exceed the number of inputs to this transformer.</strong>
   * Extraneous elements should be ignored.
   *
   * <strong>The implementing method must not allow the values array to globally escape!</strong>
   * This is because, for efficiency, values arrays may be reused on subsequent method calls.  If you were to store a
   * reference to the array, its value may change later, causing unpleasant logic bugs in your code.
   *
   * @param executionCache a transitory object used for sharing temporary data across invocations of this transformer
   * @param values an array containing the inputs to the transformer (and possibly additional elements that should be
   *               ignored)
   * @return the result of applying this transformer to the supplied values
   */
  R applyUnsafe(Object executionCache, Object[] values);

  /**
   * Applies the transformer to the provided values, which should have at least as many values as those expected and
   * supported by the transformer (any remaining values should be ignored).
   *
   * This method is assumed to be thread-safe; i.e. it may safely be called concurrently on the same instance.
   *
   * <strong>The implementing method must not allow the values list to globally escape!</strong>
   * This is because, for efficiency, values lists may be reused on subsequent method calls.  If you were to store a
   * reference to the list, its value may change later, causing unpleasant logic bugs in your code.
   *
   * @param executionCache a transitory object used for sharing temporary data across invocations of this transformer
   * @param values a list containing the inputs to the transformer (and possibly additional elements that should be
   *               ignored)
   * @return the result of applying this transformer to the supplied values
   */
  R applyUnsafe(Object executionCache, List<?> values);

  /**
   * Applies the transformer to the provided values, which must have an arity expected and supported by the transformer.
   * This method is assumed to be thread-safe; i.e. it may safely be called concurrently on the same instance.
   *
   * <strong>the length of the {@code values} array may exceed the number of inputs to this transformer.</strong>
   * Extraneous elements should be ignored.
   *
   * <strong>The implementing method must not allow the values array to globally escape!</strong>
   * This is because, for efficiency, values arrays may be reused on subsequent method calls.  If you were to store a
   * reference to the array, its value may change later, causing unpleasant logic bugs in your code.
   *
   * @param executionCache a transitory object used for sharing temporary data across invocations of this transformer
   * @param values the inputs to the transformer (and possibly additional values that should be ignored)
   * @param exampleIndex the index into the {@code values} of the example to be processed; the {@code n} inputs for this
   *                     example will be located at {@code values[0][index], values[1][index]...values[n][index]}.
   * @return the result of applying this transformer to the supplied values
   */
  R applyUnsafe(Object executionCache, Object[][] values, int exampleIndex);

  /**
   * Gets an "execution cache" object that will be passed to subsequent "apply" calls.  A guess of the number of
   * examples that will be seen is provided; transformers that do not benefit from caching for this number of examples
   * should return null.
   *
   * Execution cache objects must be thread-safe as they may be concurrently used by multiple threads.  Aside from this,
   * they do not need to meet any other criteria (such as implemented {@link java.io.Serializable}.  If the object
   * implements {@link AutoCloseable} it <strong>may</strong> be closed when it is no longer required by the DAG
   * executor; however, the correctness of an implementation should not depend on {@link AutoCloseable#close()} being
   * called at a particular time, or ever.
   *
   * Multiple execution cache objects may be created and used during a single DAG execution; correctness must therefore
   * not depend on, e.g. accruing state within the execution cache object.  Additionally, execution caches for the same
   * (i.e. reference-equals) transformer within the same DAG may potentially be shared across multiple DAG executions.
   *
   * @param exampleCountGuess the best available guess of how many examples will be processed using the returned
   *                          execution cache, or {@link Long#MAX_VALUE} if the potential number is unbounded
   * @return an execution cache instance to be passed to subsequent calls to the "apply" methods, or null
   */
  default Object createExecutionCache(long exampleCountGuess) {
    return null;
  }

  /**
   * Applies the transformer to all provided examples, which must have an arity expected and supported by the
   * transformer.  Each of the n inputs for the example at index {@code i} may be accessed as {@code values[0][i],
   * values[1][i]...values[n][index]}.
   *
   * <strong>the length of the {@code values} array may exceed the number of inputs to this transformer.</strong>
   * Extraneous elements should be ignored.
   *
   * This method is assumed to be thread-safe; i.e. it may safely be called concurrently on the same instance.
   *
   * <strong>The implementing method must not allow the {@code values} or {@code results} arrays to globally escape!
   * </strong>  These arrays may be reused on subsequent method calls.  If you were to store a reference to either
   * array, its value may change later, causing unpleasant logic bugs in your code.
   *
   * @param executionCache a transitory object used for sharing temporary data across invocations of this transformer
   * @param values the examples to be processed by the transformer (with possibly additional elements that should be
   *               ignored)
   * @param count the number of examples being passed; data either passed within the {@code values} array or returned
   *              via the {@code results} array beyond this number of examples is undefined (and, in particular, does
   *              not need to be cleared by this method.)
   * @param results an array of length at least {@code count} that will store the results of applying this transformer
   *                to the supplied values
   */
  default void applyAllUnsafe(Object executionCache, int count, Object[][] values, Object[] results) {
    for (int i = 0; i < count; i++) {
      results[i] = applyUnsafe(executionCache, values, i);
    }
  }

  /**
   * Applies the transformer to all provided examples.  Input values are provided as a list of lists of objects, where
   * {@code values.get(i)} corresponds to all the values for input #i for all examples.  For example, the 3rd
   * input value for the 100th example is given by {@code values.get(2}.get(99)}.  <strong>Note that
   * {@code values.get(i} does not correspond to a list of all the inputs for example #i.</strong>
   *
   * <strong>the size of the {@code values} list may exceed the number of inputs to this transformer.</strong>
   * All extraneous elements should be ignored.
   *
   * This method is assumed to be thread-safe; i.e. it may safely be called concurrently on the same instance.
   *
   * <strong>The implementing method must not allow the {@code values} or {@code results} lists to globally escape!
   * </strong>  The data structures backing them may be reused on subsequent method calls.  If you were to store a
   * reference to either list, its value may change later, causing unpleasant logic bugs in your code.
   *
   * @param executionCache a transitory object used for sharing temporary data across invocations of this transformer
   * @param values the examples to be processed by the transformer (with possibly additional elements that should be
   *               ignored)
   * @param count the number of examples being passed; data either within the {@code values} constituent lists beyond
   *              this number of examples is undefined.
   * @param results a list of capacity at least {@code count} that will store the results of applying this transformer
   *                to the supplied values
   */
  default void applyAllUnsafe(Object executionCache, int count, List<? extends List<?>> values,
      List<? super R> results) {
    Object[] example = new Object[values.size()];

    for (int i = 0; i < count; i++) {
      for (int j = 0; j < example.length; j++) {
        example[j] = values.get(j).get(i);
      }
      results.add(applyUnsafe(executionCache, example));
    }
  }

  /**
   * Gets the preferred minibatch size to be passed to {@link #applyAllUnsafe(Object, int, Object[][], Object[])}.  The
   * DAG executor is free to ignore this preference.
   *
   * @return the minibatch size preferred by the transformer
   */
  default int getPreferredMinibatchSize() {
    return 1;
  }
}
