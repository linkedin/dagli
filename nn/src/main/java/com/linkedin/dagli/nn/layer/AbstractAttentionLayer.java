package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.nn.result.NNResult;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.DynamicInputs;
import com.linkedin.dagli.util.invariant.Arguments;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * Base class for dot-product attention layers using queries, keys, and values.
 */
abstract class AbstractAttentionLayer<S extends AbstractAttentionLayer<S>>
    extends AbstractUnaryVectorSequenceLayer<List<DenseVector>, S> implements NonTerminalLayer {
  private static final long serialVersionUID = 1;

  protected Producer<? extends Number> _queryCountProvider = new Constant<>(-1);
  protected Producer<? extends Number> _headCountProvider = new Constant<>(1);
  protected Producer<? extends Number> _headSizeProvider = new Constant<>(-1);
  protected Producer<? extends Number> _outputSizeProvider = new Constant<>(-1);
  protected Producer<? extends Boolean> _isProjectedProvider = new Constant<>(true);

  protected double _dropoutProbability = Double.NaN;

  /**
   * Returns a copy of this instance that will use the specified dropout rate.
   *
   * Each time an example is processed by the network, each element (number) in the input values passed to this layer
   * will have this (independent) probability of being "dropped" (set to 0), which can help mitigate overfitting.
   *
   * The default value, NaN, means "use the neural network's global dropout rate" (which itself defaults to 0).
   * Otherwise, values may range from 0 (no dropout) to 1 (drop everything, which is definitely not a good idea).
   *
   * @param probability a [0, 1] dropout probability, or NaN to use the global network rate
   * @return a copy of this instance that will use the specified dropout rate.
   */
  public S withDropoutProbability(double probability) {
    Arguments.check(Double.isNaN(probability) || (probability >= 0 && probability <= 1), "Invalid probability");
    return clone(c -> c._dropoutProbability = probability);
  }

  /**
   * @return the [0, 1] dropout probability for this layer, or NaN if this layer should use the neural network's global
   *         dropout rate
   */
  public double getDropoutProbability() {
    return _dropoutProbability;
  }

  /**
   * Returns a copy of this instance that will use the number of (learned) queries provided by the given
   * constant-result input (i.e. {@link Producer#hasConstantResult()} must be true for the given input).
   *
   * The default number of queries is -1, meaning "use a number of queries equal to the (maximum) length of the input
   * sequence".
   *
   * @param queryCountProvider the constant-result producer providing the query count hyperparameter value
   * @return a copy of this instance that will use the number of (learned) queries provided by the given constant-result
   *         input
   */
  protected S withQueryCount(Producer<? extends Number> queryCountProvider) {
    Arguments.check(queryCountProvider.hasConstantResult(), "Input must be constant-result");
    return clone(c -> c._queryCountProvider = queryCountProvider);
  }

  /**
   * Returns a copy of this instance that will use the specified number of (learned) queries.
   *
   * The default number of queries is -1, meaning "use a number of queries equal to the (maximum) length of the input
   * sequence".
   *
   * @param queryCount the query count to use
   * @return a copy of this instance that will use the specified number of (learned) queries
   */
  protected S withQueryCount(long queryCount) {
    return withQueryCount(new Constant<>(queryCount));
  }


  /**
   * Returns a copy of this instance that will use the number of heads provided by the given
   * constant-result input (i.e. {@link Producer#hasConstantResult()} must be true for the given input).
   *
   * The default number of heads is 1.
   *
   * @param headCountProvider the constant-result producer providing the head count hyperparameter value
   * @return a copy of this instance that will use the number of heads provided by the given constant-result
   *         input
   */
  public S withHeadCount(Producer<? extends Number> headCountProvider) {
    Arguments.check(headCountProvider.hasConstantResult(), "Input must be constant-result");
    return clone(c -> c._headCountProvider = headCountProvider);
  }

  /**
   * Returns a copy of this instance that will use the specified number of heads.
   *
   * The default number of heads is 1.
   *
   * @param headCount the head count hyperparameter value
   * @return a copy of this instance that will use the specified number of heads
   */
  public S withHeadCount(long headCount) {
    return withHeadCount(new Constant<>(headCount));
  }


  /**
   * Returns a copy of this instance that will use the head size provided by the given
   * constant-result input (i.e. {@link Producer#hasConstantResult()} must be true for the given input).
   *
   * The default head size is -1, meaning "use a head size equal to the size of each vector in the input sequence" (i.e.
   * if the input is a sequence of vectors each with 32 elements, the head size will be 32).
   *
   * @param headSizeProvider the constant-result producer providing the head size hyperparameter value
   * @return a copy of this instance that will use the head size provided by the given constant-result input
   */
  public S withHeadSize(Producer<? extends Number> headSizeProvider) {
    Arguments.check(headSizeProvider.hasConstantResult(), "Input must be constant-result");
    return clone(c -> c._headSizeProvider = headSizeProvider);
  }

  /**
   * Returns a copy of this instance that will use the specified head size.
   *
   * The default head size is -1, meaning "use a head size equal to the size of each vector in the input sequence" (i.e.
   * if the input is a sequence of vectors each with 32 elements, the head size will be 32).
   *
   * @param headSize the head size hyperparameter value
   * @return a copy of this instance that will use the specified head size
   */
  public S withHeadSize(long headSize) {
    return withHeadSize(new Constant<>(headSize));
  }

  /**
   * Returns a copy of this instance that will use the output size provided by the given constant-result input (i.e.
   * {@link Producer#hasConstantResult()} must be true for the given input).
   *
   * The output size is the size of each vector in the sequence of vectors outputted by the attention layer.
   *
   * The default output size is -1, meaning "use an output size equal to [number of heads] * [head size]".
   *
   * @param outputSizeProvider the constant-result producer providing the output size hyperparameter value
   * @return a copy of this instance that will use the output size provided by the given constant-result input
   */
  public S withOutputSize(Producer<? extends Number> outputSizeProvider) {
    Arguments.check(outputSizeProvider.hasConstantResult(), "Input must be constant-result");
    return clone(c -> c._outputSizeProvider = outputSizeProvider);
  }

  /**
   * Returns a copy of this instance that will use the specified output size.
   *
   * The output size is the size of each vector in the sequence of vectors outputted by the attention layer.
   *
   * The default output size is -1, meaning "use an output size equal to [number of heads] * [head size]".
   *
   * @param outputSize the output size hyperparameter value
   * @return a copy of this instance that will use the specified output size
   */
  public S withOutputSize(long outputSize) {
    return withOutputSize(new Constant<>(outputSize));
  }

  /**
   * Returns a copy of this instance that will use (or not use) projection according the given boolean constant-result
   * input ({@link Producer#hasConstantResult()} must be true for the given input).
   *
   * By default, projection <i>will</i> be used.  Some hyperparameter combinations are not possible without projection,
   * depending on the specific type of attention being employed.  However, projection also entails projection weight
   * matrices, which increase the total number of model parameters.
   *
   * @param useProjectionProvider the constant-result producer providing a boolean dictating whether or not projection
   *                              should be employed
   * @return a copy of this instance that will use (or not use) projection according to the given constant-result input
   */
  public S withProjection(Producer<? extends Boolean> useProjectionProvider) {
    Arguments.check(useProjectionProvider.hasConstantResult(), "Input must be constant-result");
    return clone(c -> c._isProjectedProvider = useProjectionProvider);
  }

  /**
   * Returns a copy of this instance that will use (or not use) projection as specified.
   *
   * By default, projection <i>will</i> be used.  Some hyperparameter combinations are not possible without projection,
   * depending on the specific type of attention being employed.  However, projection also entails projection weight
   * matrices, which increase the total number of model parameters.
   *
   * @param useProjection a boolean dictating whether or not projection should be employed
   * @return a copy of this instance that will use (or not use) projection
   */
  public S withProjection(boolean useProjection) {
    return withProjection(new Constant<>(useProjection));
  }

  @Override
  List<? extends Producer<?>> getDynamicConfigurationInputProducers() {
    return Arrays.asList(_queryCountProvider, _headCountProvider, _headSizeProvider, _outputSizeProvider,
        _isProjectedProvider);
  }

  @Override
  Producer<List<DenseVector>> outputFromNNResult(Producer<? extends NNResult> nnResultProducer, int outputIndex) {
    return NNResult.InternalAPI.toVectorSequence(nnResultProducer, outputIndex);
  }

  @Override
  AttentionLayerConfig getDynamicConfig(Map<NNLayer<?, ?>, DynamicLayerConfig> ancestorConfigs,
      DynamicInputs dynamicInputs, DynamicInputs.ConstantInputs constantInputs) {
    DynamicLayerConfig parentConfig = ancestorConfigs.get(this.getInputLayer());

    long queryCount = constantInputs.get(_queryCountProvider).longValue();
    long headCount = constantInputs.get(_headCountProvider).longValue();
    long headSize = constantInputs.get(_headSizeProvider).longValue();
    long outputSize = constantInputs.get(_outputSizeProvider).longValue();
    boolean isProjected = constantInputs.get(_isProjectedProvider).booleanValue();

    if (queryCount == -1) {
      // the vectors of the input sequence are our queries; the query count is thus the length of the input sequence:
      queryCount = parentConfig.getOutputShape()[0];
    }

    if (headSize == -1) {
      // head size defaults to the size of each vector in the input sequence
      headSize = parentConfig.getOutputShape()[1];
    }

    if (outputSize == -1) {
      // output size defaults to [head count] * [head size]
      outputSize = headCount * headSize;
    }

    return AttentionLayerConfig.Builder.setOutputShape(new long[] { queryCount, outputSize })
        .setQueryCount(queryCount)
        .setHeadCount(headCount)
        .setHeadSize(headSize)
        .setOutputSize(outputSize)
        .setIsProjected(isProjected)
        .build();
  }
}
