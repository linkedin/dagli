package com.linkedin.dagli.dl4j;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.math.mdarray.MDArray;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.nn.ParameterStore;
import com.linkedin.dagli.nn.ParameterViewer;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.preparer.AbstractBatchPreparerDynamic;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.preparer.PreparerResultMixed;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparableTransformerDynamic;
import com.linkedin.dagli.transformer.AbstractPreparedStatefulTransformerDynamic;
import com.linkedin.dagli.transformer.DynamicInputs;
import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.tuple.Tuple2;
import com.linkedin.dagli.util.invariant.Arguments;
import com.linkedin.dagli.util.array.ArraysEx;
import com.linkedin.dagli.util.collection.Lists;
import com.linkedin.dagli.util.collection.Maps;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.MultiDataSet;
import org.nd4j.linalg.dataset.api.iterator.MultiDataSetIterator;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;


/**
 * Base class for "custom" DL4J neural networks that cannot be expressed by {@link NeuralNetwork}.  Any DL4J
 * computational graph may be used; {@link AbstractCustomNeuralNetwork} provides {@code protected} methods for
 * configuring the feature and label inputs as well as the outputs.
 *
 * {@link CustomNeuralNetwork} provides an implementation of this class for arbitrary computation graphs whose inputs
 * and outputs can be dynamically defined at runtime.  For specific computation graphs with known architectures, a
 * class deriving from this one can use the protected methods defined here to implement their own public methods
 * corresponding to their specific computational graph, enforcing static type safety.  Training may be similarly
 * customized by subclassing {@link AbstractPreparer}, and the final prepared transformer (used for inference) by
 * subclassing {@link AbstractPrepared}; alternatively {@link Preparer} and {@link Prepared} provide "standard"
 * implementations that should be appropriate for most use cases.
 *
 * @param <N> the type of the prepared transformer yielded by preparing (training) the neural network
 * @param <S> the most-derived class descending from this one
 */
