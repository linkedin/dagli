package com.linkedin.dagli.input;

import com.linkedin.dagli.distribution.SparseVectorFromDistribution;
import com.linkedin.dagli.function.FunctionResult1;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import com.linkedin.dagli.math.vector.DenseDoubleArrayVector;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.vector.CategoricalFeatureVector;
import com.linkedin.dagli.vector.CompositeSparseVector;
import com.linkedin.dagli.vector.DenseVectorFromNumbers;
import com.linkedin.dagli.vector.LazyConcatenatedDenseVector;
import com.linkedin.dagli.vector.ManyHotVector;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Base class for vector input configurators, optionally supporting the aggregation of one or more input values.
 *
 * @param <V> the type of inputs that may be aggregated (if applicable; otherwise Void)
 * @param <T> the type of the configured result object (e.g. a transformer)
 * @param <S> the type of the derived-most class extended this abstract class
 */
public abstract class AbstractFeatureVectorInput<V, T, S extends AbstractFeatureVectorInput<V, T, S>> extends AbstractInput<V, T, S> {
  protected abstract T fromVector(Producer<? extends Vector> vector);
  protected abstract T fromDenseVector(Producer<? extends DenseVector> vector);

  /**
   * Configures the input as a combination of other vectors.
   *
   * @param inputs the vectors to combine
   * @return the configured transformer
   */
  @SafeVarargs
  public final T fromVectors(Producer<? extends Vector>... inputs) {
    return fromVectors(Arrays.asList(inputs));
  }

  /**
   * Configures the input as a combination of other vectors.
   *
   * @param inputs the vectors to combine
   * @return the configured transformer
   */
  public T fromVectors(List<? extends Producer<? extends Vector>> inputs) {
    return fromVector(inputs.size() == 1 ? inputs.get(0) : new CompositeSparseVector().withInputs(inputs));
  }

  /**
   * Configures the input as a concatenation of dense vectors.
   *
   * @param inputs the vectors to concatenate
   * @return the configured transformer
   */
  @SafeVarargs
  public final T fromDenseVectors(Producer<? extends DenseVector>... inputs) {
    return fromVectors(Arrays.asList(inputs));
  }

  /**
   * Configures the input as a concatenation of dense vectors.
   *
   * @param inputs the vectors to concatenate
   * @return the configured transformer
   */
  public T fromDenseVectors(List<? extends Producer<? extends DenseVector>> inputs) {
    return fromDenseVector(inputs.size() == 1 ? inputs.get(0) : new LazyConcatenatedDenseVector().withInputs(inputs));
  }

  /**
   * Configures the input as vectorized categorical values (using the {@link CategoricalFeatureVector} transformer).
   *
   * @param inputs the producers providing categorical features
   * @return the configured transformer
   */
  public T fromCategoricalValues(Producer<?>... inputs) {
    return fromCategoricalValues(Arrays.asList(inputs));
  }

  /**
   * Configures the input as vectorized categorical values (using the {@link CategoricalFeatureVector} transformer).
   *
   * @param inputs the producers providing categorical features
   * @return the configured transformer
   */
  public T fromCategoricalValues(List<? extends Producer<?>> inputs) {
    return fromVector(new CategoricalFeatureVector().withInputs(inputs));
  }

  /**
   * Configures the input as vectorized categorical values (using the {@link CategoricalFeatureVector} transformer).
   *
   * @param input the producer providing a list of categorical features
   * @return the configured transformer
   */
  public T fromCategoricalValueList(Producer<? extends Iterable<?>> input) {
    return fromVector(new CategoricalFeatureVector().withInputList(input));
  }

  /**
   * Configures the input as a combination of one or more vectorized distributions (using the
   * {@link SparseVectorFromDistribution} transformer).
   *
   * @param inputs the producers providing distributions to be vectorized as features
   * @return the configured transformer
   */
  @SafeVarargs
  public final T fromDistributions(Producer<? extends DiscreteDistribution<?>>... inputs) {
    return fromDistributions(Arrays.asList(inputs));
  }

