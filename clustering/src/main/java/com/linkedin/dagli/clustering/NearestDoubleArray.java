package com.linkedin.dagli.clustering;

import com.linkedin.dagli.annotation.equality.DeepArrayValueEquality;
import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.vector.DenseDoubleArrayVector;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import com.linkedin.dagli.util.invariant.Arguments;
import com.linkedin.dagli.vector.ScoredVector;
import java.util.List;
import java.util.Objects;
import org.apache.commons.math3.ml.distance.EuclideanDistance;


/**
 * {@link NearestDoubleArray} finds the array that is "closest" (as determined by Euclidean distance) to an input array
 * from a pre-determined list of candidates.  It then returns a structure containing the closest array's index,
 * distance (the "score"--here, lower is better), and the closest array itself (as a {@link DenseDoubleArrayVector}
 *
 * One use of this class is as the "prediction" transformer for KMeans clustering algorithms.
 */
@ValueEquality
public class NearestDoubleArray
    extends AbstractPreparedTransformer1WithInput<double[], ScoredVector, NearestDoubleArray> {
  private static final long serialVersionUID = 1;
  private static final EuclideanDistance EUCLIDEAN_DISTANCE = new EuclideanDistance();

  @DeepArrayValueEquality
  private double[][] _candidates = null;

  /**
   * Sets the candidate vectors.  Inputs to this transformer will be checked against these candidates to determine which
   * candidate is the closest, and at which distance, in order to calculate the resultant {@link ScoredVector}.
   *
   * @param candidates the array of candidates to use
   * @return a copy of this instance that will use the provided candidates
   */
  public NearestDoubleArray withCandidates(List<double[]> candidates) {
    Objects.requireNonNull(candidates);
    Arguments.check(candidates.size() > 0);

    return clone(c -> c._candidates = candidates.stream().map(double[]::clone).toArray(double[][]::new));
  }

  @Override
  public ScoredVector apply(double[] array) {
    double minDistance = EUCLIDEAN_DISTANCE.compute(array, _candidates[0]);
    int minIndex = 0;

    for (int i = 1; i < _candidates.length; i++) {
      double distance = EUCLIDEAN_DISTANCE.compute(array, _candidates[i]);
      if (distance < minDistance) {
        minDistance = distance;
        minIndex = i;
      }
    }

    return ScoredVector.Builder
        .setIndex(minIndex)
        .setScore(minDistance)
        .setVector(DenseDoubleArrayVector.wrap(_candidates[minIndex]))
        .build();
  }

  @Override
  public void validate() {
    super.validate();
    if (_candidates == null) {
      throw new IllegalStateException(
          "No candidate vectors have been set.  Call withCandidates(...) to set candidates.");
    }
  }
}
