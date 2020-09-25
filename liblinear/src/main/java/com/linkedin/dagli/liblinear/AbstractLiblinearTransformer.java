package com.linkedin.dagli.liblinear;

import com.jeffreypasternack.liblinear.Model;
import com.jeffreypasternack.liblinear.SolverType;
import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparableTransformer2;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer2;
import com.linkedin.dagli.transformer.PreparedTransformer2;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.function.Supplier;


/**
 * Provides common parameters shared by all (preparable) Liblinear transformers.
 *
 * @param <L> The label type
 * @param <R> The return type of the transformer
 * @param <N> The type of the prepared (trained) liblinear transformer
 * @param <S> The type of the derived class
 */
abstract class AbstractLiblinearTransformer<L, R, N extends PreparedTransformer2<L, Vector, R>, S extends AbstractLiblinearTransformer<L, R, N, S>>
    extends AbstractPreparableTransformer2<L, Vector, R, N, S> {

  private static final long serialVersionUID = 1;

  protected double _bias = 1;
  protected double _likelihoodVersusRegularizationMultiplier = 1;
  protected SolverType _solverType = SolverType.L2R_LR;
  protected double _epsilon = 0.01;
  protected double _svrEpsilonLoss = 0.1;
  protected boolean _silent = false;
  protected int _threadCount = 1;

  /**
   * Specifies the label to use.  Depending on the cardinality of the set of unique labels provided the model will
   * be binary or multinomial.
   *
   * @param labelInput the labels to use
   * @return a copy of this instance that will use the specified labels
   */
  public S withLabelInput(Producer<? extends L> labelInput) {
    return clone(c -> c._input1 = labelInput);
  }

  /**
   * Specifies the features to use (expressed as feature vectors).
   *
   * @param featureInput the features to use
   * @return a copy of this instance that will use the specified features
   */
  public S withFeatureInput(Producer<? extends Vector> featureInput) {
    return clone(c -> c._input2 = featureInput);
  }

  protected <T extends AbstractLiblinearTransformer<L, ?, ?, ?>> T copyTo(Supplier<T> newInstanceSupplier) {
    T result = newInstanceSupplier.get();
    result._bias = _bias;
    result._likelihoodVersusRegularizationMultiplier = _likelihoodVersusRegularizationMultiplier;
    result._solverType = _solverType;
    result._epsilon = _epsilon;
    result._svrEpsilonLoss = _svrEpsilonLoss;
    result._silent = _silent;
    result._input1 = _input1;
    result._input2 = _input2;

    return result;
  }

  boolean isSilent() {
    return _silent;
  }

  /**
   * Disables or enables Liblinear's status updates to stderr.  Defaults to false (enabled).
   * Note that this manipulates a global property of Liblinear (a limitation of its implementation) and multiple
   * Liblinear instances with a mix of silence enabled and disabled will have unpredictable effects.
   *
   * @param silent whether or not LibLinear will spam stderr with updates
   * @return a new LiblinearBuilder with silent set as desired.
   */
  public S withSilent(boolean silent) {
    return clone(c -> c._silent = silent);
  }

  double getBias() {
    return _bias;
  }

  /**
   * Sets the bias that will be used; we will automatically add this as an extra feature to all examples when
   * {@code bias >= 0}.  The default is 1.
   *
   * @param bias the bias to use, or {@code < 0} to not automatically add any bias.
   */
  public S withBias(double bias) {
    return clone(c -> c._bias = bias);
  }

  double getLikelihoodVersusRegularizationLossMultiplier() {
    return _likelihoodVersusRegularizationMultiplier;
  }

  /**
   * Sets the multiplier for the likelihood in the loss function (Liblinear calls this "C").  Higher values mean
   * that the model cares more about the likelihood of the data, and hence the weights will be less regularized,
   * Typically values are 1 to 1000; the default is 1.
   *
   * @param multiplier the multiplier to use.
   */
  public S withLikelihoodVersusRegularizationLossMultiplier(double multiplier) {
    return clone(c -> c._likelihoodVersusRegularizationMultiplier = multiplier);
  }

  SolverType getSolverType() {
    return _solverType;
  }

  /**
   * Sets the solver type for liblinear; there are many choices, but the default is L2_LR (L2-normalized logistic
   * regression).
   *
   * @param type the type of solver to use.
   */
  public S withSolverType(SolverType type) {
    // Preconditions.checkArgument(type.isLogisticRegressionSolver());
    return clone(c -> c._solverType = type);
  }

  double getEpsilon() {
    return _epsilon;
  }

  /**
   * Sets the epsilon stopping criterion.  The default is 0.01.
   *
   * @param epsilon the epsilon value to use.
   */
  public S withEpsilon(double epsilon) {
    return clone(c -> c._epsilon = epsilon);
  }

  double getSVREpsilonLoss() {
    return _svrEpsilonLoss;
  }

  /**
   * Sets the epsilon loss parameter for epsilon-SVR.  Not relevant for other solver types.  Default value is 0.1.
   * Liblinear calls this parameter "p".
   *
   * @param loss the loss value to use
   */
  public S withSVREpsilonLoss(double loss) {
    return clone(c -> c._svrEpsilonLoss = loss);
  }

  /**
   * Sets the number of threads to use.  Currently, only L2_LR supports multithreading; if another solver type is used
   * this parameter will be ignored.
   *
   * @param threads the number of threads to use
   * @return a copy of this instance with the specified thread count
   */
  public S withThreadCount(int threads) {
    return clone(c -> c._threadCount = threads);
  }

  int getThreadCount() {
    return _threadCount;
  }

  /**
   * Base class for a trained liblinear model.
   *
   * @param <L> the type of label used in training (if regression this will be Double)
   * @param <R> the type of result generated by the model, e.g. a
   *            {@link com.linkedin.dagli.math.distribution.DiscreteDistribution} over the predicted labels
   * @param <S> the type of the derived prepared transformer
   */
  @ValueEquality
  protected static abstract class Prepared<L, R, S extends Prepared<L, R, S>>
      extends AbstractPreparedTransformer2<L, Vector, R, Prepared<L, R, S>> {

    private static final long serialVersionUID = 1;

    protected final double _bias;
    protected final Long2IntOpenHashMap _featureIDMap;
    protected final Model _model;

    Model getModel() {
      return _model;
    }

    Long2IntOpenHashMap getFeatureIDMap() {
      return _featureIDMap;
    }

    Prepared(double bias, Model model, Long2IntOpenHashMap featureIDMap) {
      _bias = bias;
      _model = model;
      _featureIDMap = featureIDMap;
    }
  }
}
