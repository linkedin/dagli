package com.linkedin.dagli.nn.dl4j;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG3x2;
import com.linkedin.dagli.dl4j.NeuralNetwork;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.nn.layer.NNDenseLayer;
import com.linkedin.dagli.nn.layer.NNRegression;
import com.linkedin.dagli.nn.optimizer.AdaMax;
import com.linkedin.dagli.placeholder.Placeholder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Tests a model that tries to predict the (Euclidean) normalized element-wise sum of two vectors.
 */
public class SimpleRegressionModelTest {
  @Test
  public void test() {
    Placeholder<Vector> vec1 = new Placeholder<>();
    Placeholder<Vector> vec2 = new Placeholder<>();
    Placeholder<Vector> label = new Placeholder<>();

    NNDenseLayer denseLayer1A = new NNDenseLayer().withUnitCount(16).withInputFromTruncatedVector(3, vec1);
    NNDenseLayer denseLayer2A = new NNDenseLayer().withUnitCount(16).withInputFromTruncatedVector(3, vec2);

    NNDenseLayer denseLayerB = new NNDenseLayer().withInputFromConcatenatedLayers(denseLayer1A, denseLayer2A);
    NNDenseLayer denseLayerC = new NNDenseLayer().withUnitCount(3).withInput(denseLayerB);

    NNRegression normalizedSum = new NNRegression().withPredictionInput(denseLayerC).withLabelInput(label);

    NeuralNetwork neuralNetwork = new NeuralNetwork().withLossLayers(normalizedSum)
        .withMaxEpochs(Long.MAX_VALUE) // early stopping will occur long before this...
        .withOptimizer(new AdaMax())
        .withOutputLayers(normalizedSum, denseLayer1A) // denseLayer1A isn't really useful to us; it's just for testing
        .withEvaluationHoldoutProportion(0.1, 123)
        .withMaxTrainingAmountWithoutImprovement(5)
        .withTrainingProgressLoggingFrequency(1);

    DAG3x2<Vector, Vector, Vector, DenseVector, DenseVector> dag = DAG.withPlaceholders(vec1, vec2, label)
        .withOutputs(neuralNetwork.asLayerOutput(normalizedSum), neuralNetwork.asLayerOutput(denseLayer1A));

    Random r = new Random(1);

    List<DenseVector> vectors1 = new ArrayList<>();
    List<DenseVector> vectors2 = new ArrayList<>();
    List<Vector> labels = new ArrayList<>();

    for (int i = 0; i < 10000; i++) {
      vectors1.add(randomVectorOfSize3(r));
      vectors2.add(randomVectorOfSize3(r));

      Vector labelVec = vectors1.get(i).lazyAdd(vectors2.get(i));
      labels.add(labelVec.lazyDivide(labelVec.norm(2)));
    }

    DAG3x2.Result<Vector, Vector, Vector, DenseVector, DenseVector> result =
        dag.prepareAndApply(vectors1, vectors2, labels);

    for (int i = 0; i < labels.size(); i++) {
      double euclideanDistance = labels.get(i).lazySubtract(result.getResult1().toList().get(i)).norm(2);
      Assertions.assertTrue(euclideanDistance < 0.2, "Excessively bad prediction on example " + i);
    }

    // check to make sure that the dense layer actually has the expected number of non-zero activations
    // (technically, a valid neural network could have a zero activation value from the 16th, but this is exceedingly
    // unlikely.)
    Assertions.assertEquals(result.getResult2().toList().get(0).maxNonZeroElementIndex().getAsLong(), 15L);
  }

  private static DenseVector randomVectorOfSize3(Random r) {
    return new DenseFloatArrayVector(r.nextFloat(), r.nextFloat(), r.nextFloat());
  }
}
