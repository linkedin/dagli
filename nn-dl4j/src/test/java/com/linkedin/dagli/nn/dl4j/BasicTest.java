package com.linkedin.dagli.nn.dl4j;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG2x1;
import com.linkedin.dagli.dl4j.NeuralNetwork;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.nn.activation.Identity;
import com.linkedin.dagli.nn.layer.NNDenseLayer;
import com.linkedin.dagli.nn.layer.NNRegression;
import com.linkedin.dagli.nn.optimizer.StochasticGradientDescent;
import com.linkedin.dagli.objectio.ConstantReader;
import com.linkedin.dagli.placeholder.Placeholder;
import org.junit.jupiter.api.Test;


/**
 * Makes sure that a neural network can be trained without throwing an exception.
 */
public class BasicTest {
  @Test
  public void dagTest() {
    Placeholder<DenseVector> labels = new Placeholder<>();
    Placeholder<DenseVector> features = new Placeholder<>();

    NNRegression regression = new NNRegression().withPredictionInput(
        new NNDenseLayer().withActivationFunction(new Identity()).withInputFromDenseVector(features))
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
