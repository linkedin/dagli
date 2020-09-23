package com.linkedin.dagli.evaluation;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.distribution.MostLikelyLabelsFromDistribution;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
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
import com.linkedin.dagli.util.collection.Iterables;
import java.util.Objects;


/**
 * A preparable transformer that evaluates the performance of a ranker, resulting in a {@link RankingEvaluationResult}.
 *
 * The ranking considers only the top k ranked items (see {@link #withK(int)}), by default 5.
 * If the actual label is in these top k predictions the example is considered to be predicted "correctly" and its
 * reciprocal rank is calculated as 1/[position of the actual label in the prediction list].  If the actual label is not
 * in the top k predictions, the example is considered to be predicted "incorrectly" and the reciprocal rank is taken as
 * 0.  No labels beyond the top k in the predicted list are examined, which may be important for performance if the list
 * is very long.  Finally, if the predicted label list is empty, the example is considered to have "no prediction",
 * which is tracked separately from "incorrect", although both will decrease accuracy.
 *
 * The transformer takes three parameters: the weight of the example, the actual label and a ranked list of predicted
 * labels (starting from the top/best/most likely prediction).  The result of the transformer is a constant (same result
 * for all inputs) {@link RankingEvaluationResult} instance.
 */
@ValueEquality
public class TopKEvaluation extends
  AbstractPreparableTransformer3<Number,
                                Object,
                                Iterable<?>,
                                RankingEvaluationResult,
                                ConstantResultTransformation3.Prepared<Number, Object, Iterable<?>, RankingEvaluationResult>,
                                TopKEvaluation> {

  private static final long serialVersionUID = 1;

  private int _k = 5;

  /**
   * Creates a new instance.
   */
  public TopKEvaluation() {
    super(new Constant<>(1.0), MissingInput.get(), MissingInput.get());
  }

  @Override
  protected boolean hasAlwaysConstantResult() {
    return true;
  }

  /**
   * Returns a copy of this instance that will examine the specified number of elements in the prediction list when
   * computing the evaluation results.  If the actual label is in the first k predicted results, the prediction is
   * counted as correct and the reciprocal rank is 1/[position of correct label].  If the actual label is not present,
   * or is present after the kth position, the example is counted as incorrect and the reciprocal rank is 0.
   *
   * Note that this means that no predictions beyond the kth are actually examined, which may have performance benefits
   * if the iterable of predictions is very large.
   *
   * The default value of k is 5.
   *
   * @param k the number of top predicted results to check for the actual label
   * @return a copy of this instance that will use the specified k
   */
  public TopKEvaluation withK(int k) {
    return clone(c -> c._k = k);
  }

  /**
   * Returns a copy of this instance that will obtain the weight for each example used to calculate evaluation results
   * from the specified input.  If not set, the default constant value of 1.0 is used.
   *
   * @param input the input that will provide per-example weights
   * @return a copy of this instance that will use the specified input
   */
  public TopKEvaluation withWeightInput(Producer<? extends Number> input) {
    return withInput1(input);
  }

  /**
   * Returns a copy of this instance that will obtain the actual label for each example from the specified input.
   *
   * @param input the input that will provide the expected, actual label for each example
   * @return a copy of this instance that will use the specified input
   */
  public TopKEvaluation withActualLabelInput(Producer<?> input) {
    return withInput2(input);
  }

  /**
   * Returns a copy of this instance that will obtain the predicted ranking of labels for each example from the
   * specified input, in order of best to worst (where the first label in the iteration order of the collection of
   * labels is considered the highest probability/best prediction, the second label is the second-highest/second-best,
   * etc.
   *
   * @param input the input that will provide the predicted ranking of labels (in order of best to worst) for each
   *              example
   * @return a copy of this instance that will use the specified input
   */
  public TopKEvaluation withPredictedLabelsInput(Producer<? extends Iterable<?>> input) {
    return withInput2(input);
  }

  /**
   * Creates a copy of this instance that will obtain the predicted ranking of labels from a
   * {@link com.linkedin.dagli.math.distribution.DiscreteDistribution} (ranked in decreasing order of probability).
   *
   * Ties will be broken arbitrarily.
   *
   * @param input the input that will provide the predicted distribution for each example
   * @return a copy of this instance that will use the specified input
   */
  public <T> TopKEvaluation withPredictedDistributionInput(Producer<? extends DiscreteDistribution<T>> input) {
    return withPredictedLabelsInput(new MostLikelyLabelsFromDistribution<>(input).withLimit(_k));
  }

  @Override
  protected Preparer3<Number,
                      Object,
                      Iterable<?>,
                      RankingEvaluationResult,
                      ConstantResultTransformation3.Prepared<Number, Object, Iterable<?>, RankingEvaluationResult>>
  getPreparer(PreparerContext context) {
    return new Preparer(_k);
  }

  static class Preparer extends
      AbstractStreamPreparer3<Number,
                              Object,
                              Iterable<?>,
                              RankingEvaluationResult,
                              ConstantResultTransformation3.Prepared<Number, Object, Iterable<?>, RankingEvaluationResult>> {

    private RankingEvaluationResult _evaluation = RankingEvaluationResult.Builder
        .setCorrectWeight(0)
        .setTotalWeight(0)
        .setCorrectCount(0)
        .setTotalCount(0)
        .setIncorrectWeight(0)
        .setIncorrectCount(0)
        .setWeightedReciprocalRankSum(0)
        .setReciprocalRankSum(0)
        .build();

    private int _k;

    Preparer(int k) {
      _k = k;
    }

    /**
     * Calculates the 1-based position of the true label in a list of predicted labels.
     *
     * If the label is among the first _k items, returns its 1-based position.
     * If the list of predicted labels is empty, returns 0.
     * If none of the above, returns _k + 1
     *
     * @param trueLabel the label to seek in the provided predictedLabels
     * @param predictedLabels the labels to scan for the correct label
     * @return the 1-based position of the label if it is present, 0 if the predictions list is empty, _k + 1 if the
     *         label is not found (and the predictions list is not empty)
     */
    private int getLabelPosition(Object trueLabel, Iterable<?> predictedLabels) {
      int pos = 0;
      for (Object predicted : predictedLabels) {
        if (++pos == _k + 1 || Objects.equals(trueLabel, predicted)) {
          return pos;
        }
      }

      // return 0 if we didn't iterate over any elements in predictedLabels (i.e. the iterable was empty)
      // otherwise, return _k + 1
      return pos == 0 ? 0 : _k + 1;
    }

    @Override
    public void process(Number weight, Object trueLabel, Iterable<?> predictedLabels) {
      int pos = getLabelPosition(trueLabel, predictedLabels);
      assert pos <= _k + 1;

      if (pos == 0) {
        // if pos == 0 this is neither correct nor incorrect; it's considered to be "no prediction", but treated as
        // having a reciprocal rank of 0 and "not correct" when, e.g. computing accuracy.
      } else if (pos <= _k) {
        _evaluation._correctCount++;
        _evaluation._correctWeight += weight.doubleValue();
        double reciprocalRank = 1.0 / pos;
        _evaluation._reciprocalRankSum += reciprocalRank;
        _evaluation._weightedReciprocalRankSum += reciprocalRank * weight.doubleValue();
      } else { // implies pos == k + 1
        _evaluation._incorrectCount++;
        _evaluation._incorrectWeight += weight.doubleValue();
      }

      _evaluation._totalWeight += weight.doubleValue();
      _evaluation._totalCount++;
    }

    @Override
    public PreparerResult<ConstantResultTransformation3.Prepared<Number, Object, Iterable<?>, RankingEvaluationResult>> finish() {
      return new PreparerResult<>(
          new ConstantResultTransformation3.Prepared<Number, Object, Iterable<?>, RankingEvaluationResult>().withResult(
              _evaluation));
    }
  }

  /**
   * Evaluates the given predicted labels against the actual labels and returns the resulting evaluation.
   *
   * @param k the number of items in each predicted ranked list that should be considered as "correct"
   * @param weights the weights for each example (can be used to place more emphasis on some examples and less on
   *                others)
   * @param actualLabels the actual, true labels for the examples
   * @param predictedRankings the predicted ranked lists (from most to least probable) labels for the examples; each
   *                          actual label should correspond to the predicted ranked list at the same index in their
   *                          respective lists
   * @return the evaluation
   */
  public static RankingEvaluationResult evaluate(int k, Iterable<? extends Number> weights, Iterable<?> actualLabels,
      Iterable<? extends Iterable<?>> predictedRankings) {
    return PreparableTransformer3.prepare(new TopKEvaluation().withK(k), weights, actualLabels, predictedRankings)
        .getPreparedTransformerForNewData()
        .getResult();
  }

  /**
   * Evaluates the given predicted labels against the actual labels and returns the resulting evaluation.
   *
   * @param k the number of items in each predicted ranked list that should be considered as "correct"
   * @param actualLabels the actual, true labels for the examples
   * @param predictedRankings the predicted ranked lists (from most to least probable) labels for the examples; each
   *                          actual label should correspond to the predicted ranked list at the same index in their
   *                          respective lists
   * @return the evaluation
   */
  public static RankingEvaluationResult evaluate(int k, Iterable<?> actualLabels,
      Iterable<? extends Iterable<?>> predictedRankings) {
    return evaluate(k, new ConstantReader<>(1.0, Iterables.size64(actualLabels)), actualLabels, predictedRankings);
  }
}
