package com.linkedin.dagli.vector;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.object.Max;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformerDynamic;
import com.linkedin.dagli.util.invariant.Arguments;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Creates a {@link com.linkedin.dagli.math.vector.LazyConcatenatedDenseVector} by concatenating a sequence of dense
 * vectors provided as inputs (both this transformer and the type of {@link com.linkedin.dagli.math.vector.Vector} it
 * produces have the same name).
 *
 * The range of indices in the resulting concatenated vector (its "index space") allocated for each input vector is
 * calculated from the maximum indices non-zero element index observed across all the vectors provided through that
 * input during preparation.
 *
 * For example, if the highest non-zero element index seen from the first input was 3, and the highest index from the
 * second input was 2, then the concatenated vector given the particular inputs [0, 42] and [1, 2] would be
 * [0, 42, 0, 0, 1, 2] with a capacity of of 7.  The vectors produced by this transformer will all have the same
 * capacity.
 *
 * <strong>Although this is a prepared transformer, it automatically creates preparable ancestors, and a DAG using
 * this transformer will consequently also be preparable.</strong>  If, after preparation, an input vector with
 * higher non-zero element indices than those seen during preparation (for that input) are encountered, such elements
 * are silently ignored.
 */
@ValueEquality
public class LazyConcatenatedDenseVector
    extends AbstractPreparedTransformerDynamic<com.linkedin.dagli.math.vector.LazyConcatenatedDenseVector, LazyConcatenatedDenseVector> {

  private static final long serialVersionUID = 1;

  /**
   * Returns a copy of this instance that will concatenate the given input {@link DenseVector}s.
   *
   * @param denseVectors the inputs that will provide the vectors to concatenate
   * @return a copy of this instance that will concatenate the given input {@link DenseVector}s
   */
  @SafeVarargs
  public final LazyConcatenatedDenseVector withInputs(Producer<? extends DenseVector>... denseVectors) {
    return withInputs(Arrays.asList(denseVectors));
  }

  /**
   * Returns a copy of this instance that will concatenate the given input {@link DenseVector}s.
   *
   * @param denseVectors the inputs that will provide the vectors to concatenate
   * @return a copy of this instance that will concatenate the given input {@link DenseVector}s
   */
  public LazyConcatenatedDenseVector withInputs(List<? extends Producer<? extends DenseVector>> denseVectors) {
    Arguments.check(!denseVectors.isEmpty(), "At least one input must be provided");
    return clone(c -> {
      // add the given producers as inputs, and automatically also create Max(MaxNonZeroVectorElementIndex) parents that
      // will give us the highest non-zero vector element indices for each of our inputs.
      ArrayList<Producer<?>> inputs = new ArrayList<>(denseVectors.size() * 2);
      denseVectors.stream()
          .map(p -> new Max<Long>().withInput(new MaxNonZeroVectorElementIndex().withInput(p)))
          .forEach(inputs::add);
      inputs.addAll(denseVectors);
      c._inputs = inputs;
    });
  }

  @Override
  protected com.linkedin.dagli.math.vector.LazyConcatenatedDenseVector apply(List<?> values) {
    final int halfOfInputCount = values.size() / 2;
    long[] vectorCapacities = new long[halfOfInputCount];
    Arrays.setAll(vectorCapacities, index -> values.get(index) == null ? 0 : (Long) values.get(index) + 1);

    return com.linkedin.dagli.math.vector.LazyConcatenatedDenseVector.wrap(
        values.subList(halfOfInputCount, values.size()).toArray(new DenseVector[0]), vectorCapacities);
  }
}
