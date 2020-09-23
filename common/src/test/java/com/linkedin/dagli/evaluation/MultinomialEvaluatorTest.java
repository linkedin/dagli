package com.linkedin.dagli.evaluation;

import com.linkedin.dagli.dag.LocalDAGExecutor;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.Preparer3;
import com.linkedin.dagli.transformer.ConstantResultTransformation3;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class MultinomialEvaluatorTest {
  @Test
  public void testEvaluator() {
    MultinomialEvaluation evaluator = new MultinomialEvaluation();
    Preparer3<Number, Object, Object, MultinomialEvaluationResult, ConstantResultTransformation3.Prepared<Number, Object, Object, MultinomialEvaluationResult>>
        preparer = evaluator.getPreparer(PreparerContext.builder(2).setExecutor(new LocalDAGExecutor()).build());

    preparer.process(1, 1, 2);
    preparer.process(1, 1, 1);
    MultinomialEvaluationResult evaluation = preparer.finish(null).getPreparedTransformerForNewData().apply(0, 0, 0);

    Assertions.assertEquals(2, evaluation.getActualLabelToWeightAndCountMap().get(1).getCount());
    Assertions.assertEquals(1, evaluation.getPredictedLabelToWeightAndCountMap().get(1).getWeight());
    Assertions.assertEquals(0.5, evaluation.getWeightedAccuracy());
    Assertions.assertEquals(0.5, evaluation.getUnweightedAccuracy());
    Assertions.assertEquals(1, evaluation.getCorrectCount());
    Assertions.assertEquals(1, evaluation.getCorrectWeight());
    Assertions.assertEquals(2, evaluation.getTotalCount());
    Assertions.assertEquals(2, evaluation.getTotalWeight());
    Assertions.assertEquals(0, evaluation.compareTo(evaluation));

    // make sure this doesn't throw:
    evaluation.getSummary();
  }
}
