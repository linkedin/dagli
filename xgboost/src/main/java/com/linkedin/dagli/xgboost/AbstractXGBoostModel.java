package com.linkedin.dagli.xgboost;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.function.FunctionResult1;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.preparer.AbstractStreamPreparer3;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparableTransformer3;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer2;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer3;
import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.tuple.Tuple3;
import com.linkedin.dagli.util.invariant.Arguments;
import com.linkedin.dagli.util.array.ArraysEx;
import com.linkedin.dagli.vector.CategoricalFeatureVector;
import com.linkedin.dagli.vector.DensifiedVector;
import com.linkedin.dagli.view.PreparedTransformerView;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import ml.dmlc.xgboost4j.LabeledPoint;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;


/**
 * Gradient Boosted Decision Tree model.
 *
 * @param <L> the type of label
 * @param <R> the type of result produced by the model
 * @param <S> the derived class
 * @param <P> the type of prepared transformer that will result from training
 */
abstract class AbstractXGBoostModel<
    L, R, P extends AbstractXGBoostModel.Prepared<L, R, ?>, S extends AbstractXGBoostModel<L, R, P, S>>
    extends AbstractPreparableTransformer3<Number, L, DenseVector, R, P, S> {

  private static final long serialVersionUID = 1;

  private static final int MISSING_ID_MARKER = -1;

  /**
   * Types of objective functions used by XGBoost.  These are not necessarily exhaustive, but are rather the ones
   * presently in use.
   */
  protected enum XGBoostObjectiveType {
    REGRESSION,
    CLASSIFICATON
  }

  /**
   * Objective functions used by XGBoost.  These determine whether and how the model performs regression or
   * classification.
   */
  protected enum XGBoostObjective {
    REGRESSION_SQUARED_ERROR("reg:squarederror", XGBoostObjectiveType.REGRESSION, false),
    CLASSIFICATION_SOFTMAX("multi:softprob", XGBoostObjectiveType.CLASSIFICATON, true),
    CLASSIFICATION_LOGISTIC_REGRESSION("binary:logistic", XGBoostObjectiveType.CLASSIFICATON, false);

    private String _objectiveName;
    private XGBoostObjectiveType _type;
    private boolean _shouldSpecifyNumberOfClasses;

    public String getObjectiveName() {
      return _objectiveName;
    }

    public XGBoostObjectiveType getType() {
      return _type;
    }

    public boolean shouldSpecifyNumberOfClasses() {
      return _shouldSpecifyNumberOfClasses;
    }

    XGBoostObjective(String objectiveName, XGBoostObjectiveType type, boolean shouldSpecifyNumberOfClasses) {
      _objectiveName = objectiveName;
      _type = type;
      _shouldSpecifyNumberOfClasses = shouldSpecifyNumberOfClasses;
    }
  }

  protected double _learningRateMultiplier = 0.3;
  protected int _maxDepth = 3;
  protected boolean _silent = true;
  protected int _rounds = 4;
  protected int _threadCount = -1; // <= 0 means "use the number of logical cores"
  protected int _earlyStoppingRounds = -1; // <= 0 means "no early stopping"

  /**
   * Creates a new instance.  The weight input is initially set to a null generator, such that all examples will
   * have equal weight unless a weight input is (optionally) specified by the client.
   */
  public AbstractXGBoostModel() {
    super(Constant.nullValue(), MissingInput.get(), MissingInput.get());
  }

  /**
   * Sets the input that will provide the weight for each example.  If not set, each example is assumed to have equal
   * weight.
   *
   * @param weightInput the input providing the per-example weights
   * @return a copy of this instance that will use the specified input
   */
  public S withWeightInput(Producer<? extends Number> weightInput) {
    return clone(c -> c._input1 = weightInput);
  }

  /**
   * Sets the input that will provide the label for each example.
   *
   * @param labelInput the input providing the label for each example
   * @return a copy of this instance that will use the specified input
   */
  public S withLabelInput(Producer<? extends L> labelInput) {
    return clone(c -> c._input2 = labelInput);
  }

  /**
   * Returns a copy of this instance that will use the features for each example provided as {@link DenseVector}s by the
   * given producer.
   *
   * @param featureInput the input providing the features, expressed as a dense vector.
   * @return a copy of this instance that will use the specified input
   */
  public S withFeatureInput(Producer<? extends DenseVector> featureInput) {
    return clone(c -> c._input3 = featureInput);
  }

  /**
   * Returns a copy of this instance that will use the features for each example provided as (sparse) {@link Vector}s by
   * the given producer.
   *
   * @param featureInput the input providing the features, expressed as a sparse vector.
   * @return a copy of this instance that will use the specified input
   */
  public S withSparseFeatureInput(Producer<? extends Vector> featureInput) {
    return withFeatureInput(new DensifiedVector(featureInput));
  }

  /**
   * Gets the learning rate multiplier.  Lower values effectively slow learning, which can result in a better fitting
   * model.  The default is 0.3.
   *
   * @return the learning rate multiplier
   */
  public double getLearningRateMultiplier() {
    return _learningRateMultiplier;
  }

  /**
   * Sets the learning rate multiplier.  Lower values effectively slow learning, which can result in a better fitting
   * model.  The default is 0.3.
   *
   * @param learningRateMultiplier the learning rate multiplier to use (must be > 0)
   * @return a copy of this instance that will use the specified learning rate multiplier
   */
  public S withLearningRateMultiplier(double learningRateMultiplier) {
    Arguments.check(learningRateMultiplier > 0, "Learning rate multiplier must be > 0");
    return clone(c -> c._learningRateMultiplier = learningRateMultiplier);
  }

  /**
   * Gets the maximum depth of each learned tree.  The default is 3.
   *
   * @return the maximum tree depth
   */
  public int getMaxDepth() {
    return _maxDepth;
  }

  /**
   * Sets the maximum depth of each learned tree.  The default is 3.  Deeper trees can learn more complex conjunctions
   * over the features, but are also more liable to overfit.
   *
   * @param maxDepth the maximum tree depth to use, >= 1
   * @return a copy of this instance that will use the specified maximum tree depth
   */
  public S withMaxDepth(int maxDepth) {
    Arguments.check(maxDepth >= 1, "Maximum tree depth must be at least 1");
    return clone(c -> c._maxDepth = maxDepth);
  }

  /**
   * Whether or not XGBoost will output status information to stderr during training
   *
   * @return true if output to stderr will suppressed, false otherwise
   */
  public boolean isSilent() {
    return _silent;
  }

  /**
   * Sets whether or not XGBoost will output status information to stderr during training
   *
   * @param silent true if output to stderr will suppressed, false otherwise
   * @return a copy of this instance with the specified vociferousness
   */
  public S withSilent(boolean silent) {
    return clone(c -> c._silent = silent);
  }

  /**
   * Gets the number of rounds of training that will be performed (modulo early stopping).
   *
   * This is also the number of trees that will be learned, except in the case of multinomial classification (with more
   * than two labels).  In that case, in each round the number of trees learned will equal the number of labels.
   *
   * @return the number of training rounds
   */
  public int getRounds() {
    return _rounds;
  }

  /**
   * Sets the number of rounds of training that will be performed (modulo early stopping).
   *
   * This is also the number of trees that will be learned, except in the case of multinomial classification (with more
   * than two labels).  In that case, in each round the number of trees learned will equal the number of labels.
   *
   * The default is 4.
   *
   * @param rounds the number of training rounds
   * @return a copy of this instance that will train with the specified number of rounds
   */
  public S withRounds(int rounds) {
    return clone(c -> c._rounds = rounds);
  }

  /**
   * Gets the number of threads used for training.  A value <= 0 means that as many threads as there are logical CPU
   * cores will be used.
   *
   * @return the number of threads to be used, or a value <= 0 indicating that the number used will equal the number of
   *         logical cores.
   */
  public int getThreadCount() {
    return _threadCount;
  }

  /**
   * Sets the number of threads that will be used for training.
   *
   * A value <= 0 will request the use of as many threads as there are logical CPU cores.
   *
   * @param threadCount the number of threads to be used
   * @return a copy of this instance, configured to use the requested number of training threads
   */
  public S withThreadCount(int threadCount) {
    return clone(c -> c._threadCount = threadCount);
  }

  /**
   * Sets whether early stopping is used.  If true, training will stop immediately if the loss on the training
   * data worsens (increases).
   *
   * Caution: theoretically you should be able specify the number of rounds over which loss must consistently worsen
   * before stopping.  However, the implementation in XGBoost4j is entirely buggy and does not yet work as advertised.
   * When this is fixed, this method will be deprecated, to be replaced by a withEarlyStoppingRounds(...) method.
   *
   * @return a copy of this instance, with early stopping enabled or disabled as specified
   */
  public S withEarlyStopping(boolean enabled) {
    return clone(c -> c._earlyStoppingRounds = enabled ? 2 : -1);
  }

  /**
   * Determines whether early stopping is enabled.
   *
   * Caution: currently early stopping behavior is simplistic because XGBoost4j has a serious bug.  When this is fixed
   * in a later version, this method will be deprecated in favor of a getEarlyStoppingRounds() method.
   *
   * @return true if training stops if loss on the training data worsens (increases), false otherwise
   */
  public boolean isEarlyStopping() {
    return _earlyStoppingRounds >= 1;
  }

  protected abstract XGBoostObjective getObjective(int labelCount);
  protected abstract XGBoostObjectiveType getObjectiveType();

  protected static LabeledPoint makeDenseLabeledPoint(Number weight, float label, DenseVector vec) {
    float[] vals = vec instanceof DenseFloatArrayVector ? ((DenseFloatArrayVector) vec).getArray() : vec.toFloatArray();

    if (vals.length == 0) {
      // xgboost doesn't like empty feature vectors...
      vals = new float[]{0};
    }

    float weightValue = weight != null ? weight.floatValue() : 1.0f;
    return new LabeledPoint(label, null, vals, weightValue, -1, Float.NaN);
  }

  // Not currently used because XGBoost doesn't play well with large indices, but may be useful in the future:
  protected static LabeledPoint makeSparseLabeledPoint(Number weight, int labelID, Vector vec) {
    int vecSize = Math.toIntExact(vec.size64());
    if (vecSize == 0) {
      // xgboost doesn't like empty feature vectors...
      return new LabeledPoint(labelID, null, new float[]{0});
    }

    int[] indices = new int[vecSize];
    float[] values = new float[vecSize];

    int[] index = new int[1]; // use an array to "box" the int so it can be passed to the lambda below

    vec.forEach((idx, value) -> {
      indices[index[0]] = Long.hashCode(idx);
      values[index[0]] = (float) value;
      index[0]++;
    });

    float weightValue = weight != null ? weight.floatValue() : 1.0f;
    return new LabeledPoint(labelID, indices, values, weightValue, -1, Float.NaN);
  }

  protected abstract P createPrepared(Object2IntOpenHashMap<L> labelMap, Booster booster);

  @Override
  protected Preparer<L, R, P> getPreparer(PreparerContext context) {
    return new Preparer<>(this);
  }

  private static class Preparer<L, R, P extends AbstractXGBoostModel.Prepared<L, R, ?>>
      extends AbstractStreamPreparer3<Number, L, DenseVector, R, P> {
    private final ArrayList<Tuple3<Number, L, DenseVector>> _labeledVectorList = new ArrayList<>();
    private final AbstractXGBoostModel<L, R, P, ?> _owner;

    public Preparer(AbstractXGBoostModel<L, R, P, ?> owner) {
      _owner = owner;
    }

    @Override
    public void process(Number weight, L label, DenseVector features) {
      _labeledVectorList.add(Tuple3.of(weight, label, features));
    }

    @Override
    public PreparerResult<P> finish() {
      boolean isRegression = _owner.getObjectiveType() == XGBoostObjectiveType.REGRESSION;

      Object2IntOpenHashMap<L> labelIDMap = new Object2IntOpenHashMap<>();
      labelIDMap.defaultReturnValue(MISSING_ID_MARKER);

      DMatrix data;

      try {
        data = new DMatrix(_labeledVectorList.stream().map(labeledVector -> {
          final float label;
          if (!isRegression) {
            int labelID = labelIDMap.getInt(labeledVector.get1());
            if (labelID == MISSING_ID_MARKER) { // marker value for "not there"
              labelID = labelIDMap.size();
              labelIDMap.put(labeledVector.get1(), labelID);
            }
            label = labelID;
          } else {
            label = ((Number) labeledVector.get1()).floatValue();
          }

          DenseVector vec = labeledVector.get2();
          return makeDenseLabeledPoint(labeledVector.get0(), label, vec);
        }).iterator(), null);

        // need to set the weights here, too, because there's a bug in the DMatrix constructor whereby weights on the
        // LabeledPoints don't get used.
        float[] weights = new float[_labeledVectorList.size()];
        boolean hasWeights = false;
        for (int i = 0; i < _labeledVectorList.size(); i++) {
          Number num = _labeledVectorList.get(i).get0();
          if (num != null) {
            hasWeights = true;
            weights[i] = num.floatValue();
          } else {
            weights[i] = 1.0f;
          }
        }
        if (hasWeights) {
          data.setWeight(weights);
        }
      } catch (XGBoostError err) {
        // this shouldn't happen, but if it does just rethrow
        throw new RuntimeException(err);
      }

      XGBoostObjective objective = _owner.getObjective(labelIDMap.size());
      HashMap<String, Object> params = new HashMap<>();
      params.put("eta", _owner.getLearningRateMultiplier());
      params.put("max_depth", _owner.getMaxDepth());
      params.put("silent", _owner.isSilent() ? 1 : 0);
      params.put("objective", objective.getObjectiveName());
      params.put("nthread",
          _owner._threadCount <= 0 ? Runtime.getRuntime().availableProcessors() : _owner._threadCount);

      if (objective.shouldSpecifyNumberOfClasses()) {
        params.put("num_class", labelIDMap.size());
      }
      //params.put("num_output_group", 100000000);

      HashMap<String, DMatrix> watches = new HashMap<>();
      watches.put("train", data);

      try {
        //train a boost model
        Booster booster =
            XGBoost.train(data, params, _owner.getRounds(), watches, null, null, null, _owner._earlyStoppingRounds);
        XGBoostModel.IS_THREAD_CONFIGURED_FOR_SINGLE_THREADED_PREDICTION.set(false);
        return new PreparerResult<>(_owner.createPrepared(labelIDMap, booster));
      } catch (XGBoostError err) {
        throw new RuntimeException("Encountered an XGBoostException while training model", err);
      } finally {
        data.dispose(); // free memory allocated by native library immediately
      }
    }
  }

  /**
   * Returns a transformer that, for each example, provides an array of the leaves that are active for each tree in the
   * forest learned by this XGBoost model.
   *
   * These leaves are commonly used as features in other models and provide learned, useful conjunctive features.
   *
   * The number of leaves in the array obtained for each example will match the number of trees in the boosted forest.
   * For binary and regression models, the number of trees matches the number of "rounds" (unless training was subject
   * to early stopping).  However, for multinomial classification models, it is actually the number of rounds times the
   * number of labels, since one tree per label is learned in each round.
   *
   * @return a prepared transformer that will produce an array of the active leaves (one per tree in the boosted forest)
   *         for each example
   */
  public PreparedTransformer<int[]> asLeafIDArray() {
    PreparedTransformerView<Prepared<?, ?, ?>> preparedTransformerView = new PreparedTransformerView<>(this);
    return new XGBoostLeaves(preparedTransformerView, getInput3());
  }

  /**
   * Returns a transformer that, for each example, provides a (sparse) feature vector corresponding to the leaves that
   * are active for each tree in the forest learned by this XGBoost model.  Each leaf in each tree will map to a
   * unique (or nearly so, since the element index is computed via hashing and there is a trivial probability of
   * collision) element in the feature vector; when that leaf is active, the element will have the value 1.
   *
   * The feature vector can be used as an input other models and effectively provides learned conjunctions of the
   * original features.
   *
   * The number of active leaves in for each example will match the number of trees in the boosted forest.
   *
   * For binary and regression models, the number of trees matches the number of "rounds" (unless training was subject
   * to early stopping).  However, for multinomial classification models, it is actually the number of rounds times the
   * number of labels, since one tree per label is learned in each round.
   *
   * @return a prepared transformer that will produce an array of the active leaves (one per tree in the boosted forest)
   *         for each example
   */
  public PreparedTransformer<Vector> asLeafFeatures() {
    return new CategoricalFeatureVector().withInput(
        new FunctionResult1<int[], IntList>(ArraysEx::asList).withInput(asLeafIDArray()));
  }

  /**
   * Given a trained XGBoost model, gets the leaves (one per tree) corresponding to the example.
   */
  @ValueEquality
  private static class XGBoostLeaves
      extends AbstractPreparedTransformer2<Prepared<?, ?, ?>, DenseVector, int[], XGBoostLeaves> {
    private static final long serialVersionUID = 1;

    /**
     * Creates a new instance.  Because this class has private visibility, we don't bother with the niceties of in-place
     * builders and simply pass the inputs directly to the constructor.
     *
     * @param preparedModelInput an input that will provide instances of {@link Prepared}
     * @param denseVectorInput an input that will supply {@link DenseVector}s
     */
    XGBoostLeaves(Producer<? extends Prepared<?, ?, ?>> preparedModelInput,
        Producer<? extends DenseVector> denseVectorInput) {
      super(preparedModelInput, denseVectorInput);
    }

    @Override
    public int[] apply(Prepared<?, ?, ?> value0, DenseVector value1) {
      return ArraysEx.toIntegersLossy(XGBoostModel.predictAsFloats(value0.getBooster(), value1,
          (booster, dmatrix) -> booster.predictLeaf(dmatrix, 0)[0]));
    }
  }

  /**
   * Abstract base class for trained XGBoost models.  Note that unfortunately Booster does not implement
   * equals()/hashCode() so the ability to discern equality for instances of this class is limited.  However, this is
   * not a significant concern since the preparable XGBoost transformers that produce these prepared transformers
   * deduplicate more robustly and it would be unusual to create instances of this class (or a subclass) directly
   * from different-but-logically-equivalent Booster objects.
   *
   * @param <L> the type of the label
   * @param <R> the type of result the prepared XGBoost transformer will produce
   * @param <S> the type of the class ultimately deriving from this class
   */
  protected abstract static class Prepared<L, R, S extends Prepared<L, R, S>>
      extends AbstractPreparedTransformer3<Number, L, DenseVector, R, S> {

    private static final long serialVersionUID = 1;

    /**
     * A {@link Booster} which represents the XGBoost model.  Unfortunately, as of XGBoost4J v.90, Booster does not
     * define a serialVersionUID, so it can unnecessarily break serialization between versions.  This should always be
     * tested when updating to a newer version of XGBoost, even if it is nominally serialization-compatible.
     */
    protected Booster _booster;

    Prepared(Booster booster) {
      _booster = booster;
    }

    /**
     * Gets the underlying XGBoost booster (the trained model).
     *
     * @return the Booster object representing the trained XGBoost model
     */
    public Booster getBooster() {
      return _booster;
    }
  }
}
