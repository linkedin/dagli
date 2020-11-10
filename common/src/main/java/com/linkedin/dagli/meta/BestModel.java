package com.linkedin.dagli.meta;

import com.linkedin.dagli.annotation.equality.IgnoredByValueEquality;
import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.array.ArrayElement;
import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG1x1;
import com.linkedin.dagli.dag.DAG1x2;
import com.linkedin.dagli.dag.DynamicDAG;
import com.linkedin.dagli.generator.ExampleIndex;
import com.linkedin.dagli.list.VariadicList;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.placeholder.internal.PlaceholderInternalAPI;
import com.linkedin.dagli.preparer.AbstractBatchPreparerDynamic;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.producer.ChildProducer;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparableTransformerDynamic;
import com.linkedin.dagli.transformer.PreparableTransformer;
import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.transformer.PreparedTransformerDynamic;
import com.linkedin.dagli.transformer.Transformer;
import com.linkedin.dagli.tuple.Tuple2;
import com.linkedin.dagli.util.collection.Iterables;
import com.linkedin.dagli.util.invariant.Arguments;
import com.linkedin.dagli.view.AbstractTransformerView;
import com.linkedin.dagli.view.PreparedTransformerView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * BestModel is a metatransformer (a transformer that performs operations on other transformers) taking a list of
 * preparable transformers ("models", e.g. logistic regression, FastText, an entire DAG, or any other kind of preparable
 * transformer) and then, while BestModel is being prepared, it determines which of these is "best" using a technique
 * called cross-validation.  The (prepared) model that is determined to be the best will then be used by the prepared
 * {@link BestModel} for inference in the final prepared DAG, and all other models will be ignored and discarded.
 *
 * A BestModel must be, at minimum configured with these methods before use:
 * - withEvaluator(...) to set a preparable transformer (e.g. {@link com.linkedin.dagli.evaluation.TopKEvaluation}, a
 *   DAG, or any other arbitrary logic) that will produce a {@link Comparable} "evaluation" that determines which
 *   model is best (the model whose predictions produce the largest/highest evaluation object wins)
 * - withCandidates(...) to add one or more candidate models.  There must be at least one candidate model!
 * Other settings have reasonable defaults and do not need to set explicitly unless desired.
 *
 * Example use case: you want to try several different classifiers, or many different hyperparameter (configuration)
 * settings for one classifier.  You would create a BestModel with these classifiers as candidates and BestModel will
 * choose the best one, which will then be used for all subsequent inference.
 *
 * How cross-validation works: BestModel uses k-fold cross-validation, creating multiple splits ("folds") of the data
 * (default: 5) each reserving a proportion (1/k) of the data for evaluation, and the remainder for preparing
 * (training) the candidate models.  Each candidate model is prepared (trained) multiple times, once for each split, and
 * its performance evaluated using each split's held-out evaluation data, using an evaluator you specify (e.g.
 * to calculate simple accuracy, F1 score, etc.); we suggest considering an evaluator in the
 * com.linkedin.dagli.evaluation namespace, such as {@link com.linkedin.dagli.evaluation.MultinomialEvaluation},
 * or constructing a new evaluator DAG using one of these pre-existing evaluators as components.  Whichever model has
 * the highest score (as determined by comparing the {@link Comparable} output of the evaluator) is then selected by
 * cross-validation, and trained one final time with <strong>all</strong> input data.
 *
 * Inference: by default, when BestModel is applied to a preparation (training) example it will use a model trained with
 * ALL the preparation data, including that example; this is cheating because the model has been trained on the example
 * it is now trying to predict a label for.  This is a problem when the model's predicted label is used as a feature for
 * a downstream model; to change this behavior so that inference uses a model it trained on a subset of the data that
 * excluded that example, use withPreparationDataInferenceMode(CROSS_INFERENCE).
 *
 * Models: all models must necessarily have outputs of a type that extends R (or R itself).  If your models generate
 * disparate result types, wrap them in DAGs together with the necessary conversion transformer(s) to process their
 * outputs.  E.g. if model A creates a {@link com.linkedin.dagli.math.distribution.DiscreteDistribution} of labels of
 * type R, and model B produces the most-likely output R directly, we can create a new DAG that wraps model A and uses a
 * {@link com.linkedin.dagli.distribution.MostLikelyLabelFromDistribution} transformer to convert its
 * {@link com.linkedin.dagli.math.distribution.DiscreteDistribution} output to type R.
 *
 * Inputs to BestModel: like many metatransformers, you shouldn't set the inputs to BestModel directly using its
 * withInputsUnsafe(...) method.  BestModel has no fixed arity and instead takes as input all the inputs of the models
 * it evaluates and, optionally, a "group" input, whereby examples in the same group must all be in the training data or
 * all be in the evaluation data, but not both--this can be used to prevent "cheating" in situations where training on
 * one example from the group would undesirably suggest the label for the other examples.  E.g. if examples at the same
 * exact Unix time always had the same label, using the Unix time as the group would prevent the models from being able
 * to memorize a time-to-label mapping to do well on the held-out evaluation data (although they would still be able to
 * overfit on a time-based feature, this underperformance would be correctly observed during evaluation).
 *
 * @param <R> The type of result produced by the candidate models (and thus the type of result produced by BestModel)
 */
