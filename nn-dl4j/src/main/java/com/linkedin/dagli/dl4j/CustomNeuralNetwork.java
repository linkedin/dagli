package com.linkedin.dagli.dl4j;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.mdarray.MDArray;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerDynamic;
import com.linkedin.dagli.producer.Producer;
import java.util.Map;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;


/**
 * Basic implementation of {@link AbstractCustomNeuralNetwork} wrapping a client-supplied computation graph with no
 * predetermined inputs and outputs.
 *
 * If you have a known neural network architecture, prefer to implement {@link AbstractCustomNeuralNetwork} yourself so
 * the correct typing of the inputs and outputs can be statically enforced.
 */
@ValueEquality
public class CustomNeuralNetwork extends AbstractCustomNeuralNetwork<AbstractCustomNeuralNetwork.Prepared, CustomNeuralNetwork> {
  private static final long serialVersionUID = 1;

  @Override
  protected PreparerDynamic<DL4JResult, Prepared> getPreparer(PreparerContext context) {
    return new Preparer<CustomNeuralNetwork>(this);
  }

  @Override
  public CustomNeuralNetwork withOutputs(String... outputNames) {
    return super.withOutputs(outputNames);
  }

  @Override
  public CustomNeuralNetwork withFeaturesInputFromNumber(String inputName, Producer<? extends Number> producer,
      DataType dataType) {
    return super.withFeaturesInputFromNumber(inputName, producer, dataType);
  }

  @Override
  public CustomNeuralNetwork withFeaturesInputFromNumberSequence(String inputName,
      Producer<? extends Iterable<? extends Number>> producer, long sequenceLength, DataType dataType) {
    return super.withFeaturesInputFromNumberSequence(inputName, producer, sequenceLength, dataType);
  }

  @Override
  public CustomNeuralNetwork withFeaturesInputFromVector(String inputName, Producer<? extends Vector> producer,
      long vectorLength, DataType dataType) {
    return super.withFeaturesInputFromVector(inputName, producer, vectorLength, dataType);
  }

  @Override
  public CustomNeuralNetwork withFeaturesInputFromVectorSequence(String inputName,
      Producer<? extends Iterable<? extends Vector>> producer, long sequenceLength, long vectorLength,
      DataType dataType) {
    return super.withFeaturesInputFromVectorSequence(inputName, producer, sequenceLength, vectorLength, dataType);
  }

  @Override
  public CustomNeuralNetwork withFeaturesInputFromMDArray(String inputName, Producer<? extends MDArray> producer,
      long[] shape, DataType dataType) {
    return super.withFeaturesInputFromMDArray(inputName, producer, shape, dataType);
  }

  @Override
  public CustomNeuralNetwork withFeaturesInputFromMDArraySequence(String inputName,
      Producer<? extends Iterable<? extends MDArray>> producer, long sequenceLength, long[] shape, DataType dataType) {
    return super.withFeaturesInputFromMDArraySequence(inputName, producer, sequenceLength, shape, dataType);
  }

  @Override
  public CustomNeuralNetwork withLabelInputFromNumber(String outputName, Producer<? extends Number> producer,
      DataType dataType) {
    return super.withLabelInputFromNumber(outputName, producer, dataType);
  }

  @Override
  public CustomNeuralNetwork withLabelInputFromNumberSequence(String outputName,
      Producer<? extends Iterable<? extends Number>> producer, long sequenceLength, DataType dataType) {
    return super.withLabelInputFromNumberSequence(outputName, producer, sequenceLength, dataType);
  }

  @Override
  public CustomNeuralNetwork withLabelInputFromVector(String outputName, Producer<? extends Vector> producer,
      long vectorLength, DataType dataType) {
    return super.withLabelInputFromVector(outputName, producer, vectorLength, dataType);
  }

  @Override
  public CustomNeuralNetwork withLabelInputFromVectorSequence(String outputName,
      Producer<? extends Iterable<? extends Vector>> producer, long sequenceLength, long vectorLength,
      DataType dataType) {
    return super.withLabelInputFromVectorSequence(outputName, producer, sequenceLength, vectorLength, dataType);
  }

  @Override
  public CustomNeuralNetwork withLabelInputFromMDArray(String outputName, Producer<? extends MDArray> producer,
      long[] shape, DataType dataType) {
    return super.withLabelInputFromMDArray(outputName, producer, shape, dataType);
  }

  @Override
  public CustomNeuralNetwork withLabelInputFromMDArraySequence(String outputName,
      Producer<? extends Iterable<? extends MDArray>> producer, long sequenceLength, long[] shape, DataType dataType) {
    return super.withLabelInputFromMDArraySequence(outputName, producer, sequenceLength, shape, dataType);
  }

  @Override
  public CustomNeuralNetwork withComputationGraph(ComputationGraph computationGraph) {
    return super.withComputationGraph(computationGraph);
  }

  @Override
  public CustomNeuralNetwork withMaxEpochs(long maxEpochs) {
    return super.withMaxEpochs(maxEpochs);
  }

  @Override
  public CustomNeuralNetwork withNoConcurrentInference() {
    return super.withNoConcurrentInference();
  }

  @Override
  public CustomNeuralNetwork withMinibatchSize(int minibatchSize) {
    return super.withMinibatchSize(minibatchSize);
  }

  @Override
  public CustomNeuralNetwork withMinibatchSizeForInference(int minibatchSize) {
    return super.withMinibatchSizeForInference(minibatchSize);
  }

  @Override
  public Producer<DenseVector> asLayerOutputVector(String outputName) {
    return super.asLayerOutputVector(outputName);
  }

  @Override
  public Producer<MDArray> asLayerOutputMDArray(String outputName) {
    return super.asLayerOutputMDArray(outputName);
  }

  @Override
  public Producer<INDArray> asLayerOutputINDArray(String outputName) {
    return super.asLayerOutputINDArray(outputName);
  }

  @Override
  public Producer<Map<String, ? extends MDArray>> asLayerParameters(String layer) {
    return super.asLayerParameters(layer);
  }
}
