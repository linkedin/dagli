package com.linkedin.dagli.vector;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.list.VariadicList;
import com.linkedin.dagli.math.vector.SparseIndexArrayVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.object.Convert;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1;
import java.util.Arrays;
import java.util.List;


/**
 * Transformer that translates an {@link Iterable} of {@link Number}s into a many-hot sparse {@link Vector}, where
 * the element at each index in the provided set of numbers takes the value 1.0 (by default), and all other elements are
 * 0.
 */
@ValueEquality
public class ManyHotVector extends AbstractPreparedTransformer1<long[], Vector, ManyHotVector> {
  private static final long serialVersionUID = 1;

  private double _hotElementValue = 1.0;

  /**
   * Returns a copy of this instance that will assign the specified value to "hot" elements whose indices are present
   * in the provided input.  By default this value is 1.0.
   *
   * @param hotElementValue the value that "hot" elements will be assigned in the produced vector
   * @return a copy of this instance that will use the specified value
   */
  public ManyHotVector withHotElementValue(double hotElementValue) {
    return clone(c -> c._hotElementValue = hotElementValue);
  }

  /**
   * Convenience method that may be used instead of {@link #withInputArray(Producer)} for the common case where
   * you want to create a many-hot vector from an {@link Iterable} of {@link Number}s rather than an array of longs.
   *
   * @param numbersInput an {@link Iterable} of {@link Number}s that will be taken as the "hot" indices in the produced
   *                     vector
   * @return a copy of this instance that will use the specified input
   */
  public ManyHotVector withInputList(Producer<? extends Iterable<? extends Number>> numbersInput) {
    return withInputArray(Convert.Numbers.toLongArray(numbersInput));
  }

  /**
   * Returns a copy of this instance that will create many-hot vectors where the provided arrays of longs are the
   * indices that will be considered "hot".
   *
   * @param numbersInput an array of longs
   * @return a copy of this instance that will use the specified input
   */
  public ManyHotVector withInputArray(Producer<? extends long[]> numbersInput) {
    return super.withInput1(numbersInput);
  }

  /**
   * Convenience method that may be used instead of {@link #withInputArray(Producer)} for the common case where
   * you want to create a many-hot vector from one or more {@link Number} inputs rather than an array of longs.
   *
   * @param numberInputs one or more inputs providing {@link Number}s that will be taken as the "hot" indices in the
   *                     produced vector
   * @return a copy of this instance that will use the specified input
   */
  @SafeVarargs
  public final ManyHotVector withInputs(Producer<? extends Number>... numberInputs) {
    return withInputs(Arrays.asList(numberInputs));
  }

  /**
   * Convenience method that may be used instead of {@link #withInputArray(Producer)} for the common case where
   * you want to create a many-hot vector from one or more {@link Number} inputs rather than an array of longs.
   *
   * @param numberInputs one or more inputs providing {@link Number}s that will be taken as the "hot" indices in the
   *                     produced vector
   * @return a copy of this instance that will use the specified input
   */
  public ManyHotVector withInputs(List<? extends Producer<? extends Number>> numberInputs) {
    return withInputList(new VariadicList<Number>().withInputs(numberInputs));
  }

  @Override
  public Vector apply(long[] indices) {
    return new SparseIndexArrayVector(indices, _hotElementValue);
  }
}