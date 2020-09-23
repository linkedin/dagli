package com.linkedin.dagli.nn.dl4j;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG3x1;
import com.linkedin.dagli.dl4j.NeuralNetwork;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import com.linkedin.dagli.nn.activation.Sigmoid;
import com.linkedin.dagli.nn.layer.NNActivationLayer;
import com.linkedin.dagli.nn.layer.NNClassification;
import com.linkedin.dagli.nn.layer.NNDotProductLayer;
import com.linkedin.dagli.nn.layer.NNEmbeddingLayer;
import com.linkedin.dagli.nn.optimizer.AdaMax;
import com.linkedin.dagli.placeholder.Placeholder;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Tests a simple model that uses two very abbreviated "towers" of embeddings to detect if two inputted integers are
 * sequential.
 */
public class SimpleBipartiteModelTest {
  @Test
  public void test() {
    Placeholder<Integer> integer1 = new Placeholder<>();
    Placeholder<Integer> integer2 = new Placeholder<>();
    Placeholder<Boolean> label = new Placeholder<>();

    NNEmbeddingLayer embedding1 = new NNEmbeddingLayer().withInputFromNumber(integer1).withEmbeddingSize(2);
    NNEmbeddingLayer embedding2 = new NNEmbeddingLayer().withInputFromNumber(integer2).withEmbeddingSize(2);

    NNDotProductLayer dotProduct = new NNDotProductLayer().withFirstInput(embedding1).withSecondInput(embedding2);

    NNActivationLayer squashedDotProduct =
        new NNActivationLayer().withActivationFunction(new Sigmoid()).withInput(dotProduct);

    NNClassification<Boolean> isSequential =
        new NNClassification<>().withBinaryLabelInput(label).withPredictionInput(squashedDotProduct);

    NeuralNetwork neuralNetwork =
        new NeuralNetwork().withLossLayers(isSequential).withMaxEpochs(20).withOptimizer(new AdaMax());

    DAG3x1<Integer, Integer, Boolean, DiscreteDistribution<Boolean>> dag =
        DAG.withPlaceholders(integer1, integer2, label).withOutput(neuralNetwork.asLayerOutput(isSequential));

    Random r = new Random(1);
    List<Integer> integerList1 = r.ints(10000, 0, 4).boxed().collect(Collectors.toList());
    List<Integer> integerList2 = r.ints(10000, 0, 4).boxed().collect(Collectors.toList());
    List<Boolean> labelList = IntStream.range(0, 10000)
        .mapToObj(i -> Math.abs(integerList1.get(i) - integerList2.get(i)) == 1)
        .collect(Collectors.toList());

    DAG3x1.Result<Integer, Integer, Boolean, DiscreteDistribution<Boolean>> result =
        dag.prepareAndApply(integerList1, integerList2, labelList);

    Assertions.assertEquals(labelList, DL4JTestUtil.mostLikelyLabels(result.toList()));
  }
}
