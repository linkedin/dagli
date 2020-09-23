package com.linkedin.dagli.nn.dl4j;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG2x1;
import com.linkedin.dagli.dl4j.NeuralNetwork;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.nn.layer.NNClassification;
import com.linkedin.dagli.nn.layer.NNLayer;
import com.linkedin.dagli.nn.layer.NNLearnedSelfAttentionLayer;
import com.linkedin.dagli.nn.layer.NNLinearizedVectorSequenceLayer;
import com.linkedin.dagli.nn.layer.NNMaxPoolingLayer;
import com.linkedin.dagli.nn.layer.NNMeanPoolingLayer;
import com.linkedin.dagli.nn.layer.NNPNormPoolingLayer;
import com.linkedin.dagli.nn.layer.NNPositionalEncodedLayer;
import com.linkedin.dagli.nn.layer.NNRecurrentAttentionLayer;
import com.linkedin.dagli.nn.layer.NNSelfAttentionLayer;
import com.linkedin.dagli.nn.layer.NNSequentialEmbeddingLayer;
import com.linkedin.dagli.nn.layer.NonTerminalLayer;
import com.linkedin.dagli.nn.optimizer.AdaMax;
import com.linkedin.dagli.placeholder.Placeholder;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;


/**
 * Tests a simple model that uses an LSTM layer to determine if a short sequence of integers is monotonically
 * increasing using a variety of different pooling layers.
 */
public class SimpleAttentionModelTest {
  @Test
  public void test() {

  }

  static Stream<Arguments> testArgs() {
    List<Function<NNLayer<List<DenseVector>, ? extends NonTerminalLayer>, NNLayer<List<DenseVector>, ? extends NonTerminalLayer>>>
        attentionSuppliers =
        Arrays.asList(new NNSelfAttentionLayer()::withInput, new NNRecurrentAttentionLayer()::withInput,
            new NNLearnedSelfAttentionLayer()::withInput);

    List<Function<NNLayer<List<DenseVector>, ? extends NonTerminalLayer>, NNLayer<DenseVector, ? extends NonTerminalLayer>>>
        poolingSuppliers =
        Arrays.asList(new NNLinearizedVectorSequenceLayer()::withInput, new NNMaxPoolingLayer()::withInput,
            new NNMeanPoolingLayer()::withInput, new NNPNormPoolingLayer()::withInput);

    return attentionSuppliers.stream()
        .flatMap(attentionSupplier -> poolingSuppliers.stream()
            .map(poolingSupplier -> Arguments.of(attentionSupplier, poolingSupplier)));
  }

  @ParameterizedTest
  @MethodSource("testArgs")
  public void test(
      Function<NNLayer<List<DenseVector>, ? extends NonTerminalLayer>, NNLayer<List<DenseVector>, ? extends NonTerminalLayer>> attentionSupplier,
      Function<NNLayer<List<DenseVector>, ? extends NonTerminalLayer>, NNLayer<DenseVector, ? extends NonTerminalLayer>> poolingSupplier) {
    Placeholder<List<Integer>> integerSeq = new Placeholder<>();
    Placeholder<Boolean> label = new Placeholder<>();

    NNSequentialEmbeddingLayer sequenceEmbedding =
        new NNSequentialEmbeddingLayer().withEmbeddingSize(4).withInputFromNumberSequence(integerSeq);

    NNPositionalEncodedLayer positionalSequenceEmbedding =
        new NNPositionalEncodedLayer().withInput(sequenceEmbedding);

    NNLayer<List<DenseVector>, ? extends NonTerminalLayer> attentionLayer =
        attentionSupplier.apply(positionalSequenceEmbedding);
    NNLayer<DenseVector, ? extends NonTerminalLayer> poolingLayer = poolingSupplier.apply(attentionLayer);

    NNClassification<Boolean> isMonotonicIncreasing =
        new NNClassification<>().withFeaturesInput(poolingLayer).withBinaryLabelInput(label);

    NeuralNetwork neuralNetwork =
        new NeuralNetwork().withLossLayers(isMonotonicIncreasing).withMaxEpochs(10).withOptimizer(new AdaMax());

    DAG2x1<List<Integer>, Boolean, DiscreteDistribution<Boolean>> dag =
        DAG.withPlaceholders(integerSeq, label).withOutput(neuralNetwork.asLayerOutput(isMonotonicIncreasing));

    final int maxSeqLength = 5;
    final int maxValue = 10;
    Random r = new Random(1);
    List<List<Integer>> integerSeqs = r.ints(10000, 0, maxSeqLength)
        .mapToObj(j -> r.ints(j + 1, 0, maxValue).boxed().collect(Collectors.toList()))
        .collect(Collectors.toList());

    List<Boolean> labelList = integerSeqs.stream().map(values -> {
      int previousElement = values.get(0);
      for (int i = 1; i < values.size(); i++) {
        if (values.get(i) < previousElement) {
          return false;
        }
        previousElement = values.get(i);
      }
      return true;
    }).collect(Collectors.toList());

    DAG2x1.Result<List<Integer>, Boolean, DiscreteDistribution<Boolean>> result =
        dag.prepareAndApply(integerSeqs, labelList);

    // it takes too long to train a perfectly accurate classifier--doing twice as well as a mode-predicting baseline is
    // enough for testing purposes
    Assertions.assertTrue(
        DL4JTestUtil.errorRelativeToModeBaseline(labelList, DL4JTestUtil.mostLikelyLabels(result.toList()))
            < 0.5);
  }
}
