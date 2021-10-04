package com.linkedin.dagli.nn.dl4j;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG2x1;
import com.linkedin.dagli.dl4j.NeuralNetwork;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.nn.activation.Identity;
import com.linkedin.dagli.nn.layer.NNClassification;
import com.linkedin.dagli.nn.layer.NNDenseLayer;
import com.linkedin.dagli.nn.layer.NNMaxPoolingLayer;
import com.linkedin.dagli.nn.layer.NNRegression;
import com.linkedin.dagli.nn.layer.NNSplitVectorSequenceLayer;
import com.linkedin.dagli.nn.optimizer.StochasticGradientDescent;
import com.linkedin.dagli.objectio.ConstantReader;
import com.linkedin.dagli.placeholder.Placeholder;
import java.util.Arrays;
import org.junit.jupiter.api.Test;


/**
 * Makes sure that a neural network can be trained without throwing an exception.
 */
public class BasicTest {
  /**
   * Creates a NN with duplicate (i.e. {@link Object#equals(Object)}) ancestors.  This is very much a corner case, but
   * it helps guard against an incorrect assumption that all layer object instances in the neural network's graph must
   * be logically distinct.
   */
  @Test
  public void testDuplicateAncestors() {
    Placeholder<Integer> integerPlaceholder = new Placeholder<>();
    Placeholder<Boolean> booleanPlaceholder = new Placeholder<>();

    NNSplitVectorSequenceLayer split = new NNSplitVectorSequenceLayer()
        .withSplitSize(2)
        .withInput().concatenating()
          .fromNumbers(integerPlaceholder)
          .fromNumbers(integerPlaceholder)
          .fromNumbers(integerPlaceholder)
          .fromNumbers(integerPlaceholder)
        .done();

    NNMaxPoolingLayer poolingLayer = new NNMaxPoolingLayer().withInput(split);

    NNClassification<Boolean> labelClassification =
        new NNClassification<Boolean>().withFeaturesInput(poolingLayer).withBinaryLabelInput(booleanPlaceholder);

    NeuralNetwork neuralNetwork = new NeuralNetwork().withLossLayers(labelClassification).withMaxEpochs(5);

    DAG2x1<Integer, Boolean, DiscreteDistribution<Boolean>> dag =
        DAG.withPlaceholders(integerPlaceholder, booleanPlaceholder)
            .withOutput(neuralNetwork.asLayerOutput(labelClassification));
    dag.prepare(Arrays.asList(1, 2, 3, 4), Arrays.asList(true, false, true, false));
  }

  @Test
  public void dagTest() {
    Placeholder<DenseVector> labels = new Placeholder<>();
    Placeholder<DenseVector> features = new Placeholder<>();

    NNRegression regression = new NNRegression().withPredictionInput(
        new NNDenseLayer().withActivationFunction(new Identity()).withInput().fromDenseVectors(features))
        .withLabelInput(labels);

    NeuralNetwork nn = new NeuralNetwork().withLossLayers(regression)
        .withMinibatchSize(64)
        .withMaxEpochs(10)
        .withOptimizer(new StochasticGradientDescent().withLearningRate(0.01));

    DAG2x1<DenseVector, DenseVector, DenseVector> nnDag =
        DAG.withPlaceholders(labels, features).withOutput(nn.asLayerOutput(regression));
    DAG2x1.Prepared<DenseVector, DenseVector, DenseVector> res = nnDag.prepare(
        new ConstantReader<>(new DenseFloatArrayVector(2f, 1f), 1000),
        new ConstantReader<>(new DenseFloatArrayVector(1f, 2f, 3f), 1000)
    );
  }
}
