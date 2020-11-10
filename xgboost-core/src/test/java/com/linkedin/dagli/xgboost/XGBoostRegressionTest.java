package com.linkedin.dagli.xgboost;

import com.linkedin.dagli.dag.LocalDAGExecutor;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.tester.Tester;
import com.linkedin.dagli.tuple.Tuple2;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class XGBoostRegressionTest {
  // Creates random data.  The target is the product of the entries.
  private static List<Tuple2<Float, DenseFloatArrayVector>> getExamples(int count, int dimensions) {
    Random r = new Random(0);

    ArrayList<Tuple2<Float, DenseFloatArrayVector>> examples = new ArrayList<>(count);

    for (int j = 0; j < count; j++) {
      DenseFloatArrayVector data = DenseFloatArrayVector.wrap(new float[dimensions]);
      float product = 1;

      for (int i = 0; i < dimensions; i++) {
        double val = r.nextDouble();
        product *= val;
        data.put(i, val);
      }

      examples.add(Tuple2.of(product, data));
    }

    return examples;
  }

  @Test
  public void testBasic() {
    trainAndCheck(getExamples(100000, 3), getExamples(100000, 3));
  }

  public void trainAndCheck(List<Tuple2<Float, DenseFloatArrayVector>> trainingData, List<Tuple2<Float, DenseFloatArrayVector>> evalData) {
    Placeholder<Float> floatPlaceholder = new Placeholder<>("Float");
    Placeholder<DenseFloatArrayVector> vecPlaceholder = new Placeholder<>("Vector");
    XGBoostRegression booster = new XGBoostRegression()
        .withLabelInput(Constant.nullValue())
        .withWeightInput(floatPlaceholder)
        .withFeaturesInput(vecPlaceholder)
        .withMaxDepth(5)
        .withRounds(100);

    Tester.of(booster).input(10, 1, DenseFloatArrayVector.wrap(1)).test();

    XGBoostRegression.Prepared prepared = ((XGBoostRegression.Prepared) booster.internalAPI().prepare(
        new LocalDAGExecutor().withMaxThreads(1),
        trainingData.stream().map(v -> (Number) null).collect(Collectors.toList()),
        trainingData.stream().map(Tuple2::get0).collect(Collectors.toList()),
        trainingData.stream().map(Tuple2::get1).collect(Collectors.toList())).getPreparedTransformerForNewData());

    Tester.of(prepared)
        .input(20, 1, evalData.get(0).get1())
        .skipNonTrivialEqualityCheck() // prepared XGBoost transformer does not have robust equals()
        .test();

    for (Tuple2<Float, DenseFloatArrayVector> pair : evalData) {
      assertEquals((float) pair.get0(), prepared.apply(null, pair.get0(), pair.get1()).floatValue(),
          0.05);
    }
  }
}
