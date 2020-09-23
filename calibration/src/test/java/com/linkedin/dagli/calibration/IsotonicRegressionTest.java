package com.linkedin.dagli.calibration;

import com.linkedin.dagli.dag.LocalDAGExecutor;
import com.linkedin.dagli.tester.Tester;
import com.linkedin.dagli.tuple.Tuple2;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class IsotonicRegressionTest {

  @Test
  public void testBasic() {

    List<Tuple2<Double, Double>> trainingData = new ArrayList<>();
    for (double d : new double[]{7, 5, 3, 5, 1}) {
      trainingData.add(Tuple2.of((double) trainingData.size(), d));
    }

    List<Tuple2<Double, Double>> testData = new ArrayList<>();
    double[] x = {-2.0, -1.0, 0.5, 0.75, 1.0, 2.0, 9.0};
    double[] y = {7, 7, 6, 5.5, 5, 4, 1};
    for (int i = 0; i < x.length; i++) {
      testData.add(Tuple2.of(y[i], x[i]));
    }

    IsotonicRegression isotonic = new IsotonicRegression().withIncreasing(false);

    Tester.of(isotonic).input(1.0, 1.0, 1.0).test();

    IsotonicRegression.Prepared prepared = isotonic.internalAPI()
        .prepare(new LocalDAGExecutor().withMaxThreads(1),
            IntStream.range(0, trainingData.size()).mapToObj(v -> 1.0).collect(Collectors.toList()),
            trainingData.stream().map(Tuple2::get0).collect(Collectors.toList()),
            trainingData.stream().map(Tuple2::get1).collect(Collectors.toList()))
        .getPreparedTransformerForNewData();

    Tester.of(prepared).input(1.0, 1.0, 1.0).test();

    for (Tuple2<Double, Double> pair : testData) {
      assertEquals(pair.get0(), prepared.apply(null, pair.get1(), null));
    }
  }
}