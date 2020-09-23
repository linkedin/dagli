package com.linkedin.dagli.vector;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import com.linkedin.dagli.util.invariant.Arguments;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * {@link NearestVector} finds the vector that is "closest" (currently, the only measure offered is Euclidean distance)
 * to the input vector from a pre-determined list of candidates.  It then returns a structure containing the closest
 * vector's index, distance (the "score"--here, lower is better), and the closest vector itself.
 *
 * The current implementation of {@link NearestVector} can operate most efficiently when both inputs and candidates are
 * {@link DenseFloatArrayVector}s.
 */
@ValueEquality
public class NearestVector extends AbstractPreparedTransformer1WithInput<Vector, ScoredVector, NearestVector> {
  private static final long serialVersionUID = 1;

  private ArrayList<Vector> _candidates = null;
  private boolean _allCandidatesDense = false;

  /**
   * Sets the candidate vectors.  Inputs to this transformer will be checked against these candidates to determine which
   * candidate is the closest, and at which distance, in order to calculate the resultant {@link ScoredVector}.
   *
   * @param candidates the array of candidates to use
   * @return a copy of this instance that will use the provided candidates
   */
  public NearestVector withCandidates(List<Vector> candidates) {
    Objects.requireNonNull(candidates);
    Arguments.check(candidates.size() > 0);

    return clone(c -> {
      c._candidates = new ArrayList<>(candidates);
      c._allCandidatesDense = c._candidates.stream().allMatch(candidate -> candidate instanceof DenseFloatArrayVector);
    });
  }

  // computes the (squared) Euclidean distance between two vectors
  private double squaredDistance(float[] vec1, float[] vec2) {
    int extent = Math.min(vec1.length, vec2.length);
    double dist = 0;
    for (int i = 0; i < extent; i++) {
      double v = vec1[i] - vec2[i];
      dist += v * v;
    }

    for (int i = vec1.length; i < vec2.length; i++) {
      dist += vec2[i] * vec2[i];
    }

    for (int i = vec2.length; i < vec1.length; i++) {
      dist += vec1[i] * vec1[i];
    }

    return dist;
  }

  // computes the (squared) Euclidean distance between two vectors
  private double squaredDistance(Vector vec1, float[] vec2) {
    double[] res = new double[1];
    vec1.forEach((index, value) -> {
      if (index < 0 || index >= vec2.length) {
        res[0] += value * value;
      } else {
        float vec2Elem = vec2[(int) index];
        double diff = value - vec2Elem;
        res[0] += diff * diff - (vec2Elem * vec2Elem);
      }
    });

    double result = res[0];
    for (int i = 0; i < vec2.length; i++) {
      result += vec2[i] * vec2[i];
    }

    return result;
  }

  // computes the (squared) Euclidean distance between two vectors
  private double squaredDistance(Vector vec1, Vector vec2) {
    return vec1.lazySubtract(vec2).norm(2);
  }

  @Override
  public ScoredVector apply(Vector value0) {
    double minDistance = Double.MAX_VALUE;
    int minIndex = 0;

    if (_allCandidatesDense) {
      List<DenseFloatArrayVector> denseCandidates = (List) _candidates; // we know every element is a DenseFloatArrayVector
      if (value0 instanceof DenseFloatArrayVector) {
        float[] elements = ((DenseFloatArrayVector) value0).getArray();
        for (int i = 0; i < denseCandidates.size(); i++) {
          double dist = squaredDistance(elements, denseCandidates.get(i).getArray());
          if (dist < minDistance) {
            minDistance = dist;
            minIndex = i;
          }
        }
      } else {
        for (int i = 0; i < denseCandidates.size(); i++) {
          double dist = squaredDistance(value0, denseCandidates.get(i).getArray());
          if (dist < minDistance) {
            minDistance = dist;
            minIndex = i;
          }
        }
      }
    } else {
      for (int i = 0; i < _candidates.size(); i++) {
        double dist = squaredDistance(value0, _candidates.get(i));
        if (dist < minDistance) {
          minDistance = dist;
          minIndex = i;
        }
      }
    }

    return ScoredVector.Builder
        .setIndex(minIndex)
        .setScore(minDistance)
        .setVector(_candidates.get(minIndex))
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
