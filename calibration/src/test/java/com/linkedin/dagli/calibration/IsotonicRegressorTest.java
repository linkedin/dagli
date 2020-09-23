package com.linkedin.dagli.calibration;

import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class IsotonicRegressorTest {

  /**
   * Convenience method for training an isotonic regression model using a list of values.
   * Uses the index as the x-coordinate and uses 1.0 as the weight.
   */
  private static MonotonicPiecewiseLinearFunction train(double[] y, boolean increasing) {
    return IsotonicRegressor.train(IntStream.range(0, y.length).mapToDouble(i -> i).toArray(), y, increasing);
  }

  private static MonotonicPiecewiseLinearFunction train(double[] y) {
    return train(y, true);
  }

  @Test
  public void testSingleValueModel() {
    MonotonicPiecewiseLinearFunction model = train(new double[]{100});
    assertArrayEquals(model.getPredictions(), new double[]{100}, 0.0);
  }

  @Test
  public void testIsotonicRegressionPredictions() {
    MonotonicPiecewiseLinearFunction model = train(new double[]{1, 2, 3, 1, 6, 17, 16, 17, 18});
    assertArrayEquals(model.getBoundaries(), new double[]{0, 1, 3, 4, 5, 6, 7, 8}, 0.0);
    assertArrayEquals(model.getPredictions(), new double[]{1, 2, 2, 6, 16.5, 16.5, 17.0, 18.0}, 0.0);
    assertTrue(model.isIncreasing());
  }

  @Test
  public void testNonIncreasingRegressionPredictions() {
    MonotonicPiecewiseLinearFunction model = train(new double[]{7, 5, 3, 5, 1}, false);

    double[] predictions = DoubleStream.of(-2.0, -1.0, 0.5, 0.75, 1.0, 2.0, 9.0).map(model).toArray();
    assertArrayEquals(predictions, new double[]{7, 7, 6, 5.5, 5, 4, 1}, 0.0);
  }
}
