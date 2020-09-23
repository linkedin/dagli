package com.linkedin.dagli.vector;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.transformer.AbstractPreparedTransformerVariadic;
import com.linkedin.dagli.math.vector.SparseFloatMapVector;
import com.linkedin.dagli.math.vector.Vector;
import java.util.List;


/**
 * Transformer that sums all its vector inputs and returns the result.
 */
@ValueEquality(commutativeInputs = true)
public class VectorSum extends AbstractPreparedTransformerVariadic<Vector, Vector, VectorSum> {
  private static final long serialVersionUID = 1;

  @Override
  public Vector apply(List<? extends Vector> values) {
    SparseFloatMapVector sum = new SparseFloatMapVector();
    for (Vector v : values) {
      sum.addInPlace(v);
    }

    return sum;
  }
}