public abstract class AbstractCustomNeuralNetwork<
    N extends AbstractCustomNeuralNetwork.AbstractPrepared<N> & ParameterStore<String>,
    S extends AbstractCustomNeuralNetwork<N, S>> extends AbstractPreparableTransformerDynamic<DL4JResult, N, S> {

  private static final long serialVersionUID = 1;

  private ComputationGraph _computationGraph;
  private long _maxEpochs = 16;
  private int _minibatchSize = 64;
  private int _minibatchSizeForInference = 0; // 0 means "use _minibatchSize"
  private List<String> _outputNames = null; // if null, the computation graph's output layers are used

  // should the prepared transformer use thread-local copies of the model to accomplish concurrent inference?
  private boolean _concurrentInference = true;

  // maps from input and output layer names to their corresponding input-to-INDArray converters
  // LinkedHashMap is used to ensure a consistent iteration order
  private LinkedHashMap<String, Tuple2<Producer<?>, AbstractInputConverter<?, ?>>> _inputConverters =
      new LinkedHashMap<>();
  private LinkedHashMap<String, Tuple2<Producer<?>, AbstractInputConverter<?, ?>>> _labelConverters =
      new LinkedHashMap<>();

  protected AbstractCustomNeuralNetwork() {
    this(null);
  }

  protected AbstractCustomNeuralNetwork(ComputationGraph computationGraph) {
    _computationGraph = computationGraph;
    _inputs = null;
  }

  // We don't store our inputs in the _inputs field, but rather generate them from our converter maps
  @Override
  protected List<? extends Producer<?>> getInputList() {
    return Stream.concat(_inputConverters.values().stream(), _labelConverters.values().stream())
        .map(Tuple2::get0)
        .collect(Collectors.toList());
  }

  @Override
  protected S withInputsUnsafe(List<? extends Producer<?>> newInputs) {
    // replace each converter entry's producer with the newly provided producer in the clone
    int[] i = new int[]{0};
    return clone(c -> Stream.concat(((AbstractCustomNeuralNetwork<?, ?>) c)._inputConverters.entrySet().stream(),
        ((AbstractCustomNeuralNetwork<?, ?>) c)._labelConverters.entrySet().stream())
        .forEach(entry -> entry.setValue(entry.getValue().withValue0(newInputs.get(i[0]++)))));
  }

  @Override
  protected S clone() {
    S clone = super.clone();
    // copy the input and label converter maps
    ((AbstractCustomNeuralNetwork<?, ?>) clone)._inputConverters =
        new LinkedHashMap<>(((AbstractCustomNeuralNetwork<?, ?>) clone)._inputConverters);
    ((AbstractCustomNeuralNetwork<?, ?>) clone)._labelConverters =
        new LinkedHashMap<>(((AbstractCustomNeuralNetwork<?, ?>) clone)._labelConverters);
    return clone;
  }

  private static void addMDArrayConverter(Map<String, Tuple2<Producer<?>, AbstractInputConverter<?, ?>>> converterMap,
      String name, Producer<? extends MDArray> producer, long[] shape, DataType dataType) {
    converterMap.put(name, Tuple2.of(producer, new MDArrayInputConverter(null, shape, dataType)));
  }

  private static void addMDArraySequenceConverter(
      Map<String, Tuple2<Producer<?>, AbstractInputConverter<?, ?>>> converterMap, String name,
      Producer<? extends Iterable<? extends MDArray>> producer, long sequenceLength, long[] shape, DataType dataType) {
    converterMap.put(name,
        Tuple2.of(producer, new MDArraySequenceInputConverter(null, sequenceLength, shape, dataType)));
  }

  private static void addVectorConverter(Map<String, Tuple2<Producer<?>, AbstractInputConverter<?, ?>>> converterMap,
      String name, Producer<? extends Vector> producer, long length, DataType dataType) {
    converterMap.put(name, Tuple2.of(producer, new VectorInputConverter(null, length, dataType)));
  }

  private static void addVectorSequenceConverter(
      Map<String, Tuple2<Producer<?>, AbstractInputConverter<?, ?>>> converterMap, String name,
      Producer<? extends Iterable<? extends Vector>> producer, long sequenceLength, long vectorLength,
      DataType dataType) {
    converterMap.put(name,
        Tuple2.of(producer, new VectorSequenceInputConverter(null, sequenceLength, vectorLength, dataType)));
  }

  private static void addNumberConverter(Map<String, Tuple2<Producer<?>, AbstractInputConverter<?, ?>>> converterMap,
      String name, Producer<? extends Number> producer, DataType dataType) {
    converterMap.put(name, Tuple2.of(producer, new NumberInputConverter(null, dataType)));
  }

  private static void addNumberSequenceConverter(
      Map<String, Tuple2<Producer<?>, AbstractInputConverter<?, ?>>> converterMap, String name,
      Producer<? extends Iterable<? extends Number>> producer, long sequenceLength, DataType dataType) {
    converterMap.put(name, Tuple2.of(producer, new NumberSequenceInputConverter(null, sequenceLength, dataType)));
  }

  private void checkInputName(String inputName) {
    Objects.requireNonNull(_computationGraph, "Computation graph has not yet been set");
    java.util.List<String> inputs = _computationGraph.getConfiguration().getNetworkInputs();
    Arguments.check(inputs.contains(inputName),
        () -> inputName + " is not an input for the computation graph (configured inputs are "
            + inputs + ")");
  }

  private void checkLabelName(String labelName) {
    Objects.requireNonNull(_computationGraph, "Computation graph has not yet been set");
    java.util.List<String> labels = _computationGraph.getConfiguration().getNetworkOutputs();
    Arguments.check(labels.contains(labelName),
        () -> labelName + " is not an output layer for the computation graph (configured output layers are "
            + labels + ")");
  }

  private void checkOutputName(String outputName) {
    if (_outputNames == null) {
      Objects.requireNonNull(_computationGraph, "Computation graph has not yet been set");
      Arguments.check(_computationGraph.getConfiguration().getNetworkOutputs().contains(outputName), () -> outputName
          + " is not an output layer of the computation graph and no other output layers have been specified via "
          + "withOutputLayers(...)");
    } else {
      Arguments.check(_outputNames.contains(outputName),
          () -> outputName + " is not among the output layers specified via withOutputLayers(...)");
    }
  }

  /**
   * @return a list of outputs, either those explicitly set via {@link #withOutputs(String...)} or, if no outputs have
   *         been explicitly set, the output layers defined on the computation graph
   */
  protected List<String> getEffectiveOutputs() {
    Objects.requireNonNull(_computationGraph, "Computation graph has not yet been set");
    return _outputNames == null ? _computationGraph.getConfiguration().getNetworkOutputs() : _outputNames;
  }

  /**
   * Returns a copy of this instance that will produce a result that contains outputs from the specified layers.  The
   * layer names must be nodes in the computation graph.
   *
   * By default, the output layers of the computation graph are used.
   *
   * @param outputNames the layers whose outputs/activations should be included in the result of this neural network
   *                   transfer
   * @return a copy of this instance that will produce results for the specified output layers
   */
  protected S withOutputs(String... outputNames) {
    Objects.requireNonNull(_computationGraph, "Computation graph has not yet been set");

    for (String outputName : outputNames) {
      Arguments.check(_computationGraph.getVertex(outputName) != null,
          outputName + " is not a vertex in the computation graph");
    }
    List<String> outputList = Arrays.asList(outputNames.clone());

    return clone(c -> ((AbstractCustomNeuralNetwork<?, ?>) c)._outputNames = outputList);
  }

  /**
   * Returns a copy of this instance which will accept input values from the specified producer.
   *
   * @param inputName the name of the input whose value is being provided
   * @param producer the producer providing the values
   * @param dataType the data type of the {@link INDArray} that will represent the provided values and be fed into
   *                 neural network
   * @return a copy of this instance that will accept values from the specified producer
   */
  protected S withFeaturesInputFromNumber(String inputName, Producer<? extends Number> producer, DataType dataType) {
    checkInputName(inputName);
    return clone(
        c -> addNumberConverter(((AbstractCustomNeuralNetwork<?, ?>) c)._inputConverters, inputName, producer,
            dataType));
  }

  /**
   * Returns a copy of this instance which will accept input values from the specified producer.
   *
   * @param inputName the name of the input whose value is being provided
   * @param producer the producer providing the values
   * @param sequenceLength the maximum length of the sequence; items in the {@link Iterable} beyond this limit will be
   *                       ignored
   * @param dataType the data type of the {@link INDArray} that will represent the provided values and be fed into
   *                 neural network
   * @return a copy of this instance that will accept values from the specified producer
   */
  protected S withFeaturesInputFromNumberSequence(String inputName, Producer<? extends Iterable<? extends Number>> producer,
      long sequenceLength, DataType dataType) {
    checkInputName(inputName);
    return clone(
        c -> addNumberSequenceConverter(((AbstractCustomNeuralNetwork<?, ?>) c)._inputConverters, inputName,
            producer, sequenceLength, dataType));
  }

  /**
   * Returns a copy of this instance which will accept input values from the specified producer.
   *
   * @param inputName the name of the input whose value is being provided
   * @param producer the producer providing the values
   * @param vectorLength the maximum length the vector; elements with indices less than 0 or greater than or equal
   *                     to {@code vectorLength} will be ignored
   * @param dataType the data type of the {@link INDArray} that will represent the provided values and be fed into
   *                 neural network
   * @return a copy of this instance that will accept values from the specified producer
   */
  protected S withFeaturesInputFromVector(String inputName, Producer<? extends Vector> producer, long vectorLength,
      DataType dataType) {
    checkInputName(inputName);
    return clone(
        c -> addVectorConverter(((AbstractCustomNeuralNetwork<?, ?>) c)._inputConverters, inputName, producer,
            vectorLength, dataType));
  }

  /**
   * Returns a copy of this instance which will accept input values from the specified producer.
   *
   * @param inputName the name of the input whose value is being provided
   * @param producer the producer providing the values
   * @param sequenceLength the maximum length of the sequence; items in the {@link Iterable} beyond this limit will be
   *                       ignored
   * @param vectorLength the maximum length the vector; elements with indices less than 0 or greater than or equal
   *                     to {@code vectorLength} will be ignored
   * @param dataType the data type of the {@link INDArray} that will represent the provided values and be fed into
   *                 neural network
   * @return a copy of this instance that will accept values from the specified producer
   */
  protected S withFeaturesInputFromVectorSequence(String inputName, Producer<? extends Iterable<? extends Vector>> producer,
      long sequenceLength, long vectorLength, DataType dataType) {
    checkInputName(inputName);
    return clone(
        c -> addVectorSequenceConverter(((AbstractCustomNeuralNetwork<?, ?>) c)._inputConverters, inputName,
            producer, sequenceLength, vectorLength, dataType));
  }

  /**
   * Returns a copy of this instance which will accept input values from the specified producer.
   *
   * @param inputName the name of the input whose value is being provided
   * @param producer the producer providing the values
   * @param shape the shape of the {@link MDArray}s (does not have to match the arrays' shape, but the array
   *              must have at least as many values as implied by the shape)
   * @param dataType the data type of the {@link INDArray} that will represent the provided values and be fed into
   *                 neural network
   * @return a copy of this instance that will accept values from the specified producer
   */
  protected S withFeaturesInputFromMDArray(String inputName, Producer<? extends MDArray> producer, long[] shape,
      DataType dataType) {
    checkInputName(inputName);
    return clone(
        c -> addMDArrayConverter(((AbstractCustomNeuralNetwork<?, ?>) c)._inputConverters, inputName, producer,
            shape, dataType));
  }

  /**
   * Returns a copy of this instance which will accept input values from the specified producer.
   *
   * @param inputName the name of the input whose value is being provided
   * @param producer the producer providing the values
   * @param sequenceLength the maximum length of the sequence (items in the {@link Iterable} beyond this limit will be
   *                       ignored)
   * @param shape the shape of the {@link MDArray}s (does not have to match the arrays' shape, but the array
   *              must have at least as many values as implied by the shape)
   * @param dataType the data type of the {@link INDArray} that will represent the provided values and be fed into
   *                 neural network
   * @return a copy of this instance that will accept values from the specified producer
   */
  protected S withFeaturesInputFromMDArraySequence(String inputName, Producer<? extends Iterable<? extends MDArray>> producer,
      long sequenceLength, long[] shape, DataType dataType) {
    checkInputName(inputName);
    return clone(
        c -> addMDArraySequenceConverter(((AbstractCustomNeuralNetwork<?, ?>) c)._inputConverters, inputName,
            producer, sequenceLength, shape, dataType));
  }

  /**
   * Returns a copy of this instance which will accept labels from the specified producer.
   *
   * @param outputName the name of the output layer whose label is being provided
   * @param producer the producer providing the values
   * @param dataType the data type of the {@link INDArray} that will represent the provided values and be fed into
   *                 neural network
   * @return a copy of this instance that will accept values from the specified producer
   */
  protected S withLabelInputFromNumber(String outputName, Producer<? extends Number> producer, DataType dataType) {
    checkLabelName(outputName);
    return clone(
        c -> addNumberConverter(((AbstractCustomNeuralNetwork<?, ?>) c)._labelConverters, outputName, producer,
            dataType));
  }

  /**
   * Returns a copy of this instance which will accept labels from the specified producer.
   *
   * @param outputName the name of the output layer whose label is being provided
   * @param producer the producer providing the values
   * @param sequenceLength the maximum length of the sequence; items in the {@link Iterable} beyond this limit will be
   *                       ignored
   * @param dataType the data type of the {@link INDArray} that will represent the provided values and be fed into
   *                 neural network
   * @return a copy of this instance that will accept values from the specified producer
   */
  protected S withLabelInputFromNumberSequence(String outputName,
      Producer<? extends Iterable<? extends Number>> producer, long sequenceLength, DataType dataType) {
    checkLabelName(outputName);
    return clone(
        c -> addNumberSequenceConverter(((AbstractCustomNeuralNetwork<?, ?>) c)._labelConverters, outputName,
            producer, sequenceLength, dataType));
  }

  /**
   * Returns a copy of this instance which will accept labels from the specified producer.
   *
   * @param outputName the name of the output layer whose label is being provided
   * @param producer the producer providing the values
   * @param vectorLength the maximum length the vector; elements with indices less than 0 or greater than or equal
   *                     to {@code vectorLength} will be ignored
   * @param dataType the data type of the {@link INDArray} that will represent the provided values and be fed into
   *                 neural network
   * @return a copy of this instance that will accept values from the specified producer
   */
  protected S withLabelInputFromVector(String outputName, Producer<? extends Vector> producer, long vectorLength,
      DataType dataType) {
    checkLabelName(outputName);
    return clone(
        c -> addVectorConverter(((AbstractCustomNeuralNetwork<?, ?>) c)._labelConverters, outputName, producer,
            vectorLength, dataType));
  }

  /**
   * Returns a copy of this instance which will accept labels from the specified producer.
   *
   * @param outputName the name of the output layer whose label is being provided
   * @param producer the producer providing the values
   * @param sequenceLength the maximum length of the sequence; items in the {@link Iterable} beyond this limit will be
   *                       ignored
   * @param vectorLength the maximum length the vector; elements with indices less than 0 or greater than or equal
   *                     to {@code vectorLength} will be ignored
   * @param dataType the data type of the {@link INDArray} that will represent the provided values and be fed into
   *                 neural network
   * @return a copy of this instance that will accept values from the specified producer
   */
  protected S withLabelInputFromVectorSequence(String outputName,
      Producer<? extends Iterable<? extends Vector>> producer, long sequenceLength, long vectorLength,
      DataType dataType) {
    checkLabelName(outputName);
    return clone(
        c -> addVectorSequenceConverter(((AbstractCustomNeuralNetwork<?, ?>) c)._labelConverters, outputName,
            producer, sequenceLength, vectorLength, dataType));
  }

  /**
   * Returns a copy of this instance which will accept labels from the specified producer.
   *
   * @param outputName the name of the output layer whose label is being provided
   * @param producer the producer providing the values
   * @param shape the shape of the {@link MDArray}s (does not have to match the arrays' shape, but the array
   *              must have at least as many values as implied by the shape)
   * @param dataType the data type of the {@link INDArray} that will represent the provided values and be fed into
   *                 neural network
   * @return a copy of this instance that will accept values from the specified producer
   */
  protected S withLabelInputFromMDArray(String outputName, Producer<? extends MDArray> producer, long[] shape,
      DataType dataType) {
    checkLabelName(outputName);
    return clone(
        c -> addMDArrayConverter(((AbstractCustomNeuralNetwork<?, ?>) c)._labelConverters, outputName, producer,
            shape, dataType));
  }

  /**
   * Returns a copy of this instance which will accept labels from the specified producer.
   *
   * @param outputName the name of the output layer whose label is being provided
   * @param producer the producer providing the values
   * @param sequenceLength the maximum length of the sequence (items in the {@link Iterable} beyond this limit will be
   *                       ignored)
   * @param shape the shape of the {@link MDArray}s (does not have to match the arrays' shape, but the array
   *              must have at least as many values as implied by the shape)
   * @param dataType the data type of the {@link INDArray} that will represent the provided values and be fed into
   *                 neural network
   * @return a copy of this instance that will accept values from the specified producer
   */
  protected S withLabelInputFromMDArraySequence(String outputName,
      Producer<? extends Iterable<? extends MDArray>> producer, long sequenceLength, long[] shape, DataType dataType) {
    checkLabelName(outputName);
    return clone(
        c -> addMDArraySequenceConverter(((AbstractCustomNeuralNetwork<?, ?>) c)._labelConverters, outputName,
            producer, sequenceLength, shape, dataType));
  }

  /**
   * Gets the computation graph (neural network) underlying this instance.
   *
   * @return the computation graph
   */
  protected ComputationGraph getComputationGraph() {
    return _computationGraph;
  }

  /**
   * Sets the computation graph and cleans the input/label converters and output list to remove now-invalid entries.
   * As this modifies this instance, it should only be used to configure a new clone() before it is returned.
   *
   * @param computationGraph the new computation graph
   */
  private void setComputationGraph(ComputationGraph computationGraph) {
    _computationGraph = computationGraph;
    HashSet<String> inputSet = new HashSet<>(computationGraph.getConfiguration().getNetworkInputs());
    HashSet<String> outputSet = new HashSet<>(computationGraph.getConfiguration().getNetworkOutputs());

    _inputConverters = Maps.filterByKey(_inputConverters, inputSet::contains, LinkedHashMap::new);
    _labelConverters = Maps.filterByKey(_labelConverters, outputSet::contains, LinkedHashMap::new);

    if (_outputNames != null) {
      _outputNames.removeIf(output -> computationGraph.getVertex(output) == null);
    }
  }

  /**
   * Creates a copy of this instance with the specified computation graph; this will clear any feature or label inputs
   * that have no corresponding inputs/output layers in the new computation graph.
   *
   * @param computationGraph the computation graph to be used
   * @return a copy of this instance that will use the specified computation graph
   */
  protected S withComputationGraph(ComputationGraph computationGraph) {
    return clone(c -> ((AbstractCustomNeuralNetwork<?, ?>) c).setComputationGraph(computationGraph));
  }

  /**
   * @return the maximum number of passes over the data during training
   */
  protected long getMaxEpochs() {
    return _maxEpochs;
  }

  /**
   * Returns a copy of this instance that will train up to the specified number of epochs (passes over the training
   * data).  The actual number of epochs may be smaller if another stopping condition is met beforehand.
   *
   * The default maximum number of epochs is 16.
   *
   * @param maxEpochs the maximum number of passes over the data during training
   * @return a copy of this instance that will train up to the specified maximum number of epochs
   */
  protected S withMaxEpochs(long maxEpochs) {
    return clone(c -> ((AbstractCustomNeuralNetwork<?, ?>) c)._maxEpochs = maxEpochs);
  }

  /**
   * Normally, a DL4J computation graph (which could potentially be shared across multiple transformers) can be used
   * by only one thread at a time, since {@link ComputationGraph} is stateful.  If concurrent inference is enabled,
   * it will instead create thread-local copies of the computation graph as needed.  This can also result in potentially
   * numerous copies of a high-memory model consuming high amounts of RAM.
   *
   * @return whether or not concurrent inference is enabled for inference with this neural network
   */
  protected boolean usingConcurrentInference() {
    return _concurrentInference;
  }

  /**
   * Normally, a DL4J computation graph (which could potentially be shared across multiple transformers) can be used
   * by only one thread at a time, since {@link ComputationGraph} is stateful.  Consequently, by default, thread-local
   * copies of the computation graph are created when needed to allow for concurrent inference.
   *
   * Disabling concurrent inference will prohibit creation of these additional copies such that the neural network
   * cannot be used by multiple threads simultaneously, possibly slowing inference when many examples are used.
   * However, this also avoids potentially numerous copies of a high-memory model consuming high amounts of RAM.
   *
   * @return a copy of this instance with concurrent execution disabled
   */
  protected S withNoConcurrentInference() {
    return clone(c -> ((AbstractCustomNeuralNetwork<?, ?>) c)._concurrentInference = false);
  }

  /**
   * Returns a copy of this instance that will use the specified minibatch size during training.  The minibatch size can
   * affect both training time and resulting model quality, and it serves as an important hyperparameter to the model.
   *
   * The default minibatch size is 64.
   *
   * @param minibatchSize the size of the minibatches to use for training
   * @return a copy of this instance that will use the specified minibatch size
   */
  protected S withMinibatchSize(int minibatchSize) {
    Arguments.check(minibatchSize >= 0);
    return clone(c -> ((AbstractCustomNeuralNetwork<?, ?>) c)._minibatchSize = minibatchSize);
  }

  /**
   * @return the number of examples in each training minibatch
   */
  protected int getMinibatchSize() {
    return _minibatchSize;
  }

  /**
   * Returns a copy of this instance that will use the specified minibatch size during inference (or possibly less,
   * depending on the number of examples the DAG executor provides.)  Larger minibatches may, to a point, result in
   * faster inference when executing the prepared DAG against a large number of examples.
   *
   * By default, this is same as the training minibatch size.
   *
   * @param minibatchSize the size of the minibatches to prefer during inference, or 0 to use the training minibatch
   *                      size
   * @return a copy of this instance that will use the specified minibatch size
   */
  protected S withMinibatchSizeForInference(int minibatchSize) {
    Arguments.check(minibatchSize >= 0);
    return clone(c -> ((AbstractCustomNeuralNetwork<?, ?>) c)._minibatchSizeForInference = minibatchSize);
  }

  /**
   * @return the number of examples in each inference minibatch
   */
  protected int getMinibatchSizeForInference() {
    return _minibatchSizeForInference == 0 ? getMinibatchSize() : _minibatchSizeForInference;
  }

  /**
   * Gets a producer that obtains the output of the specified layer as a vector.  If the layer's output is not already
   * vector-shaped, it will be flattened (as per {@link MDArray#asVector()}).
   *
   * @param outputName the name of the output whose value to get
   * @return a {@link Producer} that obtains the output of the specified layer as a vector
   */
  protected Producer<DenseVector> asLayerOutputVector(String outputName) {
    checkOutputName(outputName);

    return DL4JResult.InternalAPI.toVector(this, getEffectiveOutputs().indexOf(outputName));
  }

  /**
   * Gets a producer that obtains the output of the specified layer as an {@link MDArray}.
   *
   * @param outputName the name of the output whose value to get
   * @return a {@link Producer} that obtains the output of the specified layer as a multidimensional array
   */
  protected Producer<MDArray> asLayerOutputMDArray(String outputName) {
    checkOutputName(outputName);

    return DL4JResult.InternalAPI.toMDArray(this, getEffectiveOutputs().indexOf(outputName));
  }

  /**
   * Gets a producer that obtains the output of the specified layer as an {@link INDArray}.
   *
   * @param outputName the name of the output whose value to get
   * @return a {@link Producer} that obtains the output of the specified layer as an {@link INDArray}
   */
  protected Producer<INDArray> asLayerOutputINDArray(String outputName) {
    checkOutputName(outputName);

    return DL4JResult.InternalAPI.toINDArray(this, getEffectiveOutputs().indexOf(outputName));
  }

  /**
   * Gets a producer that will provide the parameters of the specified layer in computation graph as a {@link Map}
   * from parameter table names to their corresponding {@link MDArray}s.  The names, quantities and layouts of
   * parameters will vary depending on the underlying neural network implementation.
   *
   * For layers lacking parameters, the resulting {@link Map} will be empty.
   *
   * @param layer the layer in this neural network whose parameters should be obtained
   * @return a producer that will yield the parameters corresponding to the specified layer
   */
  protected Producer<Map<String, ? extends MDArray>> asLayerParameters(String layer) {
    Objects.requireNonNull(_computationGraph, "Computation graph has not yet been set");
    Arguments.check(_computationGraph.getVertex(layer) != null,
        () -> "The layer " + layer + " is not present in the computation graph");
    return new ParameterViewer<>(this, layer);
  }

  @Override
  public void validate() {
    super.validate();

    Objects.requireNonNull(_computationGraph, "Computation graph has not yet been set");
    List<String> inputs = _computationGraph.getConfiguration().getNetworkInputs();
    Arguments.check(_inputConverters.size() == inputs.size(),
        "Not all inputs to the neural network have been provided; required: " + inputs + ", specified: "
            + _inputConverters.keySet());
    List<String> outputs = _computationGraph.getConfiguration().getNetworkOutputs();
    Arguments.check(_labelConverters.size() == outputs.size(),
        "Not all labels to the neural network have been provided; required: " + outputs + ", specified: "
            + _labelConverters.keySet());
  }

  @SuppressWarnings("unchecked") // correctness ensured because converters always correspond to transformer's inputs
  private AbstractInputConverter<?, ?>[] prepareConverters(List<String> names, DynamicInputs dynamicInputs,
      LinkedHashMap<String, Tuple2<Producer<?>, AbstractInputConverter<?, ?>>> converterMap) {
    AbstractInputConverter<?, ?>[] result = new AbstractInputConverter<?, ?>[names.size()];
    for (int i = 0; i < names.size(); i++) {
      Tuple2<Producer<?>, AbstractInputConverter<?, ?>> entry = converterMap.get(names.get(i));
      result[i] = entry.get1().withInputAccessor((DynamicInputs.Accessor) dynamicInputs.get(entry.get0()));
    }

    return result;

  }

  /**
   * @return an array of input converters (with their {@link com.linkedin.dagli.transformer.DynamicInputs.Accessor}
   * accessors set) corresponding to the ordered list of input layers of the computation graph.
   */
  protected AbstractInputConverter<?, ?>[] getInputConverters() {
    return prepareConverters(_computationGraph.getConfiguration().getNetworkInputs(), new DynamicInputs(getInputList()),
        _inputConverters);
  }

  /**
   * @return an array of label converters (with their input accessors set) corresponding to the ordered list of input
   * layers of the computation graph.
   */
  protected AbstractInputConverter<?, ?>[] getLabelConverters() {
    return prepareConverters(_computationGraph.getConfiguration().getNetworkOutputs(), new DynamicInputs(getInputList()),
        _labelConverters);
  }

  /**
   * Basic preparer that trains a DL4J neural network.
   */
  static protected class Preparer<S extends AbstractCustomNeuralNetwork<Prepared, S>>
      extends AbstractPreparer<Prepared, S> {
    /**
     * Creates a new instance.
     *
     * @param neuralNetwork the neural network transformer being prepared
     */
    public Preparer(S neuralNetwork) {
      super(neuralNetwork);
    }

    @Override
    protected Prepared createPrepared(ComputationGraph graph) {
      return new Prepared(graph, getNeuralNetwork().getMinibatchSizeForInference(),
          getNeuralNetwork().getInputConverters(), getNeuralNetwork().getEffectiveOutputs());
    }
  }

  /**
   * Base class for DL4J neural network preparers.
   *
   * @param <N> the type of the prepared transformer
   * @param <S> the type of the neural network preparable transformer
   */
  static protected abstract class AbstractPreparer<N extends AbstractPrepared<N> & ParameterStore<String>,
                                                   S extends AbstractCustomNeuralNetwork<N, S>>
      extends AbstractBatchPreparerDynamic<DL4JResult, N> {

    private final S _neuralNetwork;

    /**
     * Creates a new instance.
     *
     * @param neuralNetwork the neural network transformer being prepared
     */
    public AbstractPreparer(S neuralNetwork) {
      _neuralNetwork = neuralNetwork;
    }

    /**
     * @return the preparable neural network transformer prepared by this instance
     */
    protected S getNeuralNetwork() {
      return _neuralNetwork;
    }

    /**
     * @return a copy of the neural network's computation graph, ready for training.  Overriding this method allows for
     *         varying parameter initialization.
     */
    protected ComputationGraph createComputationGraph() {
      ComputationGraph graph = _neuralNetwork.getComputationGraph().clone();

      // need to set listeners again to fix bug where cloned graph's Optimizer is missing its listener list
      // (and we need to use a cloned list to do so because of another bug :) )
      graph.setListeners(new ArrayList<>(graph.getListeners()));

      graph.init();
      return graph;
    }

    /**
     * Trains the neural network using the data provided by a {@link MultiDataSetIterator}.
     *
     * @param graph the computation graph to train
     * @param iterator the iterator providing the training data
     */
    protected void train(ComputationGraph graph, MultiDataSetIterator iterator) {
      graph.fit(iterator, Math.toIntExact(getNeuralNetwork().getMaxEpochs()));
    }

    /**
     * @param graph the trained computation graph
     * @return a prepared neural network transformer that will use the trained computation graph
     */
    protected abstract N createPrepared(ComputationGraph graph);

    @Override
    public void processUnsafe(Object[] values) {
      // no-op
    }

    @Override
    public PreparerResultMixed<? extends PreparedTransformer<DL4JResult>, N> finishUnsafe(
        ObjectReader<Object[]> inputs) {
      ComputationGraph graph = createComputationGraph();
      train(graph, new MinibatchingMultiDataSetIterator(inputs, getNeuralNetwork().getMinibatchSize(),
          getNeuralNetwork().getInputConverters(), getNeuralNetwork().getLabelConverters()));

      N prepared = createPrepared(graph);

      if (!getNeuralNetwork().usingConcurrentInference()) {
        prepared = prepared.withNoConcurrentInference();
      }

      return new PreparerResult<N>(prepared);
    }
  }

  /**
   * A basic prepared transformer for inference with a DL4J neural network.
   */
  @ValueEquality
  static protected class Prepared extends AbstractPrepared<Prepared> implements ParameterStore<String> {
    private static final long serialVersionUID = 1;

    protected Prepared(ComputationGraph computationGraph, int minibatchSizeForInference,
        AbstractInputConverter<?, ?>[] inputConverters, List<String> outputs) {
      super(computationGraph, minibatchSizeForInference, inputConverters, outputs);
    }

    @Override
    public Map<String, INDArrayAsMDArray> getParameters(String key) {
      return super.getParametersForLayerName(key);
    }
  }

  /**
   * Base class for prepared DL4J neural network transformers.
   *
   * @param <S> the derived type
   */
  static protected abstract class AbstractPrepared<S extends AbstractPrepared<S>>
      extends AbstractPreparedStatefulTransformerDynamic<DL4JResult, ThreadLocal<ComputationGraph>, S> {
    private static final long serialVersionUID = 1;
    private static final AbstractInputConverter<?, ?>[] EMPTY_LABEL_CONVERTER_ARRAY = new AbstractInputConverter[0];

    // The computation graph: if concurrent inference is not used, inference will use this graph instance directly,
    // synchronizing on the graph instance itself.  Note that ComputationGraph::equals() fortunately *only* compares the
    // configuration, parameters and hyperparameters of two graphs, and so it will work as desired even when/if its
    // input/mask arrays and other internal state change during execution of this transformer.
    private SerializableComputationGraph _computationGraph;
    private ReentrantLock _computationGraphLock = new ReentrantLock();
    private boolean _allowConcurrentInference = true;

    private final AbstractInputConverter<?, ?>[] _inputConverters;
    private final List<String> _outputLayerNames;
    private final int _preferredMinibatchSize;

    // override clone() so we can deep-copy the computation graph and its lock to reduce contention
    @Override
    protected S clone() {
      S result = super.clone();
      ((AbstractPrepared<?>) result)._computationGraph = new SerializableComputationGraph(_computationGraph.get().clone());
      ((AbstractPrepared<?>) result)._computationGraphLock = new ReentrantLock();
      return result;
    }

    /**
     * Creates a new instance.
     *
     * @param computationGraph the computational graph
     * @param preferredMinibatchSize the preferred inference minibatch size
     * @param inputConverters input converters used to convert inputs into NDArrays
     * @param outputs the names of the output layers
     */
    protected AbstractPrepared(ComputationGraph computationGraph, int preferredMinibatchSize,
        AbstractInputConverter<?, ?>[] inputConverters, List<String> outputs) {
      _computationGraph = new SerializableComputationGraph(computationGraph);
      _inputConverters = inputConverters;
      _outputLayerNames = outputs;
      _preferredMinibatchSize = preferredMinibatchSize;
    }

    @Override
    protected int getPreferredMinibatchSize() {
      return _preferredMinibatchSize;
    }

    /**
     * Returns a copy of this instance that will <strong>never</strong> create copies of the computation graph to allow
     * for parallel inference.  This may be useful for very large computation graphs where memory usage is a concern,
     * but can slow down inference over large numbers of examples.
     *
     * Normally, a DL4J computation graph can be used by only one thread at a time, since {@link ComputationGraph} is
     * stateful.  However, this transformer will, by default, create additional thread-local copies of the computation
     * graph as needed.
     *
     * @return a copy of this instance that will never create copies of the computation graph to perform concurrent
     *         inference
     */
    protected S withNoConcurrentInference() {
      return clone(c -> ((AbstractPrepared<?>) c)._allowConcurrentInference = false);
    }

    @Override
    protected ThreadLocal<ComputationGraph> createExecutionCache(long exampleCountGuess) {
      return _allowConcurrentInference ? new ThreadLocal<ComputationGraph>() {
        @Override
        protected ComputationGraph initialValue() {
          return AbstractPrepared.this._computationGraph.get().clone();
        }
      } : null;
    }

    /**
     * @return the computation graph of this neural network; this graph is "owned" by this instance, which
     *         may subsequently modify it (likewise, the caller should not themselves modify it)
     */
    protected ComputationGraph getComputationGraph() {
      return _computationGraph.get();
    }

    /**
     * @return the neural network's input converters
     */
    protected AbstractInputConverter<?, ?>[] getInputConverters() {
      return _inputConverters;
    }

    protected Map<String, INDArrayAsMDArray> getParametersForLayerName(String key) {
      return Maps.replaceValues(_computationGraph.get().getVertex(key).paramTable(false), INDArrayAsMDArray::new);
    }

    /**
     * Runs the computation graph against the provided minibatch and returns the results.
     *
     * @param threadLocals a {@link ThreadLocal} instance that can provide a thread-specific copy of the graph, or null
     *                     if copies should not be created/used
     * @param multiDataSet the minibatch data
     * @return the results of inference on the minibatch
     */
    protected INDArray[] apply(ThreadLocal<ComputationGraph> threadLocals, MultiDataSet multiDataSet) {
      final INDArray[] result;

      final boolean hasLock;
      if (threadLocals == null) {
        hasLock = true;
        _computationGraphLock.lock();
      } else {
        hasLock = _computationGraphLock.tryLock(); // try to use our original copy if it is available
      }

      if (hasLock) {
        try {
          return apply(_computationGraph.get(), multiDataSet);
        } finally {
          _computationGraphLock.unlock();
        }
      }

      // couldn't get lock, use a thread-local copy
      return apply(threadLocals.get(), multiDataSet);
    }

    private INDArray[] apply(ComputationGraph graph, MultiDataSet multiDataSet) {
      return graph.output(_outputLayerNames, false, multiDataSet.getFeatures(), multiDataSet.getFeaturesMaskArrays());
    }

    @Override
    protected DL4JResult apply(ThreadLocal<ComputationGraph> executionCache, List<?> values) {
      Minibatcher batcher = new Minibatcher(1, _inputConverters, EMPTY_LABEL_CONVERTER_ARRAY);
      batcher.addExample(values.toArray());
      INDArray[] outputs = apply(executionCache,
          new org.nd4j.linalg.dataset.MultiDataSet(batcher.getFeatureMinibatchINDArrays(), null,
              batcher.getFeatureMaskMinibatchINDArrays(), null));
      return new DL4JResult(outputs);
    }

    private void addAllResults(int minibatchSize, INDArray[] minibatchOutputs, List<? super DL4JResult> results) {
      if (minibatchSize == 1) {
        results.add(new DL4JResult(minibatchOutputs));
      } else {
        for (int i = 0; i < minibatchSize; i++) {
          INDArrayIndex exampleRow = NDArrayIndex.indices(i);
          results.add(new DL4JResult(
              ArraysEx.mapArray(minibatchOutputs, INDArray[]::new, original -> original.get(exampleRow))));
        }
      }
    }

    @Override
    protected void applyAll(ThreadLocal<ComputationGraph> executionCache, List<? extends List<?>> values,
        List<? super DL4JResult> results) {
      int exampleCount = values.get(0).size();
      Object[] exampleBuffer = new Object[values.size()];
      Minibatcher batcher = new Minibatcher(Math.min(exampleCount, _preferredMinibatchSize), _inputConverters,
          EMPTY_LABEL_CONVERTER_ARRAY);

      for (int exampleIndex = 0; exampleIndex < exampleCount; exampleIndex++) {
        Lists.copyColumnToArray(values, exampleIndex, exampleBuffer);
        batcher.addExample(exampleBuffer);

        if (batcher.isFull() || exampleIndex == exampleCount - 1) {
          INDArray[] outputs = apply(executionCache,
              new org.nd4j.linalg.dataset.MultiDataSet(batcher.getFeatureMinibatchINDArrays(), null,
                  batcher.getFeatureMaskMinibatchINDArrays(), null));
          addAllResults(batcher.exampleCount(), outputs, results);
          batcher.clear();
        }
      }
    }
  }
}
