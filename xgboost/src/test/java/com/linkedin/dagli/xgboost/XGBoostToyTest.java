package com.linkedin.dagli.xgboost;

import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG1x1;
import com.linkedin.dagli.dag.DAG2x1;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import com.linkedin.dagli.util.collection.LinkedNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class XGBoostToyTest {
  private static final int FEATURE_COUNT = 5;

  private static List<DenseFloatArrayVector> getData(int count, long seed) {
    ArrayList<DenseFloatArrayVector> result = new ArrayList<>(count);
    Random r = new Random(seed);

    for (int i = 0; i < count; i++) {
      float[] vec = new float[FEATURE_COUNT];
      for (int j = 0; j < vec.length; j++) {
        vec[j] = r.nextBoolean() ? 1 : 0;
      }

      result.add(DenseFloatArrayVector.wrap(vec));
    }

    return result;
  }

  private static boolean getLabel(DenseFloatArrayVector vector) {
    return vector.norm(1) % 2 == 0;
  }

  private static List<String> getLabels(List<DenseFloatArrayVector> vectors) {
    return vectors.stream().map(vec -> getLabel(vec) ? "YES" : "NO").collect(Collectors.toList());
  }

  //public static void main(String[] args) {
  @Test
  public void test() {
    Placeholder<DenseFloatArrayVector> vectorPlaceholder = new Placeholder<>();
    Placeholder<String> labelPlaceholder = new Placeholder<>();

    XGBoostClassification<String> xgBoostClassification =
        new XGBoostClassification<String>().withFeatureInput(vectorPlaceholder).withLabelInput(labelPlaceholder)
            .withThreadCount(1);

    DAG2x1<String, DenseFloatArrayVector, DiscreteDistribution<String>> dag =
        DAG.withPlaceholders(labelPlaceholder, vectorPlaceholder).withOutput(xgBoostClassification);
    
    List<DenseFloatArrayVector> data = getData(100000, 0);
    List<String> labels = getLabels(data);
    DAG1x1.Prepared<DenseFloatArrayVector, DiscreteDistribution<String>> preparedDAG =
        dag.prepare(labels, data).withGeneratorAsInput1(Constant.nullValue());
    
    List<DenseFloatArrayVector> evalData = getData(100000, 1);
    for (DenseFloatArrayVector vec : evalData) {
      preparedDAG.apply(vec);
    }
  }

  @Test
  public void testEarlyStopping() {
    Placeholder<DenseFloatArrayVector> vectorPlaceholder = new Placeholder<>();
    Placeholder<String> labelPlaceholder = new Placeholder<>();

    XGBoostClassification<String> xgBoostClassification =
        new XGBoostClassification<String>().withFeatureInput(vectorPlaceholder).withLabelInput(labelPlaceholder)
            .withEarlyStopping(true)
            .withRounds(100)
            .withThreadCount(1);

    DAG2x1<String, DenseFloatArrayVector, DiscreteDistribution<String>> dag =
        DAG.withPlaceholders(labelPlaceholder, vectorPlaceholder).withOutput(xgBoostClassification);
    
    List<DenseFloatArrayVector> data = getData(100000, 0);
    List<String> labels = getLabels(data);
    DAG2x1.Prepared<String, DenseFloatArrayVector, DiscreteDistribution<String>> preparedDAG =
        dag.prepare(labels, data);

    // Take advantage of the fact that we expect training to end after two rounds, leaving the "version"
    // less than 100 (which is actually ~50 rounds).
    assertTrue(preparedDAG.producers(XGBoostClassification.Prepared.class)
        .map(LinkedNode::getItem)
        .findFirst()
        .get()
        .getBooster()
        .getVersion() < 100);
  }
}