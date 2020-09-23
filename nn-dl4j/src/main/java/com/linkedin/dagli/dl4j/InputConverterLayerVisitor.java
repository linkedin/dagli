package com.linkedin.dagli.dl4j;

import com.linkedin.dagli.nn.FloatingPointPrecision;
import com.linkedin.dagli.nn.layer.NNActivationLayer;
import com.linkedin.dagli.nn.layer.NNBatchNormalizedLayer;
import com.linkedin.dagli.nn.layer.NNClassification;
import com.linkedin.dagli.nn.layer.NNDenseLayer;
import com.linkedin.dagli.nn.layer.NNDotProductLayer;
import com.linkedin.dagli.nn.layer.NNSequentialDenseLayer;
import com.linkedin.dagli.nn.layer.NNVectorHadamardProductLayer;
import com.linkedin.dagli.nn.layer.NNEmbeddingLayer;
import com.linkedin.dagli.nn.layer.NNIntegerInputLayer;
import com.linkedin.dagli.nn.layer.NNIntegerSequenceInputLayer;
import com.linkedin.dagli.nn.layer.NNLSTMLayer;
import com.linkedin.dagli.nn.layer.NNLastVectorInSequenceLayer;
import com.linkedin.dagli.nn.layer.NNLayer;
import com.linkedin.dagli.nn.layer.DynamicLayerConfig;
import com.linkedin.dagli.nn.layer.NNLayerVisitor;
import com.linkedin.dagli.nn.layer.NNLearnedSelfAttentionLayer;
import com.linkedin.dagli.nn.layer.NNLinearizedVectorSequenceLayer;
import com.linkedin.dagli.nn.layer.NNMaxPoolingLayer;
import com.linkedin.dagli.nn.layer.NNMeanPoolingLayer;
import com.linkedin.dagli.nn.layer.NNPNormPoolingLayer;
import com.linkedin.dagli.nn.layer.NNPositionalEncodedLayer;
import com.linkedin.dagli.nn.layer.NNRecurrentAttentionLayer;
import com.linkedin.dagli.nn.layer.NNRegression;
import com.linkedin.dagli.nn.layer.NNSelfAttentionLayer;
import com.linkedin.dagli.nn.layer.NNSequentialEmbeddingLayer;
import com.linkedin.dagli.nn.layer.NNSplitVectorSequenceLayer;
import com.linkedin.dagli.nn.layer.NNSumPoolingLayer;
import com.linkedin.dagli.nn.layer.NNVectorConcatenationLayer;
import com.linkedin.dagli.nn.layer.NNVectorConcatenationSequenceLayer;
import com.linkedin.dagli.nn.layer.NNVectorInputLayer;
import com.linkedin.dagli.nn.layer.NNVectorMeanLayer;
import com.linkedin.dagli.nn.layer.NNVectorSequenceHadamardProductLayer;
import com.linkedin.dagli.nn.layer.NNVectorSequenceInputLayer;
import com.linkedin.dagli.nn.layer.NNVectorSequenceMeanLayer;
import com.linkedin.dagli.nn.layer.NNVectorSequenceSumLayer;
import com.linkedin.dagli.nn.layer.NNVectorSumLayer;
import com.linkedin.dagli.transformer.DynamicInputs;
import java.util.Map;
import org.nd4j.linalg.api.buffer.DataType;


public class InputConverterLayerVisitor implements NNLayerVisitor<AbstractInputConverter<?, ?>> {
  private final DynamicInputs _dynamicInputs;
  private final Map<NNLayer<?, ?>, DynamicLayerConfig> _dynamicConfigs;
  private final FloatingPointPrecision _floatingPointPrecision;

  public InputConverterLayerVisitor(DynamicInputs dynamicInputs, Map<NNLayer<?, ?>, DynamicLayerConfig> dynamicConfigs,
      FloatingPointPrecision floatingPointPrecision) {
    _dynamicInputs = dynamicInputs;
    _dynamicConfigs = dynamicConfigs;
    _floatingPointPrecision = floatingPointPrecision;
  }

  @Override
  public <L> AbstractInputConverter<?, ?> visit(NNClassification<L> visited) {
    return new VectorInputConverter(_dynamicInputs.get(visited.internalAPI().getLabelVectorProducer()),
        _dynamicConfigs.get(visited.internalAPI().getInputLayer()).getOutputElementCount(),
        DL4JUtil.toDataType(_floatingPointPrecision));
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNLSTMLayer visited) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNVectorConcatenationLayer visited) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNVectorConcatenationSequenceLayer visited) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNEmbeddingLayer visited) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNVectorSequenceInputLayer visited) {
    long[] shape = _dynamicConfigs.get(visited).getOutputShape();

    return new VectorSequenceInputConverter(_dynamicInputs.get(visited.internalAPI().getInputProducer()), shape[0],
        shape[1], DL4JUtil.toDataType(_floatingPointPrecision));
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNIntegerInputLayer visited) {
    return new NumberInputConverter(_dynamicInputs.get(visited.internalAPI().getInputProducer()), DataType.LONG);
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNIntegerSequenceInputLayer visited) {
    return new NumberSequenceInputConverter(_dynamicInputs.get(visited.internalAPI().getInputProducer()),
        _dynamicConfigs.get(visited).getOutputElementCount(), DataType.LONG);
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNRegression visited) {
    return new VectorInputConverter(_dynamicInputs.get(visited.internalAPI().getLabelVectorProducer()),
        _dynamicConfigs.get(visited.internalAPI().getInputLayer()).getOutputElementCount(),
        DL4JUtil.toDataType(_floatingPointPrecision));
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNDotProductLayer visited) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNDenseLayer visited) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNSequentialEmbeddingLayer visited) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNVectorInputLayer visited) {
    return new VectorInputConverter(_dynamicInputs.get(visited.internalAPI().getInputProducer()),
        _dynamicConfigs.get(visited).getOutputElementCount(), DL4JUtil.toDataType(_floatingPointPrecision));
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNVectorHadamardProductLayer visited) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNActivationLayer visited) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNSumPoolingLayer visited) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNMaxPoolingLayer visited) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNMeanPoolingLayer visited) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNPNormPoolingLayer visited) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNLastVectorInSequenceLayer visited) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNLinearizedVectorSequenceLayer visited) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNSplitVectorSequenceLayer visited) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNSelfAttentionLayer visited) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNPositionalEncodedLayer visited) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNRecurrentAttentionLayer visited) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNLearnedSelfAttentionLayer visited) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNSequentialDenseLayer visited) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNBatchNormalizedLayer visited) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNVectorSequenceHadamardProductLayer visited) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNVectorMeanLayer visited) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNVectorSequenceSumLayer visited) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNVectorSequenceMeanLayer visited) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractInputConverter<?, ?> visit(NNVectorSumLayer visited) {
    throw new UnsupportedOperationException();
  }
}