@ValueEquality(commutativeInputs = true)
public class BestModel<R> extends AbstractPreparableTransformerDynamic<R, PreparedTransformerDynamic<R>, BestModel<R>> {
  private static final long serialVersionUID = 1;

  /**
   * When BestModel is applied to a new example (not used in preparation), it uses the best model, trained on all
   * preparation data.  However, when doing inference on a preparation example, this allows the model to cheat because
   * it's already seen the example it is now trying to predict a label for.  This tends to be a problem if this model
   * is producing a label used as a feature for another model, because the label will be much more accurate during
   * training than it will in the real world, on new data.
   *
   * Consequently, this mode changes how such inference is performed.  This has no effect on inference for new examples,
   * which always use the model trained on all preparation data.
   */
  public enum PreparationDataInferenceMode {
    /**
     * The best model, trained on all preparation data, is used to predict labels for preparation examples.  This is
     * cheating because the model has previously seen the example in question.
     */
    CHEAT,

    /**
     * A model that was trained during cross-validation on a subset of the preparation data excluding the preparation
     * example is used to infer its label.  This avoids cheating because the model has not seen the example before, but
     * the accuracy may be unrealistically low if the number of data splits used for cross-validation is low.  If this
     * is not the case an exception will be thrown.
     */
    CROSS_INFERENCE,
  }

  // the group input is always the first, at index 0
  private static final int GROUP_INPUT_INDEX = 0;

  private PreparationDataInferenceMode _preparationDataInferenceMode = PreparationDataInferenceMode.CHEAT;
  private int _splitCount = 5;

  private PreparableTransformer<? extends Comparable<?>, ?> _evaluator;

  @IgnoredByValueEquality
  private Producer<? extends R> _evaluatorPredictedLabelInput;

  // the use of LinkedStack here is suboptimal because it prevents two BestModel instances from being equals() if the
  // same candidates were added in different orders; in the future it can be replaced by a "LinkedSet".
  private List<PreparableTransformer<? extends R, ?>> _candidates = new ArrayList<>();
  private long _seed = 0;

  // not @IgnoredByValueEquality because, although our inputs will be compared commutatively, the groupInput is always
  // the first slot and an inputs list that swapped the first input (the groupInput) with the last would not be
  // logically equivalent
  private Producer<?> _groupInput = new ExampleIndex();

  @Override
  protected List<? extends Producer<?>> getInputList() {
    // we use both a list and a set to ensure that the resulting ordering of our inputs is consistent (a set/map may not
    // maintain a consistent iteration order):
    List<Producer<?>> inputList = new ArrayList<>();
    IdentityHashMap<Producer<?>, Boolean> inputSet = new IdentityHashMap<>();
    // *don't* include the evaluator's predicted label input (which we've created and is not part of the DAG)
    inputSet.put(_evaluatorPredictedLabelInput, true);
    // (we are intentionally not adding it to inputList)

    // the _groupInput is always the first input
    inputSet.put(_groupInput, true);
    inputList.add(_groupInput);

    // add all the inputs of the evaluator (other than _evaluatorPredictedLabelInput)
    addInputs(inputSet, inputList, _evaluator);

    // collect all the remaining unique inputs of our candidates and add them to our input list
    for (PreparableTransformer<? extends R, ?> candidate : _candidates) {
      addInputs(inputSet, inputList, candidate);
    }

    return inputList;
  }

