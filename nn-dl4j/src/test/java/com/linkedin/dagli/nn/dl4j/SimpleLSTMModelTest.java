package com.linkedin.dagli.nn.dl4j;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG2x1;
import com.linkedin.dagli.dl4j.NeuralNetwork;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.nn.layer.NNClassification;
import com.linkedin.dagli.nn.layer.NNLSTMLayer;
import com.linkedin.dagli.nn.layer.NNLastVectorInSequenceLayer;
import com.linkedin.dagli.nn.layer.NNLayer;
import com.linkedin.dagli.nn.layer.NNLinearizedVectorSequenceLayer;
import com.linkedin.dagli.nn.layer.NNMaxPoolingLayer;
import com.linkedin.dagli.nn.layer.NNMeanPoolingLayer;
import com.linkedin.dagli.nn.layer.NNPNormPoolingLayer;
import com.linkedin.dagli.nn.layer.NNSequentialEmbeddingLayer;
import com.linkedin.dagli.nn.layer.NNSplitVectorSequenceLayer;
import com.linkedin.dagli.nn.layer.NNSumPoolingLayer;
import com.linkedin.dagli.nn.layer.NonTerminalLayer;
import com.linkedin.dagli.nn.optimizer.AdaMax;
import com.linkedin.dagli.placeholder.Placeholder;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Tests a simple model that uses an LSTM layer to determine if a short sequence of integers is monotonically
 * increasing using a variety of different pooling layers.
 */
public class SimpleLSTMModelTest {
  @Test
  public void test() {
    // These networks should be equivalent:
    test(input -> new NNSumPoolingLayer().withInput(new NNSplitVectorSequenceLayer().withSplitSize(16)
        .withInput(new NNLinearizedVectorSequenceLayer().withInput(input))));
    test(new NNSumPoolingLayer()::withInput);

    // These two tests should also correspond to equivalent networks
    test(input -> new NNLinearizedVectorSequenceLayer().withInput(new NNSplitVectorSequenceLayer().withSplitSize(16)
        .withInput(new NNLinearizedVectorSequenceLayer().withInput(input))));
    test(new NNLinearizedVectorSequenceLayer()::withInput);

    test(new NNLastVectorInSequenceLayer()::withInput);
    test(new NNMaxPoolingLayer()::withInput);
    test(new NNMeanPoolingLayer()::withInput);
    test(new NNPNormPoolingLayer()::withInput);
  }

  public void test(
      Function<NNLayer<List<DenseVector>, ? extends NonTerminalLayer>, NNLayer<DenseVector, ? extends NonTerminalLayer>> poolingSupplier) {
    Placeholder<List<Integer>> integerSeq = new Placeholder<>();
    Placeholder<Boolean> label = new Placeholder<>();

    NNSequentialEmbeddingLayer sequenceEmbedding =
        new NNSequentialEmbeddingLayer().withEmbeddingSize(4).withInputFromNumberSequence(integerSeq);

    NNLSTMLayer lstm = new NNLSTMLayer().withUnitCount(16).withInput(sequenceEmbedding);

    NNLayer<DenseVector, ? extends NonTerminalLayer> poolingLayer = poolingSupplier.apply(lstm);

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
