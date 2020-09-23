package com.linkedin.dagli.nn.dl4j;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG2x1;
import com.linkedin.dagli.dl4j.NeuralNetwork;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.nn.layer.NNClassification;
import com.linkedin.dagli.nn.layer.NNDenseLayer;
import com.linkedin.dagli.nn.layer.NNLSTMLayer;
import com.linkedin.dagli.nn.layer.NNLastVectorInSequenceLayer;
import com.linkedin.dagli.nn.layer.NNSplitVectorSequenceLayer;
import com.linkedin.dagli.nn.optimizer.StochasticGradientDescent;
import com.linkedin.dagli.placeholder.Placeholder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;


/**
 * Test case that learns a XOR function, a classic problem not solvable by linear models.
 *
 * The xor here is a 3-way xor where the result is true iff exactly one of three features is true.
 */
public class XORTest {
  private static List<DenseVector> createFeatureVectors(int count) {
    Random random = new Random(count);
    ArrayList<DenseVector> result = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      result.add(new DenseFloatArrayVector(random.nextInt(2), random.nextInt(2), random.nextInt(2)));
    }
    return result;
  }

  /**
   * Trains a 3-way xor model using either a multinomial or binary loss layer (both have the same binary output, but
   * the former uses more parameters).
   *
   * @param useBinaryLabelInput whether or not to use the loss layer's "binary" mode, which entails fewer parameters.
   */
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testXOR(boolean useBinaryLabelInput) {
    Placeholder<DenseVector> features = new Placeholder<>("Features");
    Placeholder<Boolean> label = new Placeholder<>("Label");

    // With a single output neuron, the xor function requires two neurons in the hidden layer to learn (e.g. one to
    // detect if at least one feature is "true", and one to detect if multiple features are "true", with the subsequent
    // output neuron predicting true iff the first activation is high and the second is low), but it requires a
    // well-tuned learning rate (and/or a very high number of iterations) relative to using 3+ neurons.  An arguably
    // interesting example of a simpler model being harder to learn despite sufficient expressiveness.
    NNDenseLayer hidden = new NNDenseLayer().withUnitCount(2).withInputFromDenseVector(features);
    NNClassification<Boolean> classification =
        new NNClassification<Boolean>().withFeaturesInput(hidden);
    classification = useBinaryLabelInput ? classification.withBinaryLabelInput(label)
        : classification.withMultinomialLabelInput(label);
    NeuralNetwork neuralNetwork = new NeuralNetwork().withLossLayers(classification)
        .withMaxEpochs(2000)
        .withOptimizer(new StochasticGradientDescent().withLearningRate(0.1));

    DAG2x1<DenseVector, Boolean, DiscreteDistribution<Boolean>> dag = DAG.withPlaceholders(features, label).withOutput(
        neuralNetwork.asLayerOutput(classification));

    List<DenseVector> featuresData = createFeatureVectors(1000);
    List<Boolean> labels = featuresData.stream().map(vec -> vec.norm(1) == 1).collect(Collectors.toList());

    DAG2x1.Prepared<DenseVector, Boolean, DiscreteDistribution<Boolean>> prepared = dag.prepare(featuresData, labels);

    List<DenseVector> evalFeaturesData = createFeatureVectors(100);
    for (DenseVector vec : evalFeaturesData) {
      Assertions.assertEquals(vec.norm(1) == 1, prepared.apply(vec, false).max().get().getLabel());
    }
  }

  /**
   * Trains a 3-way xor LSTM model using either a multinomial or binary loss layer (both have the same binary output,
   * but the former uses more parameters).
   *
   * @param useBinaryLabelInput whether or not to use the loss layer's "binary" mode, which entails fewer parameters.
   */
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testXORLSTM(boolean useBinaryLabelInput) {
    Placeholder<DenseVector> features = new Placeholder<>("Features");
    Placeholder<Boolean> label = new Placeholder<>("Label");

    NNSplitVectorSequenceLayer featureSequence =
        new NNSplitVectorSequenceLayer().withInputFromDenseVector(features).withSplitSize(1);

    NNLSTMLayer lstmLayer = new NNLSTMLayer().withUnitCount(2).withInput(featureSequence);

    NNLastVectorInSequenceLayer lstmFinalOutput = new NNLastVectorInSequenceLayer().withInput(lstmLayer);

    NNClassification<Boolean> classification =
        new NNClassification<Boolean>().withFeaturesInput(lstmFinalOutput);
    classification = useBinaryLabelInput ? classification.withBinaryLabelInput(label)
        : classification.withMultinomialLabelInput(label);
    NeuralNetwork neuralNetwork = new NeuralNetwork().withLossLayers(classification)
        .withMaxEpochs(2000)
        .withOptimizer(new StochasticGradientDescent().withLearningRate(0.1));

    DAG2x1<DenseVector, Boolean, DiscreteDistribution<Boolean>> dag = DAG.withPlaceholders(features, label).withOutput(
        neuralNetwork.asLayerOutput(classification));

    List<DenseVector> featuresData = createFeatureVectors(1000);
    List<Boolean> labels = featuresData.stream().map(vec -> vec.norm(1) == 1).collect(Collectors.toList());

    DAG2x1.Prepared<DenseVector, Boolean, DiscreteDistribution<Boolean>> prepared = dag.prepare(featuresData, labels);

    List<DenseVector> evalFeaturesData = createFeatureVectors(100);
    for (DenseVector vec : evalFeaturesData) {
      Assertions.assertEquals(vec.norm(1) == 1, prepared.apply(vec, false).max().get().getLabel());
    }
  }
}
