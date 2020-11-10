package com.linkedin.dagli.vector;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.list.VariadicList;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1;
import com.linkedin.dagli.util.collection.Iterables;
import java.util.Arrays;
import java.util.List;


/**
 * Averages the inputted vectors as arguments, producing a dense vector which is the result of finding the mean of each
 * element.  So, for example, applying this to the vectors [0, 1, 2, 3] and [4, 5, 6] yields [2, 3, 4, 1.5].
 *
 * The non-zero element indices of the input vectors must be less than {@link Integer#MAX_VALUE}.
 */
@ValueEquality
public class AveragedDenseVector
    extends AbstractPreparedTransformer1<Iterable<? extends DenseVector>, DenseVector, AveragedDenseVector> {
  private static final long serialVersionUID = 1;

  /**
   * @param inputs the inputs provided the (dense) vectors to average
   * @return a copy of this transformer that will average the vectors provided by the given inputs
   */
  @SafeVarargs
  public final AveragedDenseVector withInputs(Producer<? extends DenseVector>... inputs) {
    return withInputs(Arrays.asList(inputs));
  }

  /**
   * @param inputs the inputs provided the (dense) vectors to average
   * @return a copy of this transformer that will average the vectors provided by the given inputs
   */
  public AveragedDenseVector withInputs(List<Producer<? extends DenseVector>> inputs) {
    return withInputList(new VariadicList<>(inputs));
  }

  /**
   * @param input the input providing an iterable of (dense) vectors to average
   * @return a copy of this transformer that will average the vectors provided by the given input
   */
  public AveragedDenseVector withInputList(Producer<? extends Iterable<? extends DenseVector>> input) {
    return withInput1(input);
  }

  @Override
  public DenseFloatArrayVector apply(Iterable<? extends DenseVector> values) {
    long requiredCapacity = Iterables.stream(values).mapToLong(DenseVector::capacity).max().orElse(0);

    DenseFloatArrayVector result = DenseFloatArrayVector.wrap(new float[Math.toIntExact(requiredCapacity)]);
    values.forEach(result::addInPlace);

    long count = Iterables.size64(values);
    if (count > 0) {
      result.multiplyInPlace(1.0 / count);
    }
    return result;
  }
}
