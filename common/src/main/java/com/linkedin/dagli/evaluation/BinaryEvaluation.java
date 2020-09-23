package com.linkedin.dagli.evaluation;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.distribution.LabelProbabilityFromDistribution;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import com.linkedin.dagli.math.hashing.DoubleXorShift;
import com.linkedin.dagli.objectio.ConstantReader;
import com.linkedin.dagli.preparer.AbstractStreamPreparer3;
import com.linkedin.dagli.preparer.Preparer3;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparableTransformer3;
import com.linkedin.dagli.transformer.ConstantResultTransformation3;
import com.linkedin.dagli.transformer.PreparableTransformer3;
import com.linkedin.dagli.util.collection.BigHashMap;
import com.linkedin.dagli.util.collection.Iterables;
import it.unimi.dsi.fastutil.objects.ObjectBigArrayBigList;
import it.unimi.dsi.fastutil.objects.ObjectBigArrays;
import java.util.Map;


/**
 * A preparable transformer that evaluates the scores output by a classifier and the actual (binary) label and produces
 * a {@link BinaryEvaluationResult}.
 *
 * The transformer takes three parameters: the weight of the example, the true label and the classifier's score.  The
 * result of the transformer is a constant (same result for all inputs) {@link BinaryEvaluationResult} instance which
 * can provide a confusion matrix for any postulated decision threshold, ROC plot, AUC, average precision, etc.
 *
 * Note that, because a {@link BinaryEvaluationResult} stores confusion matrices for each distinct predicted score, the
 * memory footprint of this evaluation can potentially be very large.  If this becomes a problem, an easy solution is to
 * quantize your scores (e.g. by rounding scores to the five most significant digits).
 */
