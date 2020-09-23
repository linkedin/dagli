package com.linkedin.dagli.evaluation;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.distribution.MostLikelyLabelFromDistribution;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import com.linkedin.dagli.objectio.ConstantReader;
import com.linkedin.dagli.preparer.AbstractStreamPreparer3;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.Preparer3;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparableTransformer3;
import com.linkedin.dagli.transformer.ConstantResultTransformation3;
import com.linkedin.dagli.transformer.PreparableTransformer3;
import com.linkedin.dagli.util.collection.Iterables;
import java.util.HashMap;
import java.util.Objects;


/**
 * A preparable transformer that results in a {@link MultinomialEvaluationResult}.
 *
 * The transformer takes three parameters: the weight of the example, the true label and the predicted label.  The
 * result of the transformer is a constant (same result for all inputs) {@link MultinomialEvaluationResult} instance.
 */
@ValueEquality
public class MultinomialEvaluation extends
    AbstractPreparableTransformer3<Number,
                                  Object,
                                  Object,
                                  MultinomialEvaluationResult,
                                  ConstantResultTransformation3.Prepared<Number, Object, Object, MultinomialEvaluationResult>,
                                  MultinomialEvaluation> {

  private static final long serialVersionUID = 1;

  /**
   * Creates a new instance.
   */
  public MultinomialEvaluation() {
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
  public MultinomialEvaluation withWeightInput(Producer<? extends Number> input) {
    return withInput1(input);
  }

  /**
   * Creates a copy of this instance that will obtain the actual label for each example from the specified input.
   *
   * @param input the input that will provide the expected, actual label for each example
   * @return a copy of this instance that will use the specified input
   */
  public MultinomialEvaluation withActualLabelInput(Producer<?> input) {
    return withInput2(input);
  }

  /**
   * Creates a copy of this instance that will obtain the predicted label for each example from the specified input.
   *
   * @param input the input that will provide the predicted label for each example
   * @return a copy of this instance that will use the specified input
   */
  public MultinomialEvaluation withPredictedLabelInput(Producer<?> input) {
    return withInput3(input);
  }

  /**
   * Creates a copy of this instance that will accept as the predicted label the most likely label from the provided
   * {@link com.linkedin.dagli.math.distribution.DiscreteDistribution} input.
   *
   * Ties will be broken arbitrarily and an empty distribution (with no non-zero probability labels) will be treated as
   * if the most likely label were null.
   *
   * @param input the input that will provide the predicted distribution for each example
   * @return a copy of this instance that will use the specified input
   */
  public <T> MultinomialEvaluation withPredictedDistributionInput(Producer<? extends DiscreteDistribution<T>> input) {
    return withPredictedLabelInput(new MostLikelyLabelFromDistribution<>(input).withDefaultLabel(null));
  }

  @Override
  protected Preparer3<Number,
                      Object,
                      Object,
                      MultinomialEvaluationResult,
                      ConstantResultTransformation3.Prepared<Number, Object, Object, MultinomialEvaluationResult>> getPreparer(
      PreparerContext context) {
    return new Preparer();
  }

  static class Preparer extends
      AbstractStreamPreparer3<Number,
                              Object,
                              Object,
                              MultinomialEvaluationResult,
                              ConstantResultTransformation3.Prepared<Number, Object, Object, MultinomialEvaluationResult>> {

    private double _correctWeight = 0;
    private double _totalWeight = 0;
    private long _correctCount = 0;
    private long _totalCount = 0;

    private HashMap<ActualAndPredictedLabel, WeightAndCount> _weightCounts = new HashMap<>();

    @Override
    public void process(Number weight, Object trueLabel, Object predictedLabel) {
      _totalWeight += weight.doubleValue();
      _totalCount++;

      WeightAndCount weightCount = _weightCounts.computeIfAbsent(
          ActualAndPredictedLabel.Builder.setActualLabel(trueLabel).setPredictedLabel(predictedLabel).build(),
          lp -> WeightAndCount.Builder.setWeight(0).setCount(0).build());

      weightCount._count++;
      weightCount._weight += weight.doubleValue();

      if (Objects.equals(trueLabel, predictedLabel)) {
        _correctWeight += weight.doubleValue();
        _correctCount++;
      }
    }

    @Override
    public PreparerResult<ConstantResultTransformation3.Prepared<Number, Object, Object, MultinomialEvaluationResult>> finish() {
      return new PreparerResult<>(
          new ConstantResultTransformation3.Prepared<Number, Object, Object, MultinomialEvaluationResult>().withResult(
              MultinomialEvaluationResult.Builder
                  .setCorrectWeight(_correctWeight)
                  .setTotalWeight(_totalWeight)
                  .setCorrectCount(_correctCount)
                  .setTotalCount(_totalCount)
                  .setActualAndPredictedLabelPairToWeightAndCountMap(_weightCounts)
                  .build()));
    }
  }

  /**
   * Evaluates the given predicted labels against the actual labels and returns the resulting evaluation.
   *
   * @param weights the weights for each example (can be used to place more emphasis on some examples and less on
   *                others)
   * @param actualLabels the actual, true labels for the examples
   * @param predictedLabels the predicted labels for the examples; each actual label should correspond to the predicted
   *                        label at the same index in their respective lists
   * @return the evaluation
   */
  public static MultinomialEvaluationResult evaluate(Iterable<? extends Number> weights, Iterable<?> actualLabels,
      Iterable<?> predictedLabels) {
    return PreparableTransformer3.prepare(new MultinomialEvaluation(), weights, actualLabels, predictedLabels)
        .getPreparedTransformerForNewData()
        .getResult();
  }

  /**
   * Evaluates the given predicted labels against the actual labels and returns the resulting evaluation.
   *
   * @param actualLabels the actual, true labels for the examples
   * @param predictedLabels the predicted labels for the examples; each actual label should correspond to the predicted
   *                        label at the same index in their respective lists
   * @return the evaluation
   */
  public static MultinomialEvaluationResult evaluate(Iterable<?> actualLabels, Iterable<?> predictedLabels) {
    return evaluate(new ConstantReader<>(1.0, Iterables.size64(actualLabels)), actualLabels, predictedLabels);
  }
}
