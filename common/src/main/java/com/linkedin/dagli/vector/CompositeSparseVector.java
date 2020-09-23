package com.linkedin.dagli.vector;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.vector.SparseFloatArrayVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.transformer.AbstractPreparedTransformerVariadic;
import java.util.List;
import org.apache.commons.rng.core.source64.XoRoShiRo128PlusPlus;


/**
 * Combines multiple vectors into a single sparse vector, remapping indices pseudo-randomly such that collisions are
 * improbable (but not impossible, especially when the number of elements is very large).  Values in the resulting
 * vector will be stored as floats.
 */
@ValueEquality
public class CompositeSparseVector extends AbstractPreparedTransformerVariadic<Vector, SparseFloatArrayVector, CompositeSparseVector> {
  private static final long serialVersionUID = 1;

  // seeds used for the position-based "hash" generator (a RNG)
  private static final long SEED1 = 0x47d288f848497589L; // arbitrary random value
  private static final long SEED2 = 0xf968177c88636faaL; // arbitrary random value

  @Override
  public SparseFloatArrayVector apply(List<? extends Vector> vectors) {
    // we'll generate a pseudounique number for every position in the iterable (0, 1, 2, 3...) using a very cheap
    // pseudorandom number generator.  This particular generator is known to have "less-random" lower bits, but due to
    // how we use the result this isn't a serious concern.
    XoRoShiRo128PlusPlus positionHashGenerator = new XoRoShiRo128PlusPlus(SEED1, SEED2);

    int count = Math.toIntExact(vectors.stream().mapToLong(Vector::size64).sum());

    long[] indices = new long[count];
    float[] values = new float[count];

    int[] nextArrayIndex = new int[1];

    for (Vector vec : vectors) {
      long positionHash = positionHashGenerator.next();

      vec.forEach((index, value) -> {
        indices[nextArrayIndex[0]] = index + positionHash;
        values[nextArrayIndex[0]] = (float) value;
        nextArrayIndex[0]++;
      });
    }

    return SparseFloatArrayVector.wrap(indices, values);
  }
}