  /**
   * Remaps a transformer's inputs according to a replacement map.  Inputs not present in the map are left as-is.
   *
   * @param transformer the transformer whose inputs should be remapped
   * @param map the replacement map
   * @return a new transformer with remapped inputs
   */
  @SuppressWarnings("unchecked") // safe because withInputsUnsafe(...) always returns the same type of transformer
  private static <T extends Transformer<?>> T remapInputs(T transformer, IdentityHashMap<Producer<?>, Producer<?>> map) {
    return (T) transformer.internalAPI()
        .withInputsUnsafe(transformer.internalAPI()
            .getInputList()
            .stream()
            .map(input -> map.getOrDefault(input, input))
            .collect(Collectors.toList()));
  }

  @Override
  protected BestModel<R> withInputsUnsafe(List<? extends Producer<?>> newInputs) {
    // first, establish a mapping between the old and new inputs:
    IdentityHashMap<Producer<?>, Producer<?>> mapping = new IdentityHashMap<>(newInputs.size());
    List<? extends Producer<?>> currentInputs = getInputList();
    for (int i = 0; i < currentInputs.size(); i++) {
      mapping.put(currentInputs.get(i), newInputs.get(i));
    }

    // to remap our inputs, we need to update all our candidates
    List<PreparableTransformer<? extends R, ?>> remappedCandidateList =
        _candidates.stream().map(candidate -> remapInputs(candidate, mapping)).collect(Collectors.toList());

    return clone(c -> {
      c._candidates = remappedCandidateList;
      c._evaluator = remapInputs(this._evaluator, mapping);
      c._groupInput = mapping.get(c._groupInput);
    });
  }

  /**
   * Scans the transformer for novel inputs and, when found, adds them to the input set/list
   *
   * @param inputSet a Map used as a set to identify previously-seen inputs
   * @param inputList a list for newly-discovered inputs
   * @param transformer the transformer whose inputs will be examined
   */
  private static void addInputs(IdentityHashMap<Producer<?>, Boolean> inputSet, List<Producer<?>> inputList,
      Transformer<?> transformer) {
    for (Producer<?> input : transformer.internalAPI().getInputList()) {
      if (inputSet.put(input, true) == null) { // only add input to list if this is the first time we've seen it
        inputList.add(input);
      }
    }
  }

  /**
   * Returns a copy of this instance with the specified preparation data inference mode set.  New examples always use
   * the best model trained with all data for inference, and inference on the preparation data, by default, is the same.
   * However, this is cheating because that model has already trained on the example it is being asked to predict on,
   * and hence an alternate inference mode may be used to avoid this.
   *
   * The default mode is PreparationDataInferenceMode.CHEAT.  See {@link PreparationDataInferenceMode} for more details.
   *
   * @param mode the mode to use
   * @return a copy of this instance with the desired mode set
   */
  public BestModel<R> withPreparationDataInferenceMode(PreparationDataInferenceMode mode) {
    return clone(c -> c._preparationDataInferenceMode = mode);
  }

  /**
   * Returns a copy of this instance that will use the specified number of splits to use when evaluating the candidate
   * models during preparation.  This must be at least 2; the default is 5.
   *
   * @param count the number of splits
   * @return a copy of this instance that will use the specified number of splits
   */
  public BestModel<R> withSplitCount(int count) {
    Arguments.check(count >= 2);
    return clone(c -> c._splitCount = count);
  }

