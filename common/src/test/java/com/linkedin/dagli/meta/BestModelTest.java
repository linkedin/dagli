package com.linkedin.dagli.meta;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG3x1;
import com.linkedin.dagli.evaluation.MultinomialEvaluation;
import com.linkedin.dagli.function.FunctionResult1;
import com.linkedin.dagli.function.FunctionResult2;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.tester.Tester;
import com.linkedin.dagli.transformer.TriviallyPreparable;
import com.linkedin.dagli.util.function.Function1;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;


public class BestModelTest {
  private static void getXorData(List<Integer> a, List<Integer> b, List<Integer> r) {
    Random rand = new Random(1337);
    for (int i = 0; i < 1000; i++) {
      int ai = rand.nextBoolean() ? 1 : 0;
      int bi = rand.nextBoolean() ? 1 : 0;
      int ri = ai ^ bi;

      a.add(ai);
      b.add(bi);
      r.add(ri);
    }
  }

  private static int xorFunction(int a, int b) {
    return a ^ b;
  }

  @ParameterizedTest
  @EnumSource(BestModel.PreparationDataInferenceMode.class)
  public void test(BestModel.PreparationDataInferenceMode preparationDataInferenceMode) {
    List<Integer> a = new ArrayList<>();
    List<Integer> b = new ArrayList<>();
    List<Integer> r = new ArrayList<>();

    getXorData(a, b, r);

    Placeholder<Integer> sa = new Placeholder<>();
    Placeholder<Integer> sb = new Placeholder<>();
    Placeholder<Integer> sr = new Placeholder<>();

    BestModel<Integer> bestModel = new BestModel<Integer>()
        .withSplitCount(4)
        .withSeed(1337)
        .withPreparationDataInferenceMode(preparationDataInferenceMode)
        .withEvaluator(new MultinomialEvaluation().withActualLabelInput(sr)::withPredictedLabelInput)
        .withCandidate(new TriviallyPreparable<>(
            new FunctionResult2<Integer, Integer, Integer>().withFunction(BestModelTest::xorFunction).withInputs(sa, sb)))
        .withCandidate(new TriviallyPreparable<>(
            new FunctionResult1<Integer, Integer>().withFunction(Function1.identity()).withInput(sa)));

    // basic tests of the naked preparable
    Tester.of(bestModel).keepOriginalParents().test();

    DAG3x1<Integer, Integer, Integer, Integer> dag = DAG.withPlaceholders(sa, sb, sr).withOutput(bestModel);

    Tester.of(dag)
        .allParallelInputs(a, b, r)
        .preparedTransformerTester(prepared -> Tester.of(prepared)
            .input(1, 1, null)
            .output(0)
            .input(0, 1, null)
            .output(1)
            .input(1, 0, null)
            .output(1)
            .input(0, 0, null)
            .output(0)
            .test())
        .test();
  }
}
