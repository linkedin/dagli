package com.linkedin.dagli.vector;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.list.VariadicList;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1;
import com.linkedin.dagli.util.closeable.Closeables;
import com.linkedin.dagli.util.collection.Iterables;
import java.util.Arrays;
import java.util.List;


/**
 * Produces a dense vector (of float values) from the provided {@link Iterable} collection of {@link Number}s, where the
 * first number is mapped to index 0 in the vector, the second number to index 1, etc.  Numbers that do not fit within
 * the float data type will be truncated when they are retrieved via the {@link Number#floatValue()} method.
 */
@ValueEquality
public class DenseVectorFromNumbers extends
    AbstractPreparedTransformer1<Iterable<? extends Number>, DenseFloatArrayVector, DenseVectorFromNumbers> {
  private static final long serialVersionUID = 1;

  @Override
  public DenseFloatArrayVector apply(Iterable<? extends Number> values) {
    float[] arr = new float[Math.toIntExact(Iterables.size64(values))];

    java.util.Iterator<? extends Number> iterator = values.iterator();
    try {
      for (int i = 0; i < arr.length; i++) {
        arr[i] = iterator.next().floatValue();
      }
    } finally {
      Closeables.tryClose(iterator);
    }

    return DenseFloatArrayVector.wrap(arr);
  }

  /**
   * Returns a copy of this instance that will accept a list of numbers to vectorize from the given input.
   *
   * @param input the input providing a list of numbers that should be combined into a dense vector
   * @return a copy of this instance that will combine the numbers provided by the given producer into a dense vector
   */
  public DenseVectorFromNumbers withInputList(Producer<? extends Iterable<? extends Number>> input) {
    return withInput1(input);
  }

  /**
   * Convenience method that accepts inputs as a variadic list of {@link Number} {@link Producer}s.  This is equivalent
   * to {@code denseVectorFromNumbers.withInputList(new VariadicList<Number>().withInputs(inputs))}}.
   *
   * @param inputs the inputs that should be combined into a dense vector
   * @return a copy of this instance that will combine the numbers provided by the given producers into a dense vector
   */
  @SafeVarargs
  public final DenseVectorFromNumbers withInputs(Producer<? extends Number>... inputs) {
    return withInputs(Arrays.asList(inputs));
  }

  /**
   * Convenience method that accepts inputs as a variadic list of {@link Number} {@link Producer}s.  This is equivalent
   * to {@code denseVectorFromNumbers.withInputList(new VariadicList<Number>().withInputs(inputs))}}.
   *
   * @param inputs the inputs that should be combined into a dense vector
   * @return a copy of this instance that will combine the numbers provided by the given producers into a dense vector
   */
  public DenseVectorFromNumbers withInputs(List<? extends Producer<? extends Number>> inputs) {
    return withInputList(new VariadicList<Number>().withInputs(inputs));
  }
}