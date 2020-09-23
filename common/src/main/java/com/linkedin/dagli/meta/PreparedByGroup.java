package com.linkedin.dagli.meta;

import com.linkedin.dagli.annotation.equality.IgnoredByValueEquality;
import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.preparer.AbstractBatchPreparerDynamic;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.preparer.PreparerResultMixed;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparableTransformerDynamic;
import com.linkedin.dagli.transformer.AbstractPreparedStatefulTransformerDynamic;
import com.linkedin.dagli.transformer.PreparableTransformer;
import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.transformer.PreparedTransformerDynamic;
import com.linkedin.dagli.tuple.Tuple2;
import com.linkedin.dagli.util.collection.Iterables;
import com.linkedin.dagli.view.AbstractTransformerView;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Prepares a provided transformer multiple times using different subsets of the data that are determined by a "group"
 * input.  We refer to each different preparation of the wrapped transformer as a "sub-transformer".
 *
 * When applied the prepared {@link PreparedByGroup.Prepared} to new data, one of these sub-transformers is applied,
 * again according to the "group" input.
 *
 * Example: let's say that we have two groups of integers, "A" and "B", and want to find the
 * {@link com.linkedin.dagli.object.Multiplicity} for each group.  Assume our preparation data is:
 * ["A", 1], ["A", 1], ["B", 1] (so that the multiplicity of the value 1 in group "A" == 2 and the multiplicity of the
 * value 1 in group "B" == 1.  Then, if we apply our prepared PreparedByGroup transformer to ["A", 1] we get the
 * multiplicity 2, if we apply it to ["A", 5] we get the multiplicity of 0, and if we apply it to ["B", 1] we get the
 * multiplicity of 1.
 *
 * However, what happens if we see a group that wasn't seen during preparation, for which we've prepared no
 * sub-transformer?  In this case, the outcome is determined by the {@link UnknownGroupPolicy}.
 *
 * @param <R> the type of result produced by this transformer
 */
@ValueEquality
public class PreparedByGroup<R> extends AbstractPreparableTransformerDynamic<R, PreparedByGroup.Prepared<R>, PreparedByGroup<R>> {
  private static final long serialVersionUID = 1;

  /**
   * The policy to use when a group not seen during preparation is encountered later, after the transformer is prepared.
   */
  public enum UnknownGroupPolicy {
    /**
     * Default policy: when a group unseen in preparation is encountered, the result is null.
     */
    RETURN_NULL,

    /**
     * When a group unseen in preparation is encountered, an arbitrary prepared subtransformer is used.
     */
    USE_ANY
  }

  private PreparableTransformer<? extends R, ?> _transformerToPrepareByGroup = null;

  @IgnoredByValueEquality // already included in the input list
  private Producer<?> _groupInput = null;

  private UnknownGroupPolicy _unknownGroupPolicy = UnknownGroupPolicy.RETURN_NULL;

  @Override
  public void validate() {
    super.validate();
    Objects.requireNonNull(_transformerToPrepareByGroup);
    Objects.requireNonNull(_groupInput);
  }

  /**
   * Sets the input that will provide the group used to determine which sub-transformer each example will be used to
   * prepare.
   *
   * @param groupInput the input providing the group for each example
   * @return a copy of this instance that will use the specified group input
   */
  public PreparedByGroup<R> withGroupInput(Producer<?> groupInput) {
    return clone(c -> c._groupInput = groupInput);
  }

  /**
   * Sets the {@link UnknownGroupPolicy} that will be used when applying this transformer to new groups that were not
   * seen during preparation.
   *
   * @param policy the policy to use
   * @return a copy of this instance that will use the specified policy
   */
  public PreparedByGroup<R> withUnknownGroupPolicy(UnknownGroupPolicy policy) {
    return clone(c -> c._unknownGroupPolicy = policy);
  }

  /**
   * Sets the preparable transformer that will be prepared by group.  Note that this instance will automatically
   * "inherit" all the inputs of this transformer (which will now also be inputs of this instance).
   *
   * @param transformerToPrepareByGroup the preparable transfomer to be prepared by group
   * @return a copy of this instance that will prepare the provided preparable transformer by group
   */
  public PreparedByGroup<R> withTransformer(PreparableTransformer<? extends R, ?> transformerToPrepareByGroup) {
    return clone(c -> c._transformerToPrepareByGroup = transformerToPrepareByGroup);
  }

  @Override
  protected Preparer<R> getPreparer(PreparerContext context) {
    return new Preparer<R>(_transformerToPrepareByGroup, _unknownGroupPolicy, context);
  }

  @Override
  protected List<? extends Producer<?>> getInputList() {
    return Iterables.prepend(_transformerToPrepareByGroup.internalAPI().getInputList(), _groupInput);
  }

  @Override
  protected PreparedByGroup<R> withInputsUnsafe(List<? extends Producer<?>> newInputs) {
    return clone(copy -> {
      copy._groupInput = newInputs.get(0);
      copy._transformerToPrepareByGroup =
          _transformerToPrepareByGroup.internalAPI().withInputsUnsafe(newInputs.subList(1, newInputs.size()));
    });
  }

  /**
   * Returns a transformer that will apply each group's subtransformer to the inputs and return the result as a
   * group-to-result map.
   *
   * This allows you to obtain the results for <b>all</b> the subtransformers on each example, regardless of which group
   * that example may belong to.  For example, a simple bagging mechanic could be implemented by:
   * (1) Assigning each training example to a random group
   * (2) Using {@link PreparedByGroup} to train a linear regressor for each group of examples
   * (3) Use the transformer returned by this method to get <b>every</b> linear regressor's prediction for each example
   * (4) Averaging the regressions together while discarding outliers
   *
   * Note that this {@link PreparedByGroup} instance must be valid before calling this method: the
   * {@link PreparedByGroup#validate()} method will automatically be called to verify this.
   *
   * @return a transformer that produces a group-to-subtransformer-result map
   */
  public PreparedTransformerDynamic<Map<Object, R>> toResultsByGroup() {
    validate();

    // we provide this transformer by:
    // (1) Using the SubtransformersByGroupView to extract a group-to-subtransformer map
    // (2) Using AllTransformationsMap to run each subtransformer in the map on the (non-group) inputs
    return new AllTransformationsMap<R>(new SubtransformersByGroupView<R>(this),
        _transformerToPrepareByGroup.internalAPI().getInputList());
  }

  /**
   * A viewer that extracts the group-to-subtransformer map from a {@link PreparedByGroup} transformer.
   *
   * @param <R> the type of result returned by each subtransformer
   */
  @ValueEquality
  private static class SubtransformersByGroupView<R> extends
    AbstractTransformerView<Map<Object, PreparedTransformer<? extends R>>, Prepared<R>, SubtransformersByGroupView<R>> {

    private static final long serialVersionUID = 1;

    SubtransformersByGroupView(PreparedByGroup<R> preparedByGroupTransformer) {
      super(preparedByGroupTransformer);
    }

    @Override
    protected Map<Object, PreparedTransformer<? extends R>> prepare(
        Prepared<R> preparedTransformerForNewData) {
      return preparedTransformerForNewData._subtransformerMap;
    }
  }

  /**
   * The preparer for the {@link PreparedByGroup} transform.
   *
   * @param <R> the type of result produced by the transformer
   */
  private static class Preparer<R> extends AbstractBatchPreparerDynamic<R, Prepared<R>> {
    private final PreparableTransformer<? extends R, ?> _transformerToPrepareByGroup;
    private final UnknownGroupPolicy _unknownGroupPolicy;
    private final HashMap<Object, com.linkedin.dagli.preparer.Preparer<? extends R, ?>> _preparerMap = new HashMap<>();
    private final PreparerContext _context;

    /**
     * Creates a new instance of the preparer.
     *
     * @param transformerToPrepareByGroup the transformer to be prepared by group
     * @param unknownGroupPolicy the {@link UnknownGroupPolicy}
     * @param context the context in which preparation is occurring
     */
    Preparer(PreparableTransformer<? extends R, ?> transformerToPrepareByGroup,
        UnknownGroupPolicy unknownGroupPolicy, PreparerContext context) {
      _transformerToPrepareByGroup = transformerToPrepareByGroup;
      _unknownGroupPolicy = unknownGroupPolicy;
      _context = context.withExampleCountLowerBound(1).withDefaultEstimatedExampleCount();
    }

    @Override
    public void processUnsafe(Object[] values) {
      Object group = values[0]; // the group input is always the first input

      // get preparer corresponding to the group, creating it if it does not already exist
      com.linkedin.dagli.preparer.Preparer<? extends R, ?> preparer =
          _preparerMap.computeIfAbsent(group, k -> _transformerToPrepareByGroup.internalAPI().getPreparer(_context));

      // pass the inputs (except the first, which is the group input) to the group's preparer
      preparer.processUnsafe(Arrays.copyOfRange(values, 1, values.length));
    }

    @Override
    public PreparerResult<Prepared<R>> finishUnsafe(ObjectReader<Object[]> inputs) {
      // finish each per-group preparer to get the prepared subtransformers for each group
      // TODO: it may be worthwhile to add optional multithreading here
      Map<Object, PreparerResultMixed<? extends PreparedTransformer<? extends R>, ? extends PreparedTransformer<? extends R>>>
          preparerResults = _preparerMap.entrySet()
          .stream()
          .map(t -> Tuple2.of(t.getKey(), t.getValue()
              .finishUnsafe(inputs.lazyFilter(arr -> Objects.equals(arr[0], t.getKey()))
                  .lazyMap(values -> Arrays.copyOfRange(values, 1, values.length)))))
          .collect(Collectors.toMap(Tuple2::get0, Tuple2::get1));

      // create a list of placeholders to act as parents for the prepared transformers so they'll have the correct arity
      List<Placeholder<Object>> placeholders =
          IntStream.range(0, _transformerToPrepareByGroup.internalAPI().getInputList().size())
              .mapToObj(i -> new Placeholder<>())
              .collect(Collectors.toList());

      // create a map from each group to the corresponding prepared transformer for preparation (training) data
      Map<Object, PreparedTransformer<? extends R>> preparedForPreparationData = preparerResults.entrySet()
          .stream()
          .map(r -> Tuple2.of(r.getKey(),
              r.getValue().getPreparedTransformerForPreparationData().internalAPI().withInputsUnsafe(placeholders)))
          .collect(Collectors.toMap(Tuple2::get0, Tuple2::get1));

      // create a map from each group to the corresponding prepared transformer for new data
      Map<Object, PreparedTransformer<? extends R>> preparedForNewData = preparerResults.entrySet()
          .stream()
          .map(r -> Tuple2.of(r.getKey(),
              r.getValue().getPreparedTransformerForNewData().internalAPI().withInputsUnsafe(placeholders)))
          .collect(Collectors.toMap(Tuple2::get0, Tuple2::get1));

      return new PreparerResult.Builder<Prepared<R>>()
          .withTransformerForPreparationData(new Prepared<>(preparedForPreparationData, _unknownGroupPolicy))
          .withTransformerForNewData(new Prepared<>(preparedForNewData, _unknownGroupPolicy))
          .build();
    }
  }

  /**
   * A prepared-by-group transformer wrapping multiple "sub-transformers", one for each "group" seen during preparation.
   * When applied to an example, the group input determines which sub-transformer is then applied to the remaining
   * inputs.
   *
   * If a group that was not seen during preparation is encountered (meaning there is no corresponding sub-transformer),
   * the result is determined by the {@link UnknownGroupPolicy}.
   *
   * @param <R> the type of result produced by this transformer
   */
  @ValueEquality
  public static class Prepared<R>
      extends AbstractPreparedStatefulTransformerDynamic<R, Map<PreparedTransformer<? extends R>, Object>, Prepared<R>> {
    // TODO: ideally, this class should be minibatch-aware, although the logic for this is non-trivial
    private static final long serialVersionUID = 1;

    private final Map<Object, PreparedTransformer<? extends R>> _subtransformerMap;
    private final UnknownGroupPolicy _unknownGroupPolicy;

    /**
     * Creates a new instance.
     *
     * @param subtransformerMap a map from groups to their corresponding subtransformers
     * @param unknownGroupPolicy the policy to use when a previously-unseen group is encountered
     */
    public Prepared(Map<Object, PreparedTransformer<? extends R>> subtransformerMap,
        UnknownGroupPolicy unknownGroupPolicy) {

      _unknownGroupPolicy = unknownGroupPolicy;
      _subtransformerMap = subtransformerMap;
    }

    @Override
    protected Map<PreparedTransformer<? extends R>, Object> createExecutionCache(long exampleCountGuess) {
      HashMap<PreparedTransformer<? extends R>, Object> executionCacheMap = new HashMap<>(_subtransformerMap.size());
      _subtransformerMap.values()
          .forEach(prepared -> executionCacheMap.put(prepared,
              prepared.internalAPI().createExecutionCache(exampleCountGuess)));
      return executionCacheMap;
    }

    @Override
    public R apply(Map<PreparedTransformer<? extends R>, Object> executionCache, List<?> values) {
      // get all the inputs except the first, which is the group value
      Object[] passedValues = values.subList(1, values.size()).toArray();

      // find the subtransformer for the provided group
      PreparedTransformer<? extends R> subtransformer = _subtransformerMap.getOrDefault(values.get(0), null);

      // if no subtransformer is known for the group, behavior depends on the UnknownGroupPolicy
      if (subtransformer == null) {
        switch (_unknownGroupPolicy) {
          case RETURN_NULL:
            return null;
          case USE_ANY:
            subtransformer = _subtransformerMap.values().iterator().next();
            break;
          default:
            // this can only happen if there is a bug in this implementation!
            throw new UnsupportedOperationException(
                "Support for the group policy " + _unknownGroupPolicy + " has not been implemented");
        }
      }

      return subtransformer.internalAPI().applyUnsafe(executionCache.get(subtransformer), passedValues);
    }
  }
}