  /**
   * Configures the input as a combination of one or more vectorized distributions (using the
   * {@link SparseVectorFromDistribution} transformer).
   *
   * @param inputs the producers providing distributions to be vectorized as features
   * @return the configured transformer
   */
  public T fromDistributions(List<? extends Producer<? extends DiscreteDistribution<?>>> inputs) {
    return fromVectors(inputs.stream().map(SparseVectorFromDistribution::new).collect(Collectors.toList()));
  }

  /**
   * Configures the input as a vector of numbers (using the {@link DenseVectorFromNumbers} transformer).
   *
   * @param inputs the producers providing numbers to be vectorized as features
   * @return the configured transformer
   */
  @SafeVarargs
  public final T fromNumbers(Producer<? extends Number>... inputs) {
    return fromNumbers(Arrays.asList(inputs));
  }

  /**
   * Configures the input as a vector of numbers (using the {@link DenseVectorFromNumbers} transformer).
   *
   * @param inputs the producers providing numbers to be vectorized as features
   * @return the configured transformer
   */
  public T fromNumbers(List<? extends Producer<? extends Number>> inputs) {
    return fromDenseVector(new DenseVectorFromNumbers().withInputs(inputs));
  }

  /**
   * Configures the input as a vector of numbers (using the {@link DenseVectorFromNumbers} transformer).
   *
   * @param input the producer providing a list of numbers to be vectorized as features
   * @return the configured transformer
   */
  public T fromNumberList(Producer<? extends Iterable<? extends Number>> input) {
    return fromDenseVector(new DenseVectorFromNumbers().withInputList(input));
  }

  /**
   * Configures the input as a vector with a value of 1 for each provided index number (using the {@link ManyHotVector}
   * transformer); floating-point numbers will be truncated to {@code long} index values
   *
   * @param inputs the producers providing the indices whose elements will take the value of 1 in the resulting vector
   * @return the configured transformer
   */
  @SafeVarargs
  public final T fromIndices(Producer<? extends Number>... inputs) {
    return fromIndices(Arrays.asList(inputs));
  }

  /**
   * Configures the input as a vector with a value of 1 for each provided index number (using the {@link ManyHotVector}
   * transformer); floating-point numbers will be truncated to {@code long} index values
   *
   * @param inputs the producers providing the indices whose elements will take the value of 1 in the resulting vector
   * @return the configured transformer
   */
  public T fromIndices(List<? extends Producer<? extends Number>> inputs) {
    return fromVector(new ManyHotVector().withInputs(inputs));
  }

  /**
   * Configures the input as a vector with a value of 1 for each provided index number (using the {@link ManyHotVector}
   * transformer); floating-point numbers will be truncated to {@code long} index values
   *
   * @param input the producer providing the indices whose elements will take the value of 1 in the resulting vector
   * @return the configured transformer
   */
  public T fromIndexArray(Producer<? extends long[]> input) {
    return fromVector(new ManyHotVector().withInputArray(input));
  }

  /**
   * Configures the input as a vector with a value of 1 for each provided index number (using the {@link ManyHotVector}
   * transformer); floating-point numbers will be truncated to {@code long} index values
   *
   * @param input the producer providing the indices whose elements will take the value of 1 in the resulting vector
   * @return the configured transformer
   */
  public T fromIndexList(Producer<? extends Iterable<? extends Number>> input) {
    return fromVector(new ManyHotVector().withInputList(input));
  }

  /**
   * Configures the input as a (dense) vector corresponding to the provided array (the first element of the array will
   * have index 0, the second element index 1, etc.)
   *
   * @param input the producer providing the vector element values
   * @return the configured transformer
   */
  public T fromFloatArray(Producer<? extends float[]> input) {
    return fromDenseVector(
        new FunctionResult1<float[], DenseFloatArrayVector>(DenseFloatArrayVector::wrap).withInput(input));
  }

  /**
   * Configures the input as a (dense) vector corresponding to the provided array (the first element of the array will
   * have index 0, the second element index 1, etc.)
   *
   * @param input the producer providing the vector element values
   * @return the configured transformer
   */
  public T fromDoubleArray(Producer<? extends double[]> input) {
    return fromDenseVector(
        new FunctionResult1<double[], DenseDoubleArrayVector>(DenseDoubleArrayVector::wrap).withInput(input));
  }
}
