package com.linkedin.dagli.transformer;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.dag.DynamicDAG;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.preparer.AbstractPreparerDynamic;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerDynamic;
import com.linkedin.dagli.preparer.PreparerMode;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.preparer.PreparerResultMixed;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.util.invariant.Arguments;
import com.linkedin.dagli.util.collection.Iterables;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Executes a wrapped preparable transformer by mapping the values of a particular "mapped input", consuming each of the
 * elements of an inputted iterable (collection, list, etc.) and producing the result as a list.  Except for this
 * single "mapped" input, all the inputs are "inherited" from the wrapped transformer.
 *
 * If MappedIterable is provided the iterables [1, 2, 3], [4], [5, 6], the wrapped transformer simply sees the inputs
 * 1, 2, 3, 4, 5, 6, and its output is packaged into the lists [t(1), t(2), t(3)], [t(4)], [t(5), t(6)], where "t" is
 * the transformation.
 *
 * If your transformer is already prepared, use {@link MappedIterable.Prepared}.
 *
 * @param <T> the type of value accepted by the wrapped transformer's mapped input
 * @param <R> the type of the transformed value
 */
@ValueEquality
public class MappedIterable<T, R>
    extends AbstractPreparableTransformerDynamic<List<R>, MappedIterable.Prepared<T, R>, MappedIterable<T, R>> {
  private static final long serialVersionUID = 1;

  private PreparableTransformer<R, ?> _preparable = null;

  private void setPreparable(
      Function<? super Placeholder<T>, ? extends Transformer<? extends R>> preparableWithInputFunction) {
    Placeholder<T> iterablePlaceholder = new Placeholder<>("Mapped Input Placeholder");
    _preparable = PreparableTransformer.cast(
        DynamicDAG.fromMinimalInputBoundedSubgraph(preparableWithInputFunction.apply(iterablePlaceholder),
            iterablePlaceholder));
    Producer<? extends T> existingMappedInput = getMappedInput();
    _inputs = new ArrayList<>(_preparable.internalAPI().getInputList());
    _inputs.set(0, existingMappedInput);
  }

  /**
   * Returns a copy of this instance that will accept the given producer as its mapped input.  All other inputs are
   * inherited from the wrapped transformer.
   *
   * @param mappedInput the mapped input
   * @return a copy of this instance that will accept the given producer as its mapped input
   */
  public MappedIterable<T, R> withMappedInput(Producer<? extends Iterable<? extends T>> mappedInput) {
    return clone(c -> c._inputs.set(0, mappedInput));
  }

  @SuppressWarnings("unchecked")
  private Producer<? extends T> getMappedInput() {
    return (Producer<? extends T>) _inputs.get(0);
  }

  @Override
  protected boolean hasIdempotentPreparer() {
    return _preparable.internalAPI().hasIdempotentPreparer();
  }

  @Override
  protected boolean hasAlwaysConstantResult() {
    return _preparable.internalAPI().hasAlwaysConstantResult();
  }

  /**
   * Creates an instance that will obtain a (possibly preparable) transformer from the given "factory
   * function", which will almost always be a {@code withInput(...)}-type method corresponding to the input you wish to
   * map.
   *
   * Let's say we're training a multinomial {@code LiblinearClassifer}, but our data are packaged in such a way that
   * each String label is associated with a list of feature vectors, with each [label, feature vector] pair construing
   * a training example.  Then we can write something like this:
   * <pre>{@code
   *   Placeholder<String> label = new Placeholder<>();
   *   Placeholder<List<DenseVector>> featureVectors = new Placeholder<>();
   *   LiblinearClassification<String> liblinear =
   *     new LiblinearClassification<String>().withLabelInput(label);
   *   MappedIterable<String, DiscreteDistribution<String>> classification =
   *      new MappedIterable<>(liblinear::withFeaturesInput).withMappedInput(featureVectors);
   * }</pre>
   *
   * During preparation, {@code classification} will then provide a [String label, DenseVector features] pair for every
   * element in the {@code featureVectors} list, and, during inference, it will correspondingly produce a list of
   * predicted labels (one for each feature vector).
   *
   * @param preparableWithMappedInputFunction a function that obtains a (possibly preparable) transformer given a provided
   *                                    placeholder representing the value to be mapped
   */
  public MappedIterable(
      Function<? super Placeholder<T>, ? extends Transformer<? extends R>> preparableWithMappedInputFunction) {
    this();
    setPreparable(preparableWithMappedInputFunction);
  }

  /**
   * Creates a new MappedIterable.  You must specify a preparable using {@link #withTransformer(Function)} prior to
   * using this instance.
   */
  public MappedIterable() {
    super(MissingInput.get());
  }

  /**
   * Returns a copy of this instance that will obtain a (possibly preparable) transformer from the given "factory
   * function", which will almost always be a {@code withInput(...)}-type method corresponding to the input you wish to
   * map.
   *
   * Let's say we're training a multinomial {@code LiblinearClassifer}, but our data are packaged in such a way that
   * each String label is associated with a list of feature vectors, with each [label, feature vector] pair construing
   * a training example.  Then we can write something like this:
   * <pre>{@code
   *   Placeholder<String> label = new Placeholder<>();
   *   Placeholder<List<DenseVector>> featureVectors = new Placeholder<>();
   *   LiblinearClassification<String> liblinear =
   *     new LiblinearClassification<String>().withLabelInput(label);
   *   MappedIterable<String, DiscreteDistribution<String>> classification =
   *      new MappedIterable<>(liblinear::withFeaturesInput).withMappedInput(featureVectors);
   * }</pre>
   *
   * During preparation, {@code classification} will then provide a [String label, DenseVector features] pair for every
   * element in the {@code featureVectors} list, and, during inference, it will correspondingly produce a list of
   * predicted labels (one for each feature vector).
   *
   * @param preparableWithInputFunction a function that obtains a (possibly preparable) transformer given a provided
   *                                    placeholder representing the value to be mapped
   * @return a copy of this instance that will map the specified transformer
   */
  public MappedIterable<T, R> withTransformer(
      Function<? super Placeholder<T>, ? extends Transformer<? extends R>> preparableWithInputFunction) {
    return clone(c -> c.setPreparable(preparableWithInputFunction));
  }

  private static class Preparer<T, R>
      extends AbstractPreparerDynamic<List<R>, Prepared<T, R>> {
    com.linkedin.dagli.preparer.Preparer<R, ?> _preparer;

    Preparer(com.linkedin.dagli.preparer.Preparer<R, ?> preparer) {
      Arguments.inSet(preparer.getMode(), () -> "Preparer mode " + preparer.getMode() + " is unknown to MappedIterable",
          PreparerMode.BATCH, PreparerMode.STREAM);
      _preparer = preparer;
    }

    @Override
    public void processUnsafe(Object[] values) {
      Iterable<?> iterable = (Iterable<?>) values[0]; // mapped iterable input is always first
      for (Object val : iterable) {
        values[0] = val;
        _preparer.processUnsafe(values);
      }
    }

    @Override
    public PreparerResult<Prepared<T, R>> finishUnsafe(
        ObjectReader<Object[]> inputs) {

      final PreparerResultMixed<? extends PreparedTransformer<? extends R>, ? extends PreparedTransformer<? extends R>>
          prepResult;

      if (inputs != null) {
        ObjectReader<Object[]> explodedInputs =
            inputs.lazyFlatMap(inputsArray -> Iterables.map((Iterable<?>) inputsArray[0], val -> {
              Object[] res = inputsArray.clone();
              res[0] = val;
              return res;
            }));
        prepResult = _preparer.finishUnsafe(explodedInputs);
      } else {
        prepResult = _preparer.finishUnsafe(null);
      }

      return new PreparerResult.Builder<Prepared<T, R>>()
          .withTransformerForNewData(new Prepared<>(prepResult.getPreparedTransformerForNewData()))
          .withTransformerForPreparationData(new Prepared<>(prepResult.getPreparedTransformerForPreparationData()))
          .build();
    }

    @Override
    public PreparerMode getMode() {
      return _preparer.getMode(); // inherit the mode of our wrapped preparer
    }
  }

  @Override
  protected PreparerDynamic<List<R>, Prepared<T, R>> getPreparer(PreparerContext context) {
    // we have no way of knowing have many mapped examples will actualy be seen by the preparer a priori, although
    // we'll keep the existing estimate (which implies a guess that each mapped iterable input will contain ~1 item).
    context = context.withExampleCountLowerBound(0).withExampleCountUpperBound(Long.MAX_VALUE);
    return new Preparer<>(_preparable.internalAPI().getPreparer(context));
  }

  /**
   * Executes a wrapped prepared transformer by mapping the values of a particular "mapped input", consuming each of the
   * elements of an inputted iterable (collection, list, etc.) and producing the result as a list.  Except for this
   * single "mapped" input, all the inputs are "inherited" from the wrapped transformer.
   *
   * Given a mapped input of [1, 2, 3], [4], [5, 6], the wrapped transformer is provided the inputs 1, 2, 3, 4, 5, 6
   * and its output is packaged into the lists [t(1), t(2), t(3)], [t(4)], [t(5), t(6)], where "t" is the
   * transformation.
   *
   * If your transformer is preparable, use {@link MappedIterable}.
   *
   * @param <T> the type of value accepted by the wrapped transformer's mapped input
   * @param <R> the type of the transformed value
   */
  @ValueEquality
  public static class Prepared<T, R>
      extends AbstractPreparedStatefulTransformerDynamic<List<R>, Object, Prepared<T, R>> {
    private static final long serialVersionUID = 1;

    private PreparedTransformer<R> _prepared;

    private void setPrepared(
        Function<? super Placeholder<T>, ? extends PreparedTransformer<? extends R>> preparedWithInputFunction) {
      Placeholder<T> iterablePlaceholder = new Placeholder<>("Mapped Input Placeholder");
      _prepared = PreparedTransformer.<R>cast(
          DynamicDAG.Prepared.fromMinimalInputBoundedSubgraph(preparedWithInputFunction.apply(iterablePlaceholder),
              iterablePlaceholder));
      Producer<? extends T> existingMappedInput = getMappedInput();
      _inputs = new ArrayList<>(_prepared.internalAPI().getInputList());
      _inputs.set(0, existingMappedInput);
    }

    /**
     * Returns a copy of this instance that will accept the given producer as its mapped input.  All other inputs are
     * inherited from the wrapped transformer.
     *
     * @param mappedInput the mapped input
     * @return a copy of this instance that will accept the given producer as its mapped input
     */
    public Prepared<T, R> withMappedInput(Producer<? extends Iterable<? extends T>> mappedInput) {
      return clone(c -> c._inputs.set(0, mappedInput));
    }

    @SuppressWarnings("unchecked")
    private Producer<? extends T> getMappedInput() {
      return (Producer<? extends T>) _inputs.get(0);
    }

    @Override
    protected boolean hasAlwaysConstantResult() {
      return _prepared.internalAPI().hasAlwaysConstantResult();
    }

    /**
     * Creates a new instance that will wrap the provided (prepared) transformer.
     * @param prepared the transformer to be wrapped
     */
    private Prepared(PreparedTransformer<? extends R> prepared) {
      _prepared = PreparedTransformer.cast(prepared);
    }

    /**
     * Creates a new instance.  The wrapped transformer will need to be set using {@link #withTransformer(Function)}.
     */
    public Prepared() {
      super(MissingInput.get());
    }

    /**
     * Creates a new instance that will obtain a prepared transformer from the given "factory function", which
     * will almost always be a {@code withInput(...)}-type method.
     *
     * For example, given a hypothetical {@code Concatenation} transformer that concatenates its two String inputs
     * provided as {@code Concatenation::withInputA(...)} and {@code Concatenation::withInputB(...)}, and wanted to
     * concatenate the String {@code "PREFIX"} to every String in lists of Strings, we could write something like:
     * <pre>{@code
     *    Placeholder<List<String>> stringList = new Placeholder<>();
     *    Concatenation prefixedString = new Concatenation().withInputA(new Constant<>("PREFIX"));
     *    MappedIterable.Prepared<String, String> prefixedStrings =
     *      new MappedIterable.Prepared<>(prefixedString::withInputB).withMappedInput(stringList);
     * }</pre>
     *
     * @param preparedWithMappedInputFunction the prepared transformer to wrap
     * @return a copy of this instance that will wrap the specified transformer
     */
    public Prepared(
        Function<? super Placeholder<T>, ? extends PreparedTransformer<? extends R>> preparedWithMappedInputFunction) {
      this();
      setPrepared(preparedWithMappedInputFunction);
    }

    /**
     * Returns a copy of this instance that will obtain a prepared transformer from the given "factory
     * function", which will almost always be a {@code withInput(...)}-type method.
     *
     * For example, given a hypothetical {@code Concatenation} transformer that concatenates its two String inputs
     * provided as {@code Concatenation::withInputA(...)} and {@code Concatenation::withInputB(...)}, and wanted to
     * concatenate the String {@code "PREFIX"} to every String in lists of Strings, we could write something like:
     * <pre>{@code
     *    Placeholder<List<String>> stringList = new Placeholder<>();
     *    Concatenation prefixedString = new Concatenation().withInputA(new Constant<>("PREFIX"));
     *    MappedIterable.Prepared<String, String> prefixedStrings =
     *      new MappedIterable.Prepared<>(prefixedString::withInputB).withMappedInput(stringList);
     * }</pre>
     *
     * @param preparedWithMappedInputFunction the prepared transformer to wrap
     * @return a copy of this instance that will wrap the specified prepared transformer
     */
    public MappedIterable.Prepared<T, R> witPrepared(
        Function<? super Placeholder<T>, ? extends PreparedTransformer<? extends R>> preparedWithMappedInputFunction) {
      return clone(c -> c.setPrepared(preparedWithMappedInputFunction));
    }

    @Override
    protected List<R> apply(Object executionCache, List<?> values) {
      Iterable<?> iterable = (Iterable<?>) values.get(0);
      Object[] valuesArray = values.toArray();
      return Iterables.map(iterable, val -> {
        valuesArray[0] = val;
        return _prepared.internalAPI().applyUnsafe(executionCache, valuesArray);
      });
    }

    @Override
    protected Object createExecutionCache(long exampleCountGuess) {
      return _prepared.internalAPI().createExecutionCache(exampleCountGuess);
    }
  }
}
