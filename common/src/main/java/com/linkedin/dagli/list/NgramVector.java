package com.linkedin.dagli.list;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.vector.SparseFloatMapVector;
import com.linkedin.dagli.math.vector.Vector;
import java.util.List;


/**
 * Creates a sparse vector of ngram counts from a provided list items (from which the ngrams are computed and counted).
 *
 * Each ngram maps to a vector index via hashing.  The per-item hashing function (which is used to compute the hash of
 * the entire ngram) may be provided, but otherwise defaults to {@link Object#hashCode()}.
 */
@ValueEquality
public class NgramVector extends AbstractNgrams<Vector, NgramVector> {

  private static final long serialVersionUID = 1;

  /**
   * Create a new NgramVector that will (by default) calculate unigram counts only.
   *
   * The default padding is SINGLE, although padding is irrelevant for unigrams regardless.
   */
  public NgramVector() {
    super();
  }

  @Override
  public Vector apply(List<?> sequence) {
    if (sequence.isEmpty()) {
      return Vector.empty();
    }

    SparseFloatMapVector vec = new SparseFloatMapVector(estimateNgramCount(sequence.size()));
    apply(sequence, hash -> vec.increase(hash, 1));
    return vec;
  }
}
