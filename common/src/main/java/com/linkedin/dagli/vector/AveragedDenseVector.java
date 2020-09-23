package com.linkedin.dagli.vector;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.list.VariadicList;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;


/**
 * Averages the inputted vectors as arguments, producing a dense vector which is the result of finding the mean of each
 * element.  So, for example, applying this to the vectors [0, 1, 2, 3] and [4, 5, 6] yields [2, 3, 4, 1.5].
 *
 * Because this transformer produces a DenseFloatArrayVector, inputting vectors with element indices less than 0 or
 * greater than 2^31 - 1 elements will result in an exception.
 */
@ValueEquality
public class AveragedDenseVector extends
    AbstractPreparedTransformer1WithInput<Iterable<? extends Vector>, DenseFloatArrayVector, AveragedDenseVector> {
  private static final long serialVersionUID = 1;

  /**
   * Specifies which (variadic) inputs will be used.
   *
   * Under the hood, this method uses a VariadicList to transform these inputs into a List<Vector>, which is then the
   * direct input to AveragedDenseVector.
   *
   * @param inputs the (variadic) inputs providing the vectors to be averaged
   * @return a new AveragedDenseVector that will use the provided inputs
   */
  @SafeVarargs
  public final AveragedDenseVector withInputs(Producer<? extends Vector>... inputs) {
    return withInput(new VariadicList<Vector>().withInputs(inputs));
  }

  @Override
  public DenseFloatArrayVector apply(Iterable<? extends Vector> values) {
    long maxElementIndex = -1;

    for (Vector v : values) {
      maxElementIndex = Math.max(maxElementIndex, v.maxNonZeroElementIndex().orElse(-1));
    }

    // still do the sum, even if maxElementIndex == -1 (in which case we still want to check that no negative index
    // elements are present in the input)
    long vectorCount = 0;
    DenseFloatArrayVector result = DenseFloatArrayVector.wrap(new float[Math.toIntExact(maxElementIndex + 1)]);
    for (Vector v : values) {
      vectorCount++;
      result.addInPlace(v);
    }

    if (vectorCount > 0) {
      result.multiplyInPlace(1.0 / vectorCount);
    }
    return result;
  }
}
