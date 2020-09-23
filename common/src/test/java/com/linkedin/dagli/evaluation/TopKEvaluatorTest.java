package com.linkedin.dagli.evaluation;

import com.linkedin.dagli.dag.LocalDAGExecutor;
import com.linkedin.dagli.preparer.Preparer3;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.transformer.ConstantResultTransformation3;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class TopKEvaluatorTest {
  @Test
  public void test() {
    // use a factory just for the sake of testing the factory
    TopKEvaluation evaluator = new TopKEvaluation().withK(3);
    Preparer3<
        Number,
        Object,
        Iterable<?>,
        RankingEvaluationResult,
        ConstantResultTransformation3.Prepared<Number, Object, Iterable<?>, RankingEvaluationResult>>
        preparer = evaluator.getPreparer(PreparerContext.builder(4).setExecutor(new LocalDAGExecutor()).build());

    preparer.process(1, 1, Arrays.asList(2, 3));
    preparer.process(1, 1, Collections.emptyList());
    preparer.process(1, 1, Arrays.asList(2, 1));
    preparer.process(1, 1, Arrays.asList(2, 3, 4, 1));

    RankingEvaluationResult evaluation = preparer.finish(null).getPreparedTransformerForNewData().apply(0, null, null);

    Assertions.assertEquals(1, evaluation.getNoPredictionCount());
    Assertions.assertEquals(1, evaluation.getNoPredictionWeight());
    Assertions.assertEquals(0.5 / 4, evaluation.getUnweightedMeanReciprocalRank());
    Assertions.assertEquals(0.5 / 4, evaluation.getWeightedMeanReciprocalRank());
    Assertions.assertEquals(2, evaluation.getIncorrectCount());  // failure to predict does not count as
    Assertions.assertEquals(2, evaluation.getIncorrectWeight()); // incorrect, hence this is 2 rather than 3
    Assertions.assertEquals(0.25, evaluation.getWeightedAccuracy());
    Assertions.assertEquals(1, evaluation.getCorrectCount());
    Assertions.assertEquals(1, evaluation.getCorrectWeight());
    Assertions.assertEquals(4, evaluation.getTotalCount());
    Assertions.assertEquals(4, evaluation.getTotalWeight());
    Assertions.assertEquals(0.25, evaluation.getUnweightedAccuracy());
    Assertions.assertEquals(0, evaluation.compareTo(evaluation));

    // make sure this doesn't throw:
    evaluation.getSummary();
  }
}