  /**
   * Returns a copy of this instance that will use the specified evaluator to determine which of the candidate models
   * is the best performing.
   *
   * The underlying mechanics are somewhat complicated and detailed below.  In practice, however, usage is
   * straightforward; for example:
   * <code>
   *   bestModel = bestModel.withEvaluator(
   *     new MultinomialEvaluation().withActualLabelInput(trueLabels)::withPredictedLabelInput);
   * </code>
   *
   * What's happening here is that our chosen evaluator is a {@link com.linkedin.dagli.evaluation.MultinomialEvaluation}
   * and we are passing withEvaluator(...) a function that will return a fully-configured
   * {@link com.linkedin.dagli.evaluation.MultinomialEvaluation} when it is passed the input that will provide the
   * predicted labels.
   *
   * The precise mechanism is that the evaluator is passed as a {@link com.linkedin.dagli.util.function.Function1}
   * "factory" accepting a {@link Producer} input (providing the predictions from the candidate models) and returning
   * a {@link PreparableTransformer}.  {@link BestModel} will call this function, obtain the preparable evaluator, and
   * then repeatedly prepare it, each time feeding it the outputted predictions of a candidate model.
   *
   * The evaluator transformer should, once prepared, produce an "evaluation" object corresponding to how good the
   * predictions of the candidate model were (these were seen during preparation).  This should be the output
   * <strong>regardless</strong> of the input provided (i.e. the result of the transformer should be constant);
   * {@link Producer#hasConstantResult()} must return true for this evaluator.
   *
   * Evaluations must be {@link Comparable}.  The candidate model that resulted in the "largest" evaluation is taken to
   * be the best.
   *
   * @param evaluatorFactory a factory that produces preparable evaluators when passed an input that will provide the
   *                        outputs (predictions) of the candidate models
   * @return a copy of this instance that will use the provided evaluator
   */
  public <E extends Comparable<? super E>> BestModel<R> withEvaluator(
      Function<Producer<? extends R>, PreparableTransformer<E, ?>> evaluatorFactory) {
    Placeholder<R> predictedLabelInput =
        new Placeholder<>("BestModel Predicted Label Placeholder (not exposed externally)");
    PreparableTransformer<? extends Comparable<?>, ?> evaluator = evaluatorFactory.apply(predictedLabelInput);
    Arguments.check(evaluator.hasConstantResult(), "The evaluator does not have a constant result");

    // check if the evaluator has intermediate producers between it and our placeholder:
    if (ChildProducer.ancestors(evaluator, Integer.MAX_VALUE)
        .anyMatch(path -> path.peek() == predictedLabelInput && path.size64() > 2)) {
      // we need an evaluator that accepts our placeholder as a *direct* input only, so we must create a transformer
      // that does so:
      evaluator = DynamicDAG.fromMinimalInputBoundedSubgraph(evaluator, predictedLabelInput);
    }

    // need to create final evaluator so we can pass to lambda
    final PreparableTransformer<? extends Comparable<?>, ?> finalEvaluator = evaluator;
    return clone(c -> {
      c._evaluatorPredictedLabelInput = predictedLabelInput;
      c._evaluator = finalEvaluator;
    });
  }

  /**
   * Returns a copy of this instance that will use the specified group input.  Two examples with the same group will
   * never be in both the training and evaluation parts of a data split: they will either both be in the training set
   * or both be in evaluation set.
   *
   * The default group input is an {@link ExampleIndex} such that every example has its own unique group.
   *
   * @param groupInput the input that supplies the group for each example.  The group may be any arbitrary type of
   *                   object; groups will be compared with .equals(...) and should implement .hashCode(...).
   * @return a copy of this instance that will use the specified group input
   */
  public BestModel<R> withGroupInput(Producer<?> groupInput) {
    return clone(c -> c._groupInput = groupInput);
  }

  /**
   * Returns a copy of this instance that will use the specified candidate models; any existing candidates previously
   * provided are ignored.
   *
   * The inputs of candidate models become the inputs to the BestModel, so all inputs to the candidate models must
   * be specified (i.e. no MissingInput inputs) before passing them; an exception will be thrown otherwise.
   *
   * @param candidateModels the candidate models to add for consideration as the possible best model
   * @return a copy of this instance with the specified candidate models added
   */
  @SafeVarargs
  public final BestModel<R> withCandidates(PreparableTransformer<? extends R, ?>... candidateModels) {
    return withCandidates(Arrays.asList(candidateModels));
  }

  /**
   * Returns a copy of this instance that will use the specified candidate models; any existing candidates previously
   * provided are ignored.
   *
   * The inputs of candidate models become the inputs to the BestModel, so all inputs to the candidate models must
   * be specified (i.e. no MissingInput inputs) before passing them; an exception will be thrown otherwise.
   *
   * @param candidateModels the candidate models to add for consideration as the possible best model
   * @return a copy of this instance with the specified candidate models added
   */
  public final BestModel<R> withCandidates(Iterable<PreparableTransformer<? extends R, ?>> candidateModels) {
    ArrayList<PreparableTransformer<? extends R, ?>> candidateList = Iterables.newArrayList(candidateModels);

    // check each candidate for missing inputs
    if (candidateList.stream()
        .flatMap(candidate -> candidate.internalAPI().getInputList().stream())
        .anyMatch(input -> input instanceof MissingInput)) {
        throw new IllegalArgumentException(
            "A provided candidate model has at least one unspecified input (that is, one of its inputs is an "
                + "MissingInput instance).  All the inputs of the candidates must be specified prior to calling "
                + "withCandidates(...).");
    }
    return clone(c -> c._candidates = candidateList);
  }

