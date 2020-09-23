package com.linkedin.dagli.evaluation;

import com.linkedin.dagli.tester.Tester;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link BinaryEvaluation} class.
 */
public class BinaryEvaluatorTest {
  @Test
  public void testEvaluator() {
    Tester.of(new BinaryEvaluation())
        .input(1.0, true, 0)
        .input(2.0, false, 0)
        .input(2.0, false, 0.2)
        .input(1.0, true, 0.4)
        .input(1.0, false, 0.6)
        .input(3.0, true, 0.8)
        .preparedTransformerTester(prepared -> {
          BinaryEvaluationResult eval = prepared.apply(null, null, null);
          Assertions.assertEquals(6, eval._confusionMatrices.size64());
          Assertions.assertEquals(0.8, eval.getAUC());
          Assertions.assertEquals(0.86, eval.getAveragePrecision());
          Assertions.assertEquals(0.7, eval.getConfusionMatrixAtThreshold(0.5).getAccuracy());
          Assertions.assertEquals(0.5, eval.getConfusionMatrixAtThreshold(1.0).getAccuracy());
          Assertions.assertEquals(0.5, eval.getConfusionMatrixAtThreshold(0.0).getAccuracy());
        }).test();
  }
}
