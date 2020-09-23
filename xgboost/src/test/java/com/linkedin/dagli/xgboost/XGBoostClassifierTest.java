package com.linkedin.dagli.xgboost;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG3x1;
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


public class XGBoostClassifierTest {
  // Creates random data.  The label of the (binary) vector is the number of "true" (1) entries.
  private static List<Tuple2<Integer, DenseFloatArrayVector>> getQuadnaryExamples(int count) {
    Random rand = new Random(0);
    ArrayList<Tuple2<Integer, DenseFloatArrayVector>> result = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      DenseFloatArrayVector vec = DenseFloatArrayVector.wrap(new float[]{
          rand.nextBoolean() ? 1.0f : 0.f, rand.nextBoolean() ? 1.0f : 0.f, rand.nextBoolean() ? 1.0f : 0.f});
      result.add(Tuple2.of((int) vec.size64(), vec));
    }

    return result;
  }

  // Creates random data.  The label of the (binary) vector is just the value of the single feature.
  private List<Tuple2<Integer, DenseFloatArrayVector>> getBinaryExamples(int count) {
    Random rand = new Random(0);
    ArrayList<Tuple2<Integer, DenseFloatArrayVector>> result = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      DenseFloatArrayVector vec = DenseFloatArrayVector.wrap(new float[]{rand.nextBoolean() ? 1.0f : 0.f});
      result.add(Tuple2.of((int) vec.size64(), vec));
    }

    return result;
  }

  // Creates random degenerate data with no features and label 0
  private List<Tuple2<Integer, DenseFloatArrayVector>> getUnaryExamples(int count) {
    Random rand = new Random(0);
    ArrayList<Tuple2<Integer, DenseFloatArrayVector>> result = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      DenseFloatArrayVector vec = DenseFloatArrayVector.wrap(new float[]{ });
      result.add(Tuple2.of(0, vec));
    }

    return result;
  }

  @Test
  public void testBasic() {
    trainAndCheck(getQuadnaryExamples(200), getQuadnaryExamples(100), 4);
    trainAndCheck(getBinaryExamples(200), getBinaryExamples(100), 2);
    trainAndCheck(getUnaryExamples(200), getUnaryExamples(100), 1);
  }

  public void trainAndCheck(List<Tuple2<Integer, DenseFloatArrayVector>> trainingData,
      List<Tuple2<Integer, DenseFloatArrayVector>> evalData, int labelCount) {

    Placeholder<Integer> labelPlaceholder = new Placeholder<>("Label");
    Placeholder<Integer> weightPlaceholder = new Placeholder<>("Weight");
    Placeholder<DenseFloatArrayVector> featuresPlaceholder = new Placeholder<>("Features");
    XGBoostClassification<Integer> booster =
        new XGBoostClassification<Integer>()
            .withLabelInput(labelPlaceholder)
            .withWeightInput(weightPlaceholder)
            .withFeatureInput(featuresPlaceholder)
            .withRounds(4)
            .withMaxDepth(3);

    Tester.of(booster).input(10, 1, DenseFloatArrayVector.wrap(1)).test();

    XGBoostClassification.Prepared<Integer>
        prepared = (XGBoostClassification.Prepared<Integer>) booster.internalAPI().prepare(
        new LocalDAGExecutor().withMaxThreads(1),
        trainingData.stream().map(v -> (Number) null).collect(Collectors.toList()),
        trainingData.stream().map(Tuple2::get0).collect(Collectors.toList()),
        trainingData.stream().map(Tuple2::get1).collect(Collectors.toList())).getPreparedTransformerForNewData();

    // skip non-trivial equality check because prepared XGBoost model does not have robust equals()
    Tester.of(prepared).input(20, 1, evalData.get(0).get1()).skipNonTrivialEqualityCheck().test();

    for (Tuple2<Integer, DenseFloatArrayVector> pair : evalData) {
      assertEquals(pair.get0(), prepared.apply(null, pair.get0(), pair.get1()).max().get().getLabel());
    }

    // now let's test getting the leaves out of the final model
    DAG3x1.Result<Integer, Integer, DenseFloatArrayVector, int[]> leavesDAGResult =
        DAG.withPlaceholders(weightPlaceholder, labelPlaceholder, featuresPlaceholder)
            .withOutput(booster.asLeafIDArray())
            .prepareAndApply(trainingData.stream().map(v -> (Integer) null).collect(Collectors.toList()),
                trainingData.stream().map(Tuple2::get0).collect(Collectors.toList()),
                trainingData.stream().map(Tuple2::get1).collect(Collectors.toList()));

    // make sure we're getting the expected number of leaves
    int treeCount = (labelCount > 2 ? labelCount : 1) * booster.getRounds();
    for (int[] arr : leavesDAGResult.toList()) {
      assertEquals(arr.length, treeCount);
    }
  }

  public static XGBoostClassification.Prepared<Integer> getTrainedModel() {
    List<Tuple2<Integer, DenseFloatArrayVector>> trainingData = getQuadnaryExamples(200);

    Placeholder<Integer> intPlaceholder = new Placeholder<>("Integer");
    Placeholder<DenseFloatArrayVector> vecPlaceholder = new Placeholder<>("Vector");
    XGBoostClassification<Integer> booster =
        new XGBoostClassification<Integer>()
            .withLabelInput(Constant.nullValue())
            .withWeightInput(intPlaceholder)
            .withFeatureInput(vecPlaceholder)
            .withMaxDepth(3);

    Tester.of(booster).input(10, 1, DenseFloatArrayVector.wrap(1)).test();

    return (XGBoostClassification.Prepared<Integer>) booster.internalAPI().prepare(
        new LocalDAGExecutor().withMaxThreads(1),
        trainingData.stream().map(v -> (Number) null).collect(Collectors.toList()),
        trainingData.stream().map(Tuple2::get0).collect(Collectors.toList()),
        trainingData.stream().map(Tuple2::get1).collect(Collectors.toList())).getPreparedTransformerForNewData();
  }
}
