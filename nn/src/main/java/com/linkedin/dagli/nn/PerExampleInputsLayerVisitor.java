package com.linkedin.dagli.nn;

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
import com.linkedin.dagli.producer.Producer;
import java.util.Collections;
import java.util.List;


/**
 * Visits each layer to retrieve the original per-example inputs for that layer, if any.
 */
class PerExampleInputsLayerVisitor implements NNLayerVisitor<List<? extends Producer<?>>> {
  @Override
  public <L> List<? extends Producer<?>> visit(NNClassification<L> visitee) {
    return Collections.singletonList(visitee.internalAPI().getLabelVectorProducer());
  }

  @Override
  public List<? extends Producer<?>> visit(NNVectorSequenceInputLayer visitee) {
    return Collections.singletonList(visitee.internalAPI().getInputProducer());
  }

  @Override
  public List<? extends Producer<?>> visit(NNVectorConcatenationLayer visitee) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Producer<?>> visit(NNVectorConcatenationSequenceLayer visitee) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Producer<?>> visit(NNEmbeddingLayer visitee) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Producer<?>> visit(NNLSTMLayer visitee) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Producer<?>> visit(NNIntegerInputLayer visitee) {
    return Collections.singletonList(visitee.internalAPI().getInputProducer());
  }

  @Override
  public List<? extends Producer<?>> visit(NNIntegerSequenceInputLayer visitee) {
    return Collections.singletonList(visitee.internalAPI().getInputProducer());
  }

  @Override
  public List<? extends Producer<?>> visit(NNDenseLayer visitee) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Producer<?>> visit(NNRegression visitee) {
    return Collections.singletonList(visitee.internalAPI().getLabelVectorProducer());
  }

  @Override
  public List<? extends Producer<?>> visit(NNDotProductLayer visited) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Producer<?>> visit(NNSequentialEmbeddingLayer visitee) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Producer<?>> visit(NNVectorInputLayer visitee) {
    return Collections.singletonList(visitee.internalAPI().getInputProducer());
  }

  @Override
  public List<? extends Producer<?>> visit(NNVectorHadamardProductLayer visited) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Producer<?>> visit(NNActivationLayer visited) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Producer<?>> visit(NNSumPoolingLayer visited) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Producer<?>> visit(NNMaxPoolingLayer visited) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Producer<?>> visit(NNMeanPoolingLayer visited) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Producer<?>> visit(NNPNormPoolingLayer visited) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Producer<?>> visit(NNLastVectorInSequenceLayer visited) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Producer<?>> visit(NNLinearizedVectorSequenceLayer visited) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Producer<?>> visit(NNSplitVectorSequenceLayer visited) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Producer<?>> visit(NNSelfAttentionLayer visited) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Producer<?>> visit(NNPositionalEncodedLayer visited) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Producer<?>> visit(NNRecurrentAttentionLayer visited) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Producer<?>> visit(NNLearnedSelfAttentionLayer visited) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Producer<?>> visit(NNSequentialDenseLayer visited) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Producer<?>> visit(NNBatchNormalizedLayer visited) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Producer<?>> visit(NNVectorSequenceHadamardProductLayer visited) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Producer<?>> visit(NNVectorMeanLayer visited) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Producer<?>> visit(NNVectorSequenceSumLayer visited) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Producer<?>> visit(NNVectorSequenceMeanLayer visited) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Producer<?>> visit(NNVectorSumLayer visited) {
    return Collections.emptyList();
  }
}
