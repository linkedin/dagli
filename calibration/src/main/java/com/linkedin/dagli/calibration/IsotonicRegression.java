package com.linkedin.dagli.calibration;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.preparer.AbstractStreamPreparer3;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparableTransformer3;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer3;
import java.util.ArrayList;


/**
 * Transformer for Isotonic Regression.
 * Will fit a monotonic, piecewise linear, 1-dimensional function to a data set.
 *
 * Cannot yet be used in DAGs with more than {@link Integer#MAX_VALUE} examples.
 *
 * @author dgolland
 */
@ValueEquality
public class IsotonicRegression
    extends AbstractPreparableTransformer3<Number, Number, Number, Double, IsotonicRegression.Prepared, IsotonicRegression> {

  private static final long serialVersionUID = 1;
  private boolean _increasing = true;

  /**
   * Creates a new instance.
   */
  public IsotonicRegression() {
    super();
    _input1 = new Constant<>(1);
  }

  @Override
  protected Preparer getPreparer(PreparerContext context) {
    return new Preparer(context, _increasing);
  }

  /**
   * Sets whether or not the regressed function will be monotonically increasing (true) or monotonically decreasing
   * (false).
   *
   * The value is true by default (monotonically increasing).
   *
   * @param increasing true if a monotonically increasing function should be regressed, false if a monotonically
   *                   decreasing function should be regressed
   * @return a copy of this instance that is monotonically increasing or decreasing as specified
   */
  public IsotonicRegression withIncreasing(boolean increasing) {
    return clone(c -> c._increasing = increasing);
  }

  /**
   * Sets the input that will provide the weight for each example (must be > 0).  This allows some examples to be have
   * greater impact on the learned function than others.
   *
   * By default, this input is a constant that provides a weight of 1.0 for all examples.
   *
   * @param weightInput the input that will supply the example weights
   * @return a copy of this instance that uses the specified input
   */
  public IsotonicRegression withWeightInput(Producer<? extends Number> weightInput) {
    return clone(c -> c._input1 = weightInput);
  }

  /**
   * Sets the input that will provide the values to be regressed.  The model will learn a function that maps x values
   * to y values that are as close as possible to the provided y values, with the constraint that increasing x values
   * map to monotonically increasing y values.
   *
   * @param xValueInput the input that will supply the values that the function will map from
   * @return a copy of this instance that uses the specified input
   */
  public IsotonicRegression withXValueInput(Producer<? extends Number> xValueInput) {
    return clone(c -> c._input2 = xValueInput);
  }

  /**
   * Sets the input that will provide the "labels" for isotonic regression.  These are the values that the model will
   * try to predict given the x values, subject to the constraint that these predictions monotonically increase
   * with the x values.
   *
   * @param yValueInput the input that will supply the "labels" to be predicted
   * @return a copy of this instance that uses the specified input
   */
  public IsotonicRegression withYValueInput(Producer<? extends Number> yValueInput) {
    return clone(c -> c._input3 = yValueInput);
  }

  /**
   * The preparer that actually performs the isotonic regression and produces the {@link Prepared} transformer.
   */
  private static class Preparer extends AbstractStreamPreparer3<Number, Number, Number, Double, Prepared> {
    private final boolean _increasing;
    private final ArrayList<double[]> _datapoints;

    /**
     * Creates a new instance of the preparer.
     *
     * @param context the {@link PreparerContext}
     * @param increasing whether or not the learned function will be mononically increasing (true) or decreasing (false)
     */
    public Preparer(PreparerContext context, boolean increasing) {
      _datapoints = new ArrayList<>((int) Math.min(Integer.MAX_VALUE, context.getEstimatedExampleCount()));
      _increasing = increasing;
    }

    @Override
    public PreparerResult<Prepared> finish() {
      return new PreparerResult<>(
          new Prepared(IsotonicRegressor.train(_datapoints.toArray(new double[0][]), _increasing)));
    }

    @Override
    public void process(Number weight, Number x, Number y) {
      double[] row = new double[3];
      row[0] = y.doubleValue();
      row[1] = x.doubleValue();
      row[2] = weight.doubleValue();
      _datapoints.add(row);
    }
  }

  /**
   * A prepared Isotonic Regressor representing a piecewise linear function with the guarantee that either:
   * f(x) >= f(x') if x > x' (if monotonically increasing) or
   * f(x) <= f(x') if x > x' (if monotonically decreasing)
   */
  @ValueEquality
  public static class Prepared extends AbstractPreparedTransformer3<Number, Number, Number, Double, Prepared> {
    private static final long serialVersionUID = 1;

    private final MonotonicPiecewiseLinearFunction _monotonicPiecewiseLinearFunction;

    /**
     * Creates a new instance from the provided piecewise linear function.
     *
     * @param function the function wrapped by this transformer
     */
    Prepared(MonotonicPiecewiseLinearFunction function) {
      _monotonicPiecewiseLinearFunction = function;
    }

    @Override
    public Double apply(Number unusedWeight, Number x, Number unusedY) {
      return _monotonicPiecewiseLinearFunction.applyAsDouble(x.doubleValue());
    }
  }
}