  /**
   * Returns a copy of this instance that will use the specified "seed" value to pseudo-randomize the data splits.
   *
   * @param seed the value used to pseudo-randomize the data splits
   * @return a copy of this instance that will use the specified seed
   */
  public BestModel<R> withSeed(long seed) {
    return clone(c -> c._seed = seed);
  }

  @Override
  public void validate() {
    super.validate();

    // check that withEvaluator(...) has been called to set up evaluation
    if (_evaluator == null) {
      throw new IllegalArgumentException(
          "You must call withEvaluator(...) to set the evaluator that will be used to determine which of the candidate "
          + "models is best");
    }
  }

  @Override
  protected Preparer<R> getPreparer(PreparerContext context) {
    if (this._candidates.isEmpty()) {
      throw new NoSuchElementException(
          "Attempting to prepare BestModel, but no candidates are available.  You may have forgotten to add "
              + "candidates with the withCandidate(...) method.  Remember that transformers are immutable; calls to "
              + "withCandidates() should look like 'bestModel = bestModel.withCandidates(...)'.  If you just call "
              + "'bestModel.withCandidates(...)' without storing the result, the new copy of BestModel with the added "
              + "candidate will be discarded!");
    }
    return new Preparer<>(this, context);
  }

  private static List<Producer<?>> mapInputs(int[] inputMap, List<? extends Producer<?>> inputs, Producer<?> fallbackInput) {
    List<Producer<?>> finalInputs = new ArrayList<>(inputMap.length);
    for (int inputIndex : inputMap) {
      finalInputs.add(inputIndex == -1 ? fallbackInput : inputs.get(inputIndex));
    }
    return finalInputs;
  }

  /**
   * @return a view of this {@link BestModel} instance that will produce the best prepared transformer (the best trained
   *         model)
   */
  public Producer<PreparedTransformer<R>> asBestPreparedModel() {
    return new BestPreparedModelView<>(this);
  }

