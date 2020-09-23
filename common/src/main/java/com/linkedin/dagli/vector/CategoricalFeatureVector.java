package com.linkedin.dagli.vector;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.list.VariadicList;
import com.linkedin.dagli.math.hashing.DoubleXorShift;
import com.linkedin.dagli.math.vector.SparseIndexArrayVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import com.linkedin.dagli.util.collection.Iterables;
import org.apache.commons.rng.core.source64.XoRoShiRo128PlusPlus;


/**
 * Given an iterable collection of objects representing an (ordered) list of categorical features, represents each of
 * them via a "one-hot" encoding in a sparse vector.  Each different categorical value in each position in the list will
 * correspond to a pseudounique index in the sparse vector which will be assigned the value 1.0; all other elements in
 * the vector will have value 0.  More precisely, each index in the resulting vector is determined via
 * <code>hash(hash(feature index) + hash(feature value))</code>, and leverages the {@link Object#hashCode()} method of
 * that feature value.  The indices are "pseudounique" because, although unlikely, hash collisions are of course
 * possible; consequently, it <strong>may be the case that the resulting sparse vector has fewer non-zero elements than
 * there were elements in the iterable input</strong>.
 *
 * As an example, if our input is the list of {@link String} objects ["Hello", "World", "!"], we might compute a
 * sparse vector of [-93 -> 1, 1652 -> 1, -4321 -> 1, 3399 -> 1] (these indices are hypothetical and will tend to be
 * much larger in practice as the indices will be roughly uniformly distributed 64-bit integers).  Note that there is
 * no relationship between the ordering of the categorical features (either by feature index or by value) and the
 * resulting sparse indices (i.e. categorical feature #1 with value 0 could have a higher or lower index that feature #2
 * with value 0 or feature #1 with value 1.)
 */
@ValueEquality
public class CategoricalFeatureVector extends
    AbstractPreparedTransformer1WithInput<Iterable<?>, Vector, CategoricalFeatureVector> {

  private static final long serialVersionUID = 1;

  // seeds used for the position-based "hash" generator (a RNG)
  private static final long SEED1 = 0x14c9c274aabaad5bL; // arbitrary random value
  private static final long SEED2 = 0x239c7da6201d0de6L; // arbitrary random value

  @Override
  public Vector apply(Iterable<?> iterable) {
    // we'll generate a pseudounique number for every position in the iterable (0, 1, 2, 3...) using a very cheap
    // pseudorandom number generator.  This particular generator is known to have "less-random" lower bits, but due to
    // how we use the result this isn't a real concern.
    XoRoShiRo128PlusPlus positionHashGenerator = new XoRoShiRo128PlusPlus(SEED1, SEED2);

    long[] indices = new long[Math.toIntExact(Iterables.size64(iterable))];
    int offset = 0;

    for (Object o : iterable) {
      indices[offset++] = DoubleXorShift.hashWithDefaultSeed(positionHashGenerator.next() + o.hashCode());
    }

    return SparseIndexArrayVector.wrap(indices, 1);
  }

  /**
   * Returns a copy of this instance that will receive its categorical values from the given inputs, with each input
   * producer providing a single categorical value (i.e. if an input provides a list, that list will itself be treated
   * as a single categorical value).
   *
   * @param categoricalValueInputs the inputs providing categorical values
   * @return a copy of this instance that will receive its categorical values from the given inputs
   */
  public CategoricalFeatureVector withInputs(Producer<?>... categoricalValueInputs) {
    return withInput(new VariadicList<>(categoricalValueInputs));
  }
}
