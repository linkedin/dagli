package com.linkedin.dagli.calibration;

import com.linkedin.dagli.util.invariant.Arguments;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.DoubleUnaryOperator;


/**
 * The class represents a monotonic, piecewise-linear function using an array of boundary points with corresponding
 * values. The function can be monotonically increasing or decreasing, depending on the value of the corresponding
 * argument.
 *
 * @author dgolland
 */
class MonotonicPiecewiseLinearFunction implements DoubleUnaryOperator, Serializable {
  private static final long serialVersionUID = 1;

  private final double[] _boundaries;
  private final double[] _predictions;
  private final boolean _increasing;

  MonotonicPiecewiseLinearFunction(double[] boundaries, double[] predictions, boolean increasing) {
    Arguments.check(boundaries.length > 0, "Input cannot be empty");
    Arguments.check(boundaries.length == predictions.length,
        "Length of boundaries must match length of predictions");
    Arguments.check(isMonotonicallyIncreasing(boundaries),
        "Boundaries must be non-decreasing.");
    if (increasing) {
      Arguments.check(isMonotonicallyIncreasing(predictions),
          "Predictions must be in monotonically increasing order.");
    } else {
      Arguments.check(isMonotonicallyDecreasing(predictions),
          "Predictions must be in monotonically decreasing order.");
    }
    _boundaries = boundaries;
    _predictions = predictions;
    _increasing = increasing;
  }

  // checks if the values in an array are monotonically increasing
  private static boolean isMonotonicallyIncreasing(double[] array) {
    double last = Double.NEGATIVE_INFINITY;
    for (double dbl : array) {
      if (dbl < last) {
        return false;
      }
      last = dbl;
    }
    return true;
  }

  // checks if the values in an array are monotonically decreasing
  private static boolean isMonotonicallyDecreasing(double[] array) {
    double last = Double.POSITIVE_INFINITY;
    for (double dbl : array) {
      if (dbl > last) {
        return false;
      }
      last = dbl;
    }
    return true;
  }

  /**
   * Finds the linear interpolation of two points.
   *
   * If the two points have the same x coordinate, it is assumed they have the same y coordinate (which will be returned
   * as the interpolation).  This will always be true for the points of a function (as is the case here).
   *
   * @return the linear interpolation between two points.
   */
  static double linearInterpolation(double x1, double y1, double x2, double y2, double x) {
    return y1 + (y2 - y1) * (x - x1) / (x2 - x1); // == y1 if (y2 == y1)
  }

  /**
   * @return a copy of the boundaries defining the change points in the piecewise linear model
   */
  double[] getBoundaries() {
    return Arrays.copyOf(_boundaries, _boundaries.length);
  }

  /**
   * @return the predictions at the boundaries of the piecewise linear model
   */
  double[] getPredictions() {
    return Arrays.copyOf(_predictions, _predictions.length);
  }

  /**
   * @return true if the piecewise linear function is monotonically non-decreasing
   */
  boolean isIncreasing() {
    return _increasing;
  }

  /**
   * Evaluate the function at a given point.
   *
   * If the point is at a boundary point, return the predicted value at that boundary point.
   * Otherwise, if the point is between two boundary points, return the linear interpolation between the adjacent
   * predicted values. If the point is outside the boundary points, return the predicted value at the boundary.
   *
   * @param x the point to evaluate
   * @return The value of the piecewise linear function at the boundary.
   */
  @Override
  public double applyAsDouble(double x) {
    int foundIndex = Arrays.binarySearch(_boundaries, x);
    int insertIndex = -foundIndex - 1;

    // Find if the index was lower than all values,
    // higher than all values, in between two values or exact match.
    if (insertIndex == 0) {
      return _predictions[0];
    } else if (insertIndex == _boundaries.length) {
      return _predictions[_predictions.length - 1];
    } else if (foundIndex < 0) {
      return linearInterpolation(_boundaries[insertIndex - 1], _predictions[insertIndex - 1], _boundaries[insertIndex],
          _predictions[insertIndex], x);
    } else {
      return _predictions[foundIndex];
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MonotonicPiecewiseLinearFunction)) {
      return false;
    }
    MonotonicPiecewiseLinearFunction that = (MonotonicPiecewiseLinearFunction) o;
    return _increasing == that._increasing && Arrays.equals(_boundaries, that._boundaries) && Arrays.equals(
        _predictions, that._predictions);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(_increasing);
    result = 31 * result + Arrays.hashCode(_boundaries);
    result = 31 * result + Arrays.hashCode(_predictions);
    return result;
  }
}
