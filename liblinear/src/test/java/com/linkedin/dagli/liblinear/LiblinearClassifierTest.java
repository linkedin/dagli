package com.linkedin.dagli.liblinear;

import com.linkedin.dagli.dag.LocalDAGExecutor;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.tester.Tester;
import com.linkedin.dagli.tuple.Tuple2;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class LiblinearClassifierTest {
  // Creates random data.  The label of the (binary) vector is the index of the single "on" feature
  private List<Tuple2<Integer, DenseFloatArrayVector>> getTrinaryExamples(int count) {
    Random rand = new Random(0);
    ArrayList<Tuple2<Integer, DenseFloatArrayVector>> result = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      int on = rand.nextInt(3);
      DenseFloatArrayVector vec = DenseFloatArrayVector.wrap(new float[]{
          on == 0 ? 1.0f : 0.f, on == 1 ? 1.0f : 0.f, on == 2 ? 1.0f : 0.f});
      result.add(Tuple2.of(on, vec));
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

  // Creates random degenerate data with arbitrary features and label 0
  private List<Tuple2<Integer, DenseFloatArrayVector>> getUnaryExamples(int count) {
    Random rand = new Random(0);
    ArrayList<Tuple2<Integer, DenseFloatArrayVector>> result = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      DenseFloatArrayVector vec = DenseFloatArrayVector.wrap(new float[]{ 1, rand.nextFloat(), rand.nextFloat() });
      result.add(Tuple2.of(0, vec));
    }

    return result;
  }

  @Test
  public void testBasic() {
    LiblinearClassification.Prepared prepared = trainAndCheck(getTrinaryExamples(200), getTrinaryExamples(100));

    for (int i = 0; i < 3; i++) {
      assertTrue(prepared.getBiasForLabel(i) < -0.1);
      Vector weights = prepared.getWeightsForLabel(i);
      assertEquals(StreamSupport.stream(weights.spliterator(), false).max(Comparator.naturalOrder()).get().getIndex(),
          i);
    }

    prepared = trainAndCheck(getBinaryExamples(200), getBinaryExamples(100));
    assertEquals(prepared.getWeightsForLabelIndex(0).get(0), -prepared.getWeightsForLabelIndex(1).get(0), 0.00001);
    assertTrue(prepared.getWeightsForLabel(1).get(0) > 0);
    assertTrue(prepared.getBiasForLabel(1) < 0);

    prepared = trainAndCheck(getUnaryExamples(200), getUnaryExamples(100));
    assertEquals(prepared.getWeightsForLabelIndex(0).get(0), -prepared.getWeightsForLabelIndex(1).get(0), 0.00001);
    assertTrue(prepared.getWeightsForLabel(0).get(0) > 0);
    assertTrue(prepared.getBiasForLabel(0) > 0);
  }

  public LiblinearClassification.Prepared trainAndCheck(List<Tuple2<Integer, DenseFloatArrayVector>> trainingData,
      List<Tuple2<Integer, DenseFloatArrayVector>> evalData) {
    Placeholder<Integer> intPlaceholder = new Placeholder<>("Integer");
    Placeholder<DenseFloatArrayVector> vecPlaceholder = new Placeholder<>("Vector");
    LiblinearClassification<Integer> liblinear = new LiblinearClassification<Integer>()
        .withLabelInput(intPlaceholder)
        .withFeatureInput(vecPlaceholder)
        .withLikelihoodVersusRegularizationLossMultiplier(1)
        .withBias(1);

    Tester.of(liblinear).input(1, DenseFloatArrayVector.wrap(4)).test();

    LiblinearClassification.Prepared<Integer> prepared = liblinear.internalAPI()
        .prepare(new LocalDAGExecutor().withMaxThreads(1),
            trainingData.stream().map(Tuple2::get0).collect(Collectors.toList()),
            trainingData.stream().map(Tuple2::get1).collect(Collectors.toList()))
        .getPreparedTransformerForNewData();

    Tester.of(prepared).input(1, DenseFloatArrayVector.wrap(4)).test();

    for (Tuple2<Integer, DenseFloatArrayVector> pair : evalData) {
      assertEquals(pair.get0(), prepared.apply(pair.get0(), pair.get1()).max().get().getLabel());
    }

    return prepared;
  }
}
