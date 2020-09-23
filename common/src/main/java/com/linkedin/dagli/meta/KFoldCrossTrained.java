package com.linkedin.dagli.meta;

import com.linkedin.dagli.annotation.equality.IgnoredByValueEquality;
import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.generator.ExampleIndex;
import com.linkedin.dagli.math.hashing.MurmurHash3;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.preparer.AbstractPreparerDynamic;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerDynamic;
import com.linkedin.dagli.preparer.PreparerMode;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparableTransformerDynamic;
import com.linkedin.dagli.transformer.AbstractPreparedStatefulTransformerDynamic;
import com.linkedin.dagli.transformer.PreparableTransformer;
import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.util.invariant.Arguments;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * Performs k-fold cross-training with split sizes that are approximate.  Please note that this is not the same thing
 * as cross-validation to select the best model; use {@link BestModel} for that.  Instead, cross-training ensures that,
 * when training a model and then using its predictions as downstream features, k sub-models are trained, each using
 * a proportion (k-1)/k of the data, and the prediction on a training example is always made with the sub-model that
 * did not use it in training.  Otherwise, if the model were allowed to train on the example and then make a prediction
 * that was then used as a feature, the model (and thus the quality of the feature) could be much better than what it
 * would be on future unseen examples (e.g. it could simply memorize the labels and regurgitate them as its output).
 * In that case, a downstream model using this feature could give it a very high weight (as it seems to correlate
 * very well with the label) but, in real-world examples, the feature would actually be far noisier and the
 * downstream model would suffer poor performance (this phenomenon is a form of overfitting).
 *
 * How it works:
 * In k-fold cross-training, the data is randomly split into k segments, and k-1 segments are used to train (prepare)
 * the provided preparable transformer k times.
 *
 * Groups:
 * Optionally, examples may have a "group" associated with them.  You can specify this group using the
 * withGroupInput(...) method.  Data is never split across "groups" (all examples in a group will go into the same
 * fold), but be aware that a small number of very large groups may result in unbalanced data splits (since the fold
 * each example maps to is determined by hashing the group).
 *
 * Inference on new data:
 * By default, the model is also trained one additional time, on *all* the data.  The resulting prepared model is used
 * only later, for new data not used during training.  If this is disabled by calling {}
 */
@ValueEquality
public class KFoldCrossTrained<R> extends AbstractPreparableTransformerDynamic<R, PreparedTransformer<R>, KFoldCrossTrained<R>> {

  private static final long serialVersionUID = 1;

  private PreparableTransformer<? extends R, ?> _trainer;

  @IgnoredByValueEquality // already included in the compared input list
  private Producer<?> _groupInput = new ExampleIndex();

  private int _splitCount = 5;
  private long _seed = 0;
  private boolean _retrainForNewData = true;

  /**
   * Creates a new k-fold cross-trainer for the provided preparable transformer.
   *
   * @param trainer the preparable transformer to be cross-trained
   */
  public KFoldCrossTrained(PreparableTransformer<? extends R, ?> trainer) {
    _trainer = trainer;
  }

  /**
   * Default constructor.  The preferred way to configure Dagli nodes is using in-place builder methods (with____())
   * rather than constructor arguments; the disadvantage is that this can prevent Java from inferring types correctly,
   * which is why this class also has a {@link #KFoldCrossTrained(PreparableTransformer)} convenience constructor.
   */
  public KFoldCrossTrained() { }

  @Override
  protected List<? extends Producer<?>> getInputList() {
    return getAppendedInputs(_trainer.internalAPI().getInputList(), _groupInput);
  }

  @Override
  protected KFoldCrossTrained<R> withInputsUnsafe(List<? extends Producer<?>> newInputs) {
    return clone(c -> {
      c._trainer = c._trainer.internalAPI().withInputsUnsafe(newInputs.subList(0, newInputs.size() - 1));
      c._groupInput = newInputs.get(newInputs.size() - 1); // this is always the last input
    });
  }

  // creates a copy of the provided list with an additional producer appended to the end
  private static List<Producer<?>> getAppendedInputs(List<? extends Producer<?>> inputs, Producer<?> added) {
    ArrayList<Producer<?>> res = new ArrayList<>(inputs.size() + 1);
    res.addAll(inputs);
    res.add(added);
    return res;
  }