  @ValueEquality
  private static class BestPreparedModelView<R>
      extends AbstractTransformerView<PreparedTransformer<R>, PreparedTransformerDynamic<R>, BestPreparedModelView<R>> {
    private static final long serialVersionUID = 1;

    /**
     * Creates a new view of the specified transformer
     * @param viewedTransformer the transformer that, when prepared, will be used to calculate the view
     */
    public BestPreparedModelView(PreparableTransformer<?, ? extends PreparedTransformerDynamic<R>> viewedTransformer) {
      super(viewedTransformer);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected PreparedTransformer<R> prepare(PreparedTransformerDynamic<R> preparedTransformerForNewData) {
      // our prepared transformer type is a dynamic DAG, and we know the best prepared model is its sole output because
      // the DAG is non-reduced
      return (PreparedTransformer<R>) ((DynamicDAG.Prepared<R>) preparedTransformerForNewData).internalAPI()
          .getOutputProducer(0);
    }
  }

  private static class Preparer<R> extends AbstractBatchPreparerDynamic<R, PreparedTransformerDynamic<R>> {
    // This determines how many predicted labels are cached before feeding them to the evaluator for a given candidate.
    // Batching is used because there are multiple splits for the same candidate/evaluator running in parallel and thus
    // the evaluator is locked while a batch of predicted/true label pairs is fed in.
    private static final int EVALUATION_BATCH_SIZE = 1024;

    private final BestModel<R> _owner;
    private final PreparerContext _context;

    public Preparer(BestModel<R> owner, PreparerContext context) {
      _owner = owner;
      _context = context;
    }

    @Override
    public void processUnsafe(Object[] values) {
      // noop
    }

    /**
     * @param ownerInputToIndexMap a map from the BestModel instance's inputs to their indices in the input list
     * @param candidate the target producer, sharing inputs with the BestModel, whose inputs should be mapped to its
     *                  subsuming set of inputs
     * @return an array that maps from the index of the input in the candidate's input list to the index of the same
     *         input in the BestModel's input list.  An array element of -1 indicates the input was the special
     *         {@link #_evaluatorPredictedLabelInput} which is not actually an input to BestModel.
     */
    private int[] getProducerInputToGlobalInputIndexMap(IdentityHashMap<Producer<?>, Integer> ownerInputToIndexMap,
        Transformer<?> candidate) {
      List<? extends Producer<?>> candidateInputList = candidate.internalAPI().getInputList();

      int[] inputArray = new int[candidateInputList.size()];
      for (int j = 0; j < candidateInputList.size(); j++) {
        inputArray[j] = (candidateInputList.get(j) == _owner._evaluatorPredictedLabelInput) ? -1
            : ownerInputToIndexMap.get(candidateInputList.get(j));
      }

      return inputArray;
    }

    /**
     * @return a map from the owner's inputs to their indices in the inputs list
     */
    private IdentityHashMap<Producer<?>, Integer> getOwnerInputToIndexMap() {
      // build a map from each input to our owner (a BestModel) to its index in the input list
      IdentityHashMap<Producer<?>, Integer> ownerInputToIndexMap = new IdentityHashMap<>();
      for (Producer<?> input : _owner.getInputList()) {
        ownerInputToIndexMap.put(input, ownerInputToIndexMap.size());
      }
      return ownerInputToIndexMap;
    }

    /**
     * Builds an array for each candidate containing the indices of that candidate's inputs in the "global" list of
     * inputs consumed by the owning {@link BestModel} instance.
     *
     * @return an array of arrays; result[index of candidate][index of candidate's input] provides the index of the
     *         specified input of the specified candidate in the global list of inputs
     */
    private int[][] getCandidateInputIndices(IdentityHashMap<Producer<?>, Integer> ownerInputToIndexMap) {
      // figure out the indices of each candidate's inputs
      int[][] candidateInputs = new int[_owner._candidates.size()][];
      for (int i = 0; i < _owner._candidates.size(); i++) {
        candidateInputs[i] = getProducerInputToGlobalInputIndexMap(ownerInputToIndexMap, _owner._candidates.get(i));
      }

      return candidateInputs;
    }

    private static <T extends Transformer> T replaceInput(T producer, Producer<?> toReplace, Producer<?> replacement) {
      ArrayList<Producer<?>> inputList = new ArrayList<>(producer.internalAPI().getInputList());
      Collections.replaceAll(inputList, toReplace, replacement);
      return (T) producer.internalAPI().withInputsUnsafe(inputList);
    }

    @Override
    public PreparerResult<PreparedTransformerDynamic<R>> finishUnsafe(ObjectReader<Object[]> inputs) {
      IdentityHashMap<Producer<?>, Integer> ownerInputToIndexMap = getOwnerInputToIndexMap();

      // figure out the indices of each candidate's inputs
      int[][] candidateInputs = getCandidateInputIndices(ownerInputToIndexMap);
      // and for the evaluator, too
      int[] evaluatorInputMap = getProducerInputToGlobalInputIndexMap(ownerInputToIndexMap, _owner._evaluator);

      Placeholder<Object[]> inputArrayPlaceholder = new Placeholder<>();

      // get transformers that can pull out each element of the input array
      List<ArrayElement<Object>> arrayElements = IntStream.range(0, ownerInputToIndexMap.size())
          .mapToObj(i -> new ArrayElement<>().withIndex(i).withInput(inputArrayPlaceholder))
          .collect(Collectors.toList());

      // now hook up each candidate to our array-element-ified inputs
      List<PreparableTransformer<? extends R, ?>> hookedUpCandidateList = IntStream.range(0, _owner._candidates.size())
          .mapToObj(i -> _owner._candidates.get(i)
              .internalAPI()
              .withInputsUnsafe(mapInputs(candidateInputs[i], arrayElements, null)))
          .collect(Collectors.toList());

      // now we can get the cross-trainers for each candidate
      List<KFoldCrossTrained<? extends R>> crossTrainedList = hookedUpCandidateList.stream()
          .map(candidate -> new KFoldCrossTrained<>(candidate).withGroupInput(arrayElements.get(GROUP_INPUT_INDEX))
              .withSplitCount(_owner._splitCount)
              .withSeed(_owner._seed)
              .withRetrainForNewData(false)) // don't retrain the candidate on all data
          .collect(Collectors.toList());

      final PreparableTransformer<? extends Comparable<?>, ?> evaluatorPrototype = _owner._evaluator.internalAPI()
          .withInputsUnsafe(mapInputs(evaluatorInputMap, arrayElements, _owner._evaluatorPredictedLabelInput));

      // create a list of views so we can extra the cross-validated transformers when we're done
      List<PreparedTransformerView<? extends PreparedTransformer<? extends R>>> crossTrainedViewList =
          crossTrainedList.stream().map(PreparedTransformerView::new).collect(Collectors.toList());

      // each cross-validator's output flows to an evaluator instance
      List<PreparableTransformer<? extends Comparable, ?>> evaluationList = crossTrainedList.stream()
          .map(crossTrained -> replaceInput(evaluatorPrototype, _owner._evaluatorPredictedLabelInput, crossTrained))
          .collect(Collectors.toList());

      // finally, we want to package the evaluations into a list that we can output from the DAG
      VariadicList<Comparable> evaluationResultList = new VariadicList<Comparable>().withInputs(evaluationList);

      // now we're ready to create and prepare our DAG
      DAG1x2.Prepared<Object[], List<Comparable>, List<PreparedTransformer<? extends R>>> crossValidationDAG =
          DAG.withPlaceholder(inputArrayPlaceholder)
          .withOutputs(evaluationResultList, new VariadicList<>(crossTrainedViewList))
          .withExecutor(_context.getExecutor())
          .prepare(inputs);

      // we need to get a single example input to run through the DAG so we can collect the evaluations and figure out
      // which candidate is "best"; the output of the prepared DAG above is reducible to Constants, so we can just use
      // these constant values:
      Tuple2<List<Comparable>, List<PreparedTransformer<? extends R>>> constantOutput =
          crossValidationDAG.getConstantResult();

      Objects.requireNonNull(constantOutput.get0(),
          "BestModel expected the list of prepared evaluations to reduce to a Constant, but this is not the case; "
              + "the cause may be a bug in an evaluator, a DAG reducer, or, less likely, Dagli itself");

      Objects.requireNonNull(constantOutput.get1(),
          "BestModel expected the list of cross-trained transformers to reduce to a Constant, but this is not the "
              + "case; the cause is most likely a bug in a DAG reducer or, less likely, in Dagli itself");

      final int bestCandidateIndex = Iterables.argMax(constantOutput.get0());
      final List<PreparedTransformer<? extends R>> crossTrainedPreparedList = constantOutput.get1();

      // now that we've figured out our best candidate, we need to retrain it with all our training data
      DAG1x1.Prepared<Object[], ? extends R> bestPreparedCandidateDAG = DAG.withPlaceholder(inputArrayPlaceholder)
          .withNoReduction()
          .withOutput(hookedUpCandidateList.get(bestCandidateIndex))
          .withExecutor(_context.getExecutor())
          .prepare(inputs);

      // extract the prepared best candidate
      PreparedTransformer<R> prepared =
          (PreparedTransformer<R>) bestPreparedCandidateDAG.internalAPI().getOutputProducer(0);

      List<Placeholder<?>> placeholders = PlaceholderInternalAPI.createPlaceholderList(arrayElements.size());
      prepared =
          prepared.internalAPI().withInputsUnsafe(mapInputs(candidateInputs[bestCandidateIndex], placeholders, null));

      DynamicDAG.Prepared<R> dynamicPrepared =
          new DynamicDAG.Prepared<>().withNoReduction().withPlaceholders(placeholders).withOutputs(prepared);

      // now we're ready to return our final prepared transformer(s)
      return new PreparerResult.Builder<PreparedTransformerDynamic<R>>().withTransformerForPreparationData(
          _owner._preparationDataInferenceMode == PreparationDataInferenceMode.CHEAT ? dynamicPrepared
              : PreparedTransformerDynamic.<R>from(DAG.Prepared.withPlaceholder(inputArrayPlaceholder)
                  .withOutput(crossTrainedPreparedList.get(bestCandidateIndex)), arrayElements.size()))
          .withTransformerForNewData(dynamicPrepared)
          .build();
    }
  }
}