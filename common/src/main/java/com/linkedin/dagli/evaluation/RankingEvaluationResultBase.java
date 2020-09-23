package com.linkedin.dagli.evaluation;

import com.linkedin.dagli.annotation.struct.Struct;
import com.linkedin.dagli.annotation.struct.VirtualField;


/**
 * The result of evaluating the performance of a ranking model, including ranking specific metrics (mean reciprocal
 * rank) as well as general classification metrics.  For example, a {@link TopKEvaluation} will judge an example where
 * the correct result is in the top K of the list of predicted results to be an example the ranker got "correct" for
 * the purposes of computing accuracy, the number of correctly-predicted examples, etc.
 *
 * {@link RankingEvaluationResult}s are {@link Comparable} and are compared using their weighted mean reciprocal rank.
 */
@Struct("RankingEvaluationResult")
class RankingEvaluationResultBase extends AbstractClassificationEvaluationResult
    implements Comparable<RankingEvaluationResult> {
  private static final long serialVersionUID = 1;

  /**
   * The sum weight of all lists where the correct result was not in the ranked list of items, and the the list was
   * non-empty.
   */
  double _incorrectWeight = 0;

  /**
   * The number of lists where the correct result was not in the ranked list of items, and the list was non-empty.
   */
  long _incorrectCount = 0;

  double _weightedReciprocalRankSum = 0;
  double _reciprocalRankSum = 0;

  /**
   * @return the sum weight of all predicted lists that were empty (no prediction)
   */
  @VirtualField("NoPredictionWeight")
  double getNoPredictionWeight() {
    return _totalWeight - _correctWeight - _incorrectWeight;
  }

  /**
   * @return the number of predicted lists that were empty (no prediction)
   */
  @VirtualField("NoPredictionCount")
  public long getNoPredictionCount() {
    return _totalCount - _correctCount - _incorrectCount;
  }

  /**
   * @return the mean reciprocal rank, treating all examples as equally important (ignoring weight)
   */
  @VirtualField("UnweightedMeanReciprocalRank")
  public double getUnweightedMeanReciprocalRank() {
    return _reciprocalRankSum / _totalCount;
  }

  /**
   * @return the weighted mean reciprocal rank
   */
  @VirtualField("WeightedMeanReciprocalRank")
  double getWeightedMeanReciprocalRank() {
    return _weightedReciprocalRankSum / _totalWeight;
  }

  @Override
  public String getSummary() {
    return super.getSummary() + "\n\n"
        + "Number of examples for which no prediction was made: " + getNoPredictionCount() + "\n"
        + "Weight of examples for which no prediction was made: " + getNoPredictionWeight() + "\n\n"
        + "Unweighted Mean Reciprocal Rank (MRR): " + getUnweightedMeanReciprocalRank() + "\n"
        + "Weighted Mean Reciprocal Rank (MRR): " + getWeightedMeanReciprocalRank();
  }

  @Override
  public int compareTo(RankingEvaluationResult o) {
    return Double.compare(getWeightedMeanReciprocalRank(), o.getWeightedMeanReciprocalRank());
  }
}