  /**
   * Creates a copy of this instance that will have retraining for new data enabled or disabled as specified.
   *
   * Cross-training produces submodels that are used to predict results for the training data, where the
   * submodel used for a particular example is guaranteed not to have been trained on that example, preventing
   * "cheating" that can lead to overfitting if these predictions are then used as features to train another, downstream
   * model.
   *
   * However, for future, "new" data not used during training, this concern does not (usually) apply: future data hasn't
   * been seen during training and we'd thus prefer to predict a result using a model that performs as well as possible,
   * i.e. one that has trained with all available training data.  Consequently, by default, in addition to training
   * submodels for each "fold", {@link KFoldCrossTrained} trains an additional instance of the model on
   * <strong>all</strong> training data, which is then used for any future predictions.  This is usually what you want.
   *
   * However, if you call this method with a value of false, no additional model will be trained.  Instead, future
   * examples will be predicted using one of the submodels (which one is determined by the group input just as it is
   * when making predictions for the training data: if the group for a new example is the same as a training example,
   * the submodel used to make a prediction for each will be the same).  There are three reasons why this
   * <strong>might</strong> be desirable:
   * (1) Retraining the model will require additional computational resources.  For higher numbers of folds this is
   *     usually inconsequential: if we assume training cost is linear in the number of examples, the additional cost
   *     to retrain if we're doing 5-fold cross-training is only 25% more.  However, for 2-fold cross-training, the
   *     cost is 100% more.
   * (2) Retraining the model on all data may result in predictions that are substantially different (i.e. more
   *     accurate) than those made for training examples, because the model saw a greater amount of data.  At the
   *     extreme, for 2-fold cross-training, a model trained on all data sees twice as much data as seen in each fold.
   *     This could lead to a transformer downstream in the DAG consuming these predictions seeing very different inputs
   *     at training and test time, hurting performance.  At higher folds the amounts of data used to train are much
   *     more similar (e.g. at 5-fold cross-training the amount of data used to train for each fold is 80% of all data)
   *     and so this is much less likely to matter.
   * (3) There is a need to ensure that the model used to predict the result for an example at training-time is the same
   *     as the one used to predict the result for an example at inference/test-time, given that they have the same
   *     group.  This is useful in very specific applications.
   *
   * @param shouldRetrain whether the model should be retrained using 100% of the training data so that it may be
   *                      used to predict results for future (non-training) examples.
   * @return a copy of this instance that will have retrainining enabled or disabled as specified
   */
  public KFoldCrossTrained<R> withRetrainForNewData(boolean shouldRetrain) {
    return clone(c -> c._retrainForNewData = shouldRetrain);
  }


  /**
   * Creates a copy of this instance that will perform cross-training of the provided {@link PreparableTransformer}.
   *
   * @param trainer the {@link PreparableTransformer} to be cross-trained
   * @return a copy of this instance that will cross-train the specified {@link PreparableTransformer}
   */
  public KFoldCrossTrained<R> withPreparable(PreparableTransformer<? extends R, ?> trainer) {
    return clone(c -> c._trainer = trainer);
  }

  /**
   * Sets the number of folds ("k") used for cross-training.  This must be at least 2.
   *
   * Cross-training will train k sub-models, each using a proportion (k-1)/k of the data, and the prediction on a
   * training example will always be made with the sub-model that did not use it in its training data.
   *
   * @param k the number of folds to use (and submodels to train)
   * @return a copy of this instance that will use the specified number of folds
   */
  public KFoldCrossTrained<R> withSplitCount(int k) {
    Arguments.check(k >= 2, "split count must be greater than or equal to 2");

    return clone(c -> c._splitCount = k);
  }

  /**
   * Each example is excluded from the training set of one submodel.  Which one is determined (deterministically) by the
   * example's group (if not provided, each example belongs to its own group) and the "seed".
   *
   * Keeping the seedd value the same will ensure a consistent split of data across different runs; conversely, by
   * changing the seed you can ensure differing splits.  The default seed value is 0.
   *
   * @param seed a value that changes how examples are pseudorandomly assigned to folds
   * @return a copy of this instance that will use the specified seed
   */
  public KFoldCrossTrained<R> withSeed(long seed) {
    return clone(c -> c._seed = seed);
  }

  /**
   * Sets the input that will provide the "group" for each example.  Two examples with the same group will always be
   * either both included or excluded in any particular fold.
   *
   * By default, an {@link ExampleIndex} provides the group, so each example belongs to its own unique group.
   *
   * @param groupInput a {@link Producer} providing the group for each example
   * @return a copy of this instance that will use the specified group provider
   */
  public KFoldCrossTrained<R> withGroupInput(Producer<?> groupInput) {
    return clone(c -> c._groupInput = groupInput);
  }

  // given a group, the number of folds and the seed, calculates the fold that will exclude this example from training
  private static <G> int getFold(G group, int foldCount, long seed) {
    return (int) Math.abs((MurmurHash3.fmix64(group.hashCode() ^ seed)) % foldCount);
  }

