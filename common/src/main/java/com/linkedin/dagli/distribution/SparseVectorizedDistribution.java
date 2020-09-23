package com.linkedin.dagli.distribution;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import com.linkedin.dagli.math.vector.SparseFloatArrayVector;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import com.linkedin.dagli.util.function.LongFunction1;


/**
 * Produces a sparse vector representation of a distribution; each label in the distribution is hashed to obtain the
 * corresponding vector element index, and the probability of the label is taken as the value.
 *
 * A hash function may optionally be provided when you specify the input via the withInputAndHasher method.  Both are
 * provided in a single call to ensure that the label and hash function input types are consistent.
 */
@ValueEquality
public class SparseVectorizedDistribution extends
    AbstractPreparedTransformer1WithInput<DiscreteDistribution<?>, SparseFloatArrayVector, SparseVectorizedDistribution> {

  private static final long serialVersionUID = 2;

  // used to compute the vector index of a given label; if null, the label's hashCode() method will be used instead.
  private LongFunction1.Serializable<Object> _hasher64 = null;

  /**
   * Creates an instance that calculates the (sparse) vector index of each label using the label's hashCode() method.
   */
  public SparseVectorizedDistribution() {
    super();
  }

  /**
   * Creates an instance that calculates the (sparse) vector index of each label using the label's hashCode() method.
   *
   * @param input the input distribution to vectorize
   */
  public SparseVectorizedDistribution(Producer<? extends DiscreteDistribution<?>> input) {
    super(input);
  }

  /**
   * Specifies both the discrete distribution input as well as the hash function to be used to compute the (sparse)
   * vector index from each label.
   *
   * @param input the input distribution to vectorize
   * @param hashFunction the hash function for hashing labels; to ensure that this instance can safely be serialized
   *                     we strongly recommend implementing your hash function as a proper class rather than simply
   *                     passing a lambda.  Lambdas are very tricky to serialize safely.
   * @param <T> the type of label used
   * @return a copy of this instance with the specified input and hash function.
   */
  @SuppressWarnings("unchecked")
  public <T> SparseVectorizedDistribution withInputAndHasher(
      Producer<? extends DiscreteDistribution<T>> input, LongFunction1.Serializable<? super T> hashFunction) {
    return clone(c -> {
        c._input1 = input;
        c._hasher64 = (LongFunction1.Serializable<Object>) hashFunction.safelySerializable();
    });
  }

  @Override
  public SparseFloatArrayVector apply(DiscreteDistribution<?> val1) {
    long[] indices = new long[(int) val1.size64()];
    float[] values = new float[(int) val1.size64()];
    int[] i = new int[1];

    // hash each label to determine its index in the sparse vector; the value at that index will then be the probability
    // (in the unlikely event of an index collision, SparseSortedVector.of will choose one of the values assigned to
    // the colliding indices arbitrarily)
    val1.stream().forEach(lp -> {
      indices[i[0]] = _hasher64 == null ? lp.getLabel().hashCode() : _hasher64.apply(lp.getLabel());
      values[i[0]] = (float) lp.getProbability();
      i[0]++;
    });

    return SparseFloatArrayVector.wrap(indices, values);
  }
}