@ValueEquality
public class BinaryEvaluation extends AbstractPreparableTransformer3<
    Number,
    Boolean,
    Number,
    BinaryEvaluationResult,
    ConstantResultTransformation3.Prepared<Number, Boolean, Number, BinaryEvaluationResult>, BinaryEvaluation> {

  private static final long serialVersionUID = 1;

  /**
   * Creates a new instance.
   */
  public BinaryEvaluation() {
    super(new Constant<>(1.0), MissingInput.get(), MissingInput.get());
  }

  @Override
  protected boolean hasAlwaysConstantResult() {
    return true;
  }

  @Override
  protected boolean hasIdempotentPreparer() {
    return true;
  }

  /**
   * Creates a copy of this instance that will obtain the weight for each example used to calculate evaluation results
   * from the specified input.  If not set, the default constant value of 1.0 is used.
   *
   * @param input the input that will provide per-example weights
   * @return a copy of this instance that will use the specified input
   */
  public BinaryEvaluation withWeightInput(Producer<? extends Number> input) {
    return withInput1(input);
  }

  /**
   * Creates a copy of this instance that will obtain the actual label for each example from the specified input.
   *
   * Labels are boolean and must not be null.
   *
   * @param input the input that will provide the expected, actual label for each example
   * @return a copy of this instance that will use the specified input
   */
  public BinaryEvaluation withActualLabelInput(Producer<Boolean> input) {
    return withInput2(input);
  }

  /**
   * Creates a copy of this instance that will obtain the predicted score for each example from the specified input.
   *
   * Scores may be arbitrary real values and do not need to, e.g. be {@code [0, 1]}.  However, they will be converted
   * to double values, so, e.g. large long values that are not exactly representable as doubles may be silently
   * truncated.
   *
   * @param input the input that will provide the predicted score for each example
   * @return a copy of this instance that will use the specified input
   */
  public BinaryEvaluation withPredictedScoreInput(Producer<? extends Number> input) {
    return withInput3(input);
  }

  /**
   * Creates a copy of this instance that will accept predicted scores from a provided {@link DiscreteDistribution}
   * input (the score will be taken as the probability of "true").
   *
   * @param input the input that will provide the predicted distribution for each example
   * @return a copy of this instance that will use the specified input
   */
  public BinaryEvaluation withPredictedDistributionInput(Producer<? extends DiscreteDistribution<Boolean>> input) {
    return withPredictedScoreInput(
        new LabelProbabilityFromDistribution<Boolean>().withDistributionInput(input).withLabel(true));
  }

  @Override
  protected Preparer3<Number,
                      Boolean,
                      Number,
                      BinaryEvaluationResult,
                      ConstantResultTransformation3.Prepared<Number, Boolean, Number, BinaryEvaluationResult>>
  getPreparer(PreparerContext context) {
    return new Preparer(context.getEstimatedExampleCount());
  }

  static class Preparer extends AbstractStreamPreparer3<
      Number,
      Boolean,
      Number,
      BinaryEvaluationResult,
      ConstantResultTransformation3.Prepared<Number, Boolean, Number, BinaryEvaluationResult>> {

    private final BigHashMap<Double, double[]> _scoreToWeightSums; // maps scores to sums of negative/positive weight

    Preparer(long estimatedCount) {
      if (estimatedCount < 32) {
        estimatedCount = 32;
      }
      _scoreToWeightSums = new BigHashMap<>(Double.class, Preparer::hashDouble, estimatedCount, 1.0);
    }

    private static long hashDouble(Double val) {
      return DoubleXorShift.hashWithDefaultSeed(Double.doubleToRawLongBits(val));
    }

    @Override
    public void process(Number weight, Boolean actualLabel, Number predictedScore) {
      double[] weights = _scoreToWeightSums.computeIfAbsent(predictedScore.doubleValue(), k -> new double[2]);
      weights[actualLabel ? 1 : 0] += weight.doubleValue();
    }

    @Override
    public PreparerResult<ConstantResultTransformation3.Prepared<Number, Boolean, Number, BinaryEvaluationResult>> finish() {
      long count = _scoreToWeightSums.size64();
      Map.Entry<Double, double[]>[][] entries = _scoreToWeightSums.entrySet().toBigArray();

      // sort the entries by their predicted scores (ascending)
      ObjectBigArrays.quickSort(entries, Map.Entry.comparingByKey());

      // figure out the total positive and negative weights (we'll need this to construct the confusion matrices)
      double positiveWeight = 0;
      double negativeWeight = 0;
      for (Map.Entry<Double, double[]>[] subarray : entries) {
        for (Map.Entry<Double, double[]> entry : subarray) {
          negativeWeight += entry.getValue()[0];
          positiveWeight += entry.getValue()[1];
        }
      }

      // create the list of confusion matrices
      ObjectBigArrayBigList<BinaryConfusionMatrix> confusionMatrices = new ObjectBigArrayBigList<>(count + 1);

      // keep track of accumulated weight below the decision threshold
      double positiveWeightSoFar = 0;
      double negativeWeightSoFar = 0;

      // scan over all entries in increasing order of threshold
      for (Map.Entry<Double, double[]>[] subarray : entries) {
        for (Map.Entry<Double, double[]> entry : subarray) {
          confusionMatrices.add(BinaryConfusionMatrix.Builder
              .setTruePositiveWeight(positiveWeight - positiveWeightSoFar)
              .setFalsePositiveWeight(negativeWeight - negativeWeightSoFar)
              .setTrueNegativeWeight(negativeWeightSoFar)
              .setFalseNegativeWeight(positiveWeightSoFar)
              .setDecisionThreshold(entry.getKey())
              .build());

          negativeWeightSoFar += entry.getValue()[0];
          positiveWeightSoFar += entry.getValue()[1];
        }
      }

      // if there is no confusion matrix corresponding to a threshold of positive infinity (there shouldn't be, but it's
      // not impossible), add it
      if (confusionMatrices.isEmpty() || confusionMatrices.get(confusionMatrices.size64() - 1).getDecisionThreshold() < Double.POSITIVE_INFINITY) {
        confusionMatrices.add(BinaryConfusionMatrix.Builder
            .setTruePositiveWeight(0)
            .setFalsePositiveWeight(0)
            .setTrueNegativeWeight(negativeWeight)
            .setFalseNegativeWeight(positiveWeight)
            .setDecisionThreshold(Double.POSITIVE_INFINITY)
            .build());
      }

      return new PreparerResult<>(
          new ConstantResultTransformation3.Prepared<Number, Boolean, Number, BinaryEvaluationResult>().withResult(
              BinaryEvaluationResult.Builder.setConfusionMatrices(confusionMatrices).build()));
    }
  }

  /**
   * Evaluates the given predicted scores against the actual labels and returns the resulting binary evaluation.
   *
   * @param weights the weights for each example (can be used to place more emphasis on some examples and less on
   *                others)
   * @param actualLabels the actual, real labels for the examples
   * @param predictedScores the predicted scores for the examples; each actual label should correspond to the predicted
   *                        score at the same index in their respective lists
   * @return the evaluation
   */
  public static BinaryEvaluationResult evaluate(Iterable<? extends Number> weights, Iterable<Boolean> actualLabels,
      Iterable<? extends Number> predictedScores) {
    return PreparableTransformer3.prepare(new BinaryEvaluation(), weights, actualLabels, predictedScores)
        .getPreparedTransformerForNewData()
        .getResult();
  }

  /**
   * Evaluates the given predicted labels against the actual labels and returns the resulting evaluation.
   *
   * @param actualLabels the actual, real labels for the examples
   * @param predictedScores the predicted scores for the examples; each actual label should correspond to the predicted
   *                        score at the same index in their respective lists
   * @return the evaluation
   */
  public static BinaryEvaluationResult evaluate(Iterable<Boolean> actualLabels,
      Iterable<? extends Number> predictedScores) {
    return evaluate(new ConstantReader<>(1.0, Iterables.size64(actualLabels)), actualLabels, predictedScores);
  }
}