  /**
   * A prepared, cross-trained model that will be used for inference on training (preparation) data <b>only</b>.
   * This occurs during DAG preparation for the examples used to prepare the KFoldCrossTrained transformer.
   *
   * In this case, each example will be transformed by the prepared transformer that was prepared using a subset of the
   * data that excluded that example.  This helps avoid overfitting.  See the description of the
   * {@link KFoldCrossTrained} class for more details.
   *
   * @param <R> the type result produced by this transformer
   */
  @ValueEquality
  private static class Prepared<R> extends AbstractPreparedStatefulTransformerDynamic<R, Object[], Prepared<R>> {
    private static final long serialVersionUID = 1;

    private final List<PreparedTransformer<? extends R>> _preparedTransformers;
    private final long _seed;

    /**
     * Creates a new instance.
     *
     * @param preparedTransformers the prepared transformers to be used to transform examples
     * @param seed the seed that helps determine which prepared transformer to use for which example
     */
    Prepared(PreparedTransformer<? extends R>[] preparedTransformers, long seed) {
      _preparedTransformers = Arrays.asList(preparedTransformers);
      _seed = seed;
    }

    @Override
    protected R apply(Object[] executionCache, List<?> values) {
      int fold = getFold(values.get(values.size() - 1),  _preparedTransformers.size(), _seed);

      return _preparedTransformers.get(fold)
          .internalAPI()
          .applyUnsafe(executionCache[fold], values);
    }

    @Override
    protected Object[] createExecutionCache(long exampleCountGuess) {
      return _preparedTransformers.stream()
          .map(prepared -> prepared.internalAPI().createExecutionCache(exampleCountGuess))
          .toArray();
    }
  }

  /**
   * Preparer that will perform k-fold cross-training.
   *
   * @param <R> the type of result of the prepared transformers
   */
  private static class Preparer<R> extends AbstractPreparerDynamic<R, PreparedTransformer<R>> {
    // the preparer that will prepare a model on all our data; this will be null if retrainForNewData == false
    private final com.linkedin.dagli.preparer.Preparer<? extends R, ?> _newDataPreparer;
    // preparers for each data fold/split
    private final com.linkedin.dagli.preparer.Preparer<? extends R, ?>[] _foldPreparers;

    private final long _seed;

    @Override
    public PreparerMode getMode() {
      return _foldPreparers[0].getMode(); // this is guaranteed to be the mode of all our preparers because they were
                                          // generated from the same transformer
    }

    /**
     * Create a new preparer.
     *
     * @param context information about the context in which preparation is occurring (e.g. how many examples to expect)
     * @param k the number of folds
     * @param trainer the preparable transformer that will be cross-trained
     * @param seed a seed value that helps determine the fold that will exclude a particular example from its training
     *             data
     */
    Preparer(PreparerContext context, int k, PreparableTransformer<? extends R, ?> trainer, long seed,
        boolean retrainForNewData) {
      assert k >= 2;

      _newDataPreparer = retrainForNewData ? trainer.internalAPI().getPreparer(context) : null;

      _foldPreparers = new com.linkedin.dagli.preparer.Preparer[k];
      for (int i = 0; i < k; i++) {
        _foldPreparers[i] = trainer.internalAPI().getPreparer(context
                .withExampleCountLowerBound(0) // extremely unlikely if the number of examples is large, but possible
                .withEstimatedExampleCount((context.getEstimatedExampleCount() * (k - 1)) / k));
      }

      // note that the spec of the dagli.preparer.Preparer interface guarantees that the mode of all our preparers will
      // be the same, so it suffices to check just one of them:
      Arguments.inSet(_foldPreparers[0].getMode(),
          () -> "The preparer mode " + _foldPreparers[0].getMode() + "is unknown to KFoldCrossTrained",
          PreparerMode.BATCH, PreparerMode.STREAM);

      _seed = seed;
    }

    @Override
    public void processUnsafe(Object[] values) {
      Object[] passedValues = Arrays.copyOf(values, values.length - 1);

      if (_newDataPreparer != null) {
        _newDataPreparer.processUnsafe(passedValues);
      }
      int fold = getFold(values[values.length - 1], _foldPreparers.length, _seed);
      for (int i = 0; i < _foldPreparers.length; i++) {
        if (i != fold) {
          _foldPreparers[i].processUnsafe(passedValues);
        }
      }
    }

    @Override
    public PreparerResult<PreparedTransformer<R>> finishUnsafe(ObjectReader<Object[]> inputs) {
      final Future<PreparedTransformer<? extends R>>[] preparedFoldFutures = new Future[_foldPreparers.length];
      final Future<PreparedTransformer<? extends R>> preparedForNewDataFuture;

      ExecutorService threadPool =
          Executors.newFixedThreadPool(Math.min(_foldPreparers.length + 1, Runtime.getRuntime().availableProcessors()));

      try {
        // set up futures that will train the k submodels on each of the different k folds
        for (int i = 0; i < _foldPreparers.length; i++) {
          final int index = i;
          ObjectReader<Object[]> preparerInputs = inputs == null ? null
              : inputs.lazyFilter(arr -> index != getFold(arr[arr.length - 1], _foldPreparers.length, _seed))
                  .lazyMap(arr -> Arrays.copyOf(arr, arr.length - 1));
          preparedFoldFutures[index] = threadPool.submit(
              () -> _foldPreparers[index].finishUnsafe(preparerInputs).getPreparedTransformerForNewData());
        }

        // future to prepare a transformer using all available data, if retrainForNewData was true
        preparedForNewDataFuture = _newDataPreparer != null ? threadPool.submit(() -> _newDataPreparer.finishUnsafe(
            inputs == null ? null : inputs.lazyMap(arr -> Arrays.copyOf(arr, arr.length - 1)))
            .getPreparedTransformerForNewData()) : null;
      } finally {
        threadPool.shutdown(); // make sure outstanding threads are eventually terminated
      }

      try {
        // now get each of the values from the futures in turn:
        PreparedTransformer<? extends R>[] preparedFolds = new PreparedTransformer[_foldPreparers.length];

        for (int i = 0; i < _foldPreparers.length; i++) {
          preparedFolds[i] = preparedFoldFutures[i].get();
        }


        // create the prepared transformer that will be used for inference on the training data (only)
        Prepared<R> preparedSplitter = new Prepared<R>(preparedFolds, _seed);

        // the transformer used for new data will either be trained on all data (if retrainForNewData was true) or
        // will be the same k-fold Prepared instance as used to predict for the training data (if it was false)
        PreparedTransformer<R> preparedForNewData =
            preparedForNewDataFuture != null ? new AppendedArityPreparedTransformer<R>(preparedForNewDataFuture.get(),
                MissingInput.get()) : preparedSplitter;

        return new PreparerResult.Builder<PreparedTransformer<R>>().withTransformerForPreparationData(preparedSplitter)
            .withTransformerForNewData(preparedForNewData)
            .build();
      } catch (InterruptedException | ExecutionException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  protected PreparerDynamic<R, PreparedTransformer<R>> getPreparer(PreparerContext context) {
    return new Preparer<>(context, _splitCount, _trainer, _seed, _retrainForNewData);
  }

  /**
   * Adds an additional input to a PreparedTransformer of arbitrary arity.  This is used to ensure that the prepared
   * transformer trained by KFoldCrossTrained on all the data has the right arity (since the underlying transformer
   * being trained won't have a group input as KFoldCrossTrained does).
   *
   * @param <R> the type of result returned by the transformer
   */
  @ValueEquality
  private static class AppendedArityPreparedTransformer<R>
      extends AbstractPreparedStatefulTransformerDynamic<R, Object, AppendedArityPreparedTransformer<R>> {
    private static final long serialVersionUID = 1;

    private final PreparedTransformer<? extends R> _prepared;

    /**
     * Creates a new instance from the specified transformer and additional input.
     *
     * @param prepared the transformer to add an (ignored) input to
     * @param appendedInput the added input
     */
    public AppendedArityPreparedTransformer(PreparedTransformer<? extends R> prepared, Producer<?> appendedInput) {
      super(getAppendedInputs(prepared.internalAPI().getInputList(), appendedInput));
      _prepared = prepared;
    }

    @Override
    protected void applyAll(Object executionCache, List<? extends List<?>> values, List<? super R> results) {
      // the last element in the values list is ignored; applyAllUnsafe(...) ignores spurious additional inputs
      _prepared.internalAPI().applyAllUnsafe(executionCache, values.get(0).size(), values, results);
    }

    @Override
    protected Object createExecutionCache(long exampleCountGuess) {
      return _prepared.internalAPI().createExecutionCache(exampleCountGuess);
    }

    @Override
    protected int getPreferredMinibatchSize() {
      return _prepared.internalAPI().getPreferredMinibatchSize();
    }

    @Override
    protected R apply(Object executionState, List<?> values) {
      // the last element in the values list is ignored; applyUnsafe(...) ignores spurious additional inputs
      return _prepared.internalAPI().applyUnsafe(executionState, values);
    }
  }
}
