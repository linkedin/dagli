package com.linkedin.dagli.nn;

import com.linkedin.dagli.distribution.SampledWithReplacement;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.math.distribution.BinaryDistribution;
import com.linkedin.dagli.math.hashing.DoubleXorShift;
import com.linkedin.dagli.math.mdarray.MDArray;
import com.linkedin.dagli.nn.interactive.commands.InteractiveCommand;
import com.linkedin.dagli.nn.interactive.commands.Stop;
import com.linkedin.dagli.nn.layer.DynamicLayerConfig;
import com.linkedin.dagli.nn.layer.LayerHandle;
import com.linkedin.dagli.nn.layer.LossLayer;
import com.linkedin.dagli.nn.layer.NNChildLayer;
import com.linkedin.dagli.nn.layer.NNLayer;
import com.linkedin.dagli.nn.layer.NNRootLayer;
import com.linkedin.dagli.nn.optimizer.Optimizer;
import com.linkedin.dagli.nn.optimizer.StochasticGradientDescent;
import com.linkedin.dagli.nn.result.NNResult;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.preparer.AbstractPreparerDynamic;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.preparer.PreparerResultMixed;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparableTransformerDynamic;
import com.linkedin.dagli.transformer.DynamicInputs;
import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.transformer.PreparedTransformerDynamic;
import com.linkedin.dagli.util.invariant.Arguments;
import com.linkedin.dagli.util.stdio.StandardInputListener;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Base class for all neural network transformer implementations.
 *
 * Neural networks produce a {@link NNResult} that is not intended for direct use by client code; instead, the neural
 * network's "as____()" methods should be used to obtain the results for specific layers in a robust, convenient and
 * client-friendly way.
 *
 * Many properties of the neural network are expressed as {@link TrainingAmount}s; however, derived classes may not be
 * able to honor the exact amount specified and will instead provide the closest approximation they can.
 *
 * The lifecycle of a neural network can be seen as:
 * (1) The "configuration" phase when the neural network is configured
 * (2) Preparer initialization, which occurs immediately before the neural network starts receiving examples for
 *     training (at this time, all constant input values are known and the neural network can be constructed in
 *     the underlying implementation's "native format")
 * (3) Training, when examples are seen and the network's parameters learned
 * (4) The prepared (trained) neural network is then used for inference
 *
 * Derived classes should note that the list of {@link #_inputs} stored by this transformer may diverge from those
 * specified on the network's constituent layers because of the {@link #withInputsUnsafe(List)} method.
 *
 * @param <R> the type of {@link NNResult} produced by this neural network
 * @param <N> the type of the prepared transformer yielded by preparing (training) the neural network
 * @param <S> the most-derived class descending from this one
 */
public abstract class AbstractNeuralNetwork<
    R extends NNResult,
    N extends AbstractNeuralNetwork.Prepared<R, N>,
    S extends AbstractNeuralNetwork<R, N, S>> extends AbstractPreparableTransformerDynamic<R, N, S> {

  private static final long serialVersionUID = 1;
  private static final int IS_EVALUATION_EXAMPLE_INPUT_INDEX = 0; // always the first input in the input list

  // evaluation data is optionally used for performance logging and early stopping
  private Producer<Boolean> _isEvaluationExampleInput = new Constant<>(false);

  // Layers whose activations can be retrieved during inference.  Output layers must be within the network defined
  // by the loss layers; by default, this value is null, in which case the output layers are taken as the loss layers.
  // All output layers must be distinct (no duplicates).
  private List<NNChildLayer<?, ?>> _outputLayers = null;

  // Loss layers are the leaves of the NN graph, akin to a general DAG's "targets"; a valid NN graph must have at least
  // one loss layer, and all loss layers must be distinct (no duplicates).
  private List<NNChildLayer<?, ? extends LossLayer>> _lossLayers = Collections.emptyList();

  // The following fields must be kept in sync with the list loss layers:
  // the parent-to-child map:
  private Map<NNLayer<?, ?>, Set<NNChildLayer<?, ?>>> _parentToChildrenMap = Collections.emptyMap();
  // the list of roots
  private List<NNRootLayer<?, ?>> _rootLayers = Collections.emptyList();
  // a topographically-sorted list of layers
  private List<NNLayer<?, ?>> _topographicallySortedLayers = Collections.emptyList();
  // a map from layers to unique names
  private Map<NNLayer<?, ?>, String> _layerNames = Collections.emptyMap();
  // a list of the "original" Producer inputs as specified by the layers
  private List<Producer<?>> _originalInputs = null;
  // also: the "current" list of Producer inputs from the external DAG (stored our base class's _inputs field)
  // (the "original" and "current" list of inputs may diverge due to use of withInputsUnsafe(...))

  private double _dropoutProbability = 0;

  private long _randomSeed = 1;
  private long _maxEpochs = 16;
  private long _maxTrainingTimeInSeconds = Long.MAX_VALUE;
  private int _minibatchSize = 64;
  private int _minibatchSizeForInference = 0; // 0 -> same as _minibatchSize

  private Optimizer _optimizer = new StochasticGradientDescent();

  private FloatingPointPrecision _floatingPointPrecision = FloatingPointPrecision.SINGLE;

  private boolean _trainingModelArchitectureLogging = false;
  private TrainingAmount _trainingProgressLoggingFrequency = TrainingAmount.INFINITY;
  private TrainingAmount _trainingPerformanceLoggingFrequency = TrainingAmount.INFINITY;
  private TrainingAmount _trainingParametersLoggingFrequency = TrainingAmount.INFINITY;
  private TrainingAmount _trainingSampledActivationsLoggingFrequency = TrainingAmount.INFINITY;

  private TrainingAmount _evaluationFrequency = TrainingAmount.epochs(1);
  private TrainingAmount _maxTrainingAmountWithoutImprovement = TrainingAmount.INFINITY;

  private boolean _interactiveCommands = false;

  protected AbstractNeuralNetwork() {
    super();
    _inputs = Arrays.asList(_isEvaluationExampleInput);
    _originalInputs = _inputs;
  }

  /**
   * Returns a copy of this neural network that will enable or disable interactive commands, which allows commands
   * like "stop" to be entered on standard input while the neural network is running.  Any text output resulting from
   * these commands is printed to standard output.  The available commands will be printed (to stdout) when training
   * begins.
   *
   * If multiple neural networks are training in parallel, any command will affect all of them.
   *
   * Interactive commands are disabled by default, and should remain disabled in any production/automated training
   * setting.  The intent is for them to serve as an aid during rapid, "hands on" model experimentation.
   *
   * @param enabled whether or not interactive commands should be enabled
   * @return a copy of this neural network that will enable or disable interactive commands
   */
  public S withInteractiveCommands(boolean enabled) {
    return clone(c -> ((AbstractNeuralNetwork<?, ?, ?>) c)._interactiveCommands = enabled);
  }

  /**
   * Returns a copy of this neural network that will use the specified global dropout rate.
   *
   * Each time an example is processed by the network, each element (number) in the input values passed to each layer
   * supporting dropout has an (independent) probability of being "dropped" (set to 0), which can help mitigate
   * overfitting.
   *
   * By default (if a layer-specific dropout rate has not been set by calling {@code withDropoutProbability(...)} on
   * that layer), this global dropout rate is used.
   *
   * Note that many types of layers do not support dropout and will not be affected by this global dropout rate.
   *
   * Values may range from 0 (no dropout) to 1 (drop everything, which is definitely not a good idea).  The default
   * value is 0.
   *
   * @param probability a [0, 1] dropout probability
   * @return a copy of this instance that will use the specified dropout rate.
   */
  public S withDropoutProbability(double probability) {
    Arguments.check(probability >= 0 && probability <= 1, "Invalid probability");
    return clone(c -> ((AbstractNeuralNetwork<?, ?, ?>) c)._dropoutProbability = probability);
  }

  /**
   * @return the [0, 1] dropout probability for this layer, or NaN if this layer should use the neural network's global
   *         dropout rate
   */
  public double getDropoutProbability() {
    return _dropoutProbability;
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
  public S withMinibatchSizeForInference(int minibatchSize) {
    Arguments.check(minibatchSize >= 0);
    return clone(c -> ((AbstractNeuralNetwork<?, ?, ?>) c)._minibatchSizeForInference = minibatchSize);
  }

  /**
   * @return the number of examples used in each inference minibatch
   */
  protected int getMinibatchSizeForInference() {
    return _minibatchSizeForInference == 0 ? getMinibatchSize() : _minibatchSizeForInference;
  }

  protected TrainingAmount getEvaluationFrequency() {
    return _evaluationFrequency;
  }

  /**
   * Returns a copy of this neural network that will evaluate its loss at the given frequency, in epochs.
   *
   * Loss is computed either against held-out evaluation examples, if available, or otherwise the training data itself.
   * The calculated loss is used to keep track of the best available model trained thus far as well as early stopping
   * if the model consistently fails to improve (if enabled via
   * {@link #withMaxTrainingAmountWithoutImprovement(TrainingAmount)}).
   *
   * By default, the frequency is every epoch.
   *
   * @param frequencyInEpochs the frequency to use
   * @return a copy of this neural network that will evaluate its loss at the given frequency
   */
  public S withEvaluationFrequency(double frequencyInEpochs) {
    return withEvaluationFrequency(TrainingAmount.epochs(frequencyInEpochs));
  }

  /**
   * Returns a copy of this neural network that will evaluate its loss at the given frequency.
   *
   * Loss is computed either against held-out evaluation examples, if available, or otherwise the training data itself.
   * The calculated loss is used to keep track of the best available model trained thus far as well as early stopping
   * if the model consistently fails to improve (if enabled via
   * {@link #withMaxTrainingAmountWithoutImprovement(TrainingAmount)}).
   *
   * By default, the frequency is every epoch.
   *
   * @param frequency the frequency to use, or {@link TrainingAmount#INFINITY} to disable evaluation during training
   * @return a copy of this neural network that will evaluate its loss at the given frequency
   */
  public S withEvaluationFrequency(TrainingAmount frequency) {
    return clone(c -> ((AbstractNeuralNetwork<?, ?, ?>) c)._evaluationFrequency = Objects.requireNonNull(frequency));
  }

  protected TrainingAmount getMaxTrainingAmountWithoutImprovement() {
    return _maxTrainingAmountWithoutImprovement;
  }

  /**
   * Returns a copy of this neural network that will terminate training if the given amount of training occurs without
   * improvement to the loss (either on the held-out evaluation examples or, if there are
   * no held-out evaluation examples, on the training data).  The loss is recalculated with a frequency determined by
   * {@link #withEvaluationFrequency(TrainingAmount)} (every epoch, by default.)
   *
   * By default, the maximum is infinite ({@link TrainingAmount#INFINITY}), meaning there is no limit to the number of
   * epochs that may pass without improvement.
   *
   * @param epochs the maximum number of epochs that may occur without measured improvement to the model's loss
   * @return a copy of this neural network that will terminate training if the given amount of training occurs without
   *         improvement to the loss
   */
  public S withMaxTrainingAmountWithoutImprovement(double epochs) {
    return withMaxTrainingAmountWithoutImprovement(TrainingAmount.epochs(epochs));
  }

  /**
   * Returns a copy of this neural network that will terminate training if the given amount of training occurs without
   * improvement to the loss (either on the held-out evaluation examples or, if there are
   * no held-out evaluation examples, on the training data).  The loss is recalculated with a frequency determined by
   * {@link #withEvaluationFrequency(TrainingAmount)} (every epoch, by default.)
   *
   * By default, the maximum is infinite ({@link TrainingAmount#INFINITY}), meaning there is no limit to the number of
   * epochs that may pass without improvement.
   *
   * @param trainingAmount the maximum amount of training that may occur without measured improvement to the model's
   *                       loss
   * @return a copy of this neural network that will terminate training if the given amount of training occurs without
   *         improvement to the loss
   */
  public S withMaxTrainingAmountWithoutImprovement(TrainingAmount trainingAmount) {
    return clone(
        c -> ((AbstractNeuralNetwork<?, ?, ?>) c)._maxTrainingAmountWithoutImprovement = trainingAmount);
  }

  private void setIsEvaluationExampleInput(Producer<Boolean> isEvaluationExampleInput) {
    _isEvaluationExampleInput = isEvaluationExampleInput;
    _inputs.set(IS_EVALUATION_EXAMPLE_INPUT_INDEX, isEvaluationExampleInput);
    _originalInputs.set(IS_EVALUATION_EXAMPLE_INPUT_INDEX, isEvaluationExampleInput);
  }

  /**
   * Returns a copy of this neural network that will determine whether to hold out an example for evaluation based on
   * the provided input (each example where {@code isEvaluationExampleInput} provides a value of {@code true} will be
   * held out.)
   *
   * Held out data is used for evaluating the model as training progresses for the purposes of logging the model's
   * performance and/or early stopping (i.e. where model training ends prematurely when performance on the evaluation
   * data starts to decline.)
   *
   * Holding out a greater proportion of data for evaluation improves the resulting estimate of model performance, but
   * also decreases the amount of data used to directly optimize the network's loss.
   *
   * @param isEvaluationExampleInput the input providing a value of "true" for held out examples and "false" for
   *                                 examples that will be used for training as normal
   * @return a copy of this neural network that will determine whether to hold out an example for evaluation based on
   *         the provided input
   */
  public S withEvaluationHoldoutInput(Producer<Boolean> isEvaluationExampleInput) {
    return clone(c -> ((AbstractNeuralNetwork<?, ?, ?>) c).setIsEvaluationExampleInput(isEvaluationExampleInput));
  }

  /**
   * Returns a copy of this neural network that will hold out the specified proportion of training data to use for
   * evaluating the model as training progresses for the purposes of logging the model's performance and/or early
   * stopping (i.e. where model training ends prematurely when performance on the evaluation data starts to decline.)
   *
   * Holding out a greater proportion of data for evaluation improves the resulting estimate of model performance, but
   * also decreases the amount of data used to directly optimize the network's loss.
   *
   * The decision to hold out each example is made independently at random with probability
   * {@code approximateProportion}; consequently, the actual proportion of data held out will be
   * <strong>approximately</strong> the proportion specified, but may not (and probably will not) be that exact amount
   * (more concretely, the number of examples held out will follow the binomial distribution).
   *
   * @param approximateProportion the approximate [0, 1) proportion of data that will be held out for evaluation
   * @param seed the seed for the random number generator
   * @return a copy of this neural network that will (approximately) hold out the specified proportion of training data
   *         for interleaved evaluation during training
   */
  public S withEvaluationHoldoutProportion(double approximateProportion, long seed) {
    return withEvaluationHoldoutInput(
        new SampledWithReplacement<>(new BinaryDistribution(approximateProportion)).withRandomNumberGenerator(
            new DoubleXorShift().withSeed(seed)));
  }

  protected long getMaxTrainingTimeInSeconds() {
    return _maxTrainingTimeInSeconds;
  }

  /**
   * Returns a copy of this neural network where the total training time will be approximately limited to the specified
   * duration.
   *
   * Training time is measured from the time when the neural network begins training (typically only after all examples
   * are available), <strong>not</strong> when the encapsulating DAG's execution begins.  Additionally, training may
   * not end immediately after the requested time limit is met, and may instead only end at the completion of the
   * current epoch (this will vary by implementation).
   *
   * @param maxTrainingTimeInSeconds the maximum training time (in seconds) to allow before training is stopped
   * @return a copy of this neural network where the total training time will be approximately limited to the specified
   *         duration
   */
  public S withMaxTrainingTimeInSeconds(long maxTrainingTimeInSeconds) {
    return clone(c -> ((AbstractNeuralNetwork<?, ?, ?>) c)._maxTrainingTimeInSeconds = maxTrainingTimeInSeconds);
  }

  protected boolean getTrainingModelArchitectureLogging() {
    return _trainingModelArchitectureLogging;
  }

  protected TrainingAmount getTrainingProgressLoggingFrequency() {
    return _trainingProgressLoggingFrequency;
  }

  protected TrainingAmount getTrainingPerformanceLoggingFrequency() {
    return _trainingPerformanceLoggingFrequency;
  }

  protected TrainingAmount getTrainingParametersLoggingFrequency() {
    return _trainingParametersLoggingFrequency;
  }

  protected TrainingAmount getTrainingSampledActivationsLoggingFrequency() {
    return _trainingSampledActivationsLoggingFrequency;
  }

  /**
   * Returns a copy of this instance that will log (or not) the model architecture, as described by the backing
   * neural network implementation, immediate before training begins.  This will generally include a list of layers, the
   * number of parameters, the connections between the layers, etc.
   *
   * By default, the model architecture is not logged.
   *
   * @param log true iff the model architecture should be logged
   * @return a copy of this instance that will log the model architecture (or not)
   */
  public S withTrainingModelArchitectureLogging(boolean log) {
    return clone(c -> ((AbstractNeuralNetwork<?, ?, ?>) c)._trainingModelArchitectureLogging = log);
  }

  /**
   * Returns a copy of this instance that will log information about the training progress (such as the current training
   * loss, percentage complete, estimated time remaining, etc.) at the given frequency.  The exact set of information
   * logged (and its format) will vary by implementation.
   *
   * By default, this logging is disabled (frequency == {@link TrainingAmount#INFINITY}).
   *
   * @param frequency how much training should transpire between subsequent logging events
   * @return a copy of this instance that will log at the specified frequency
   */
  public S withTrainingProgressLoggingFrequency(TrainingAmount frequency) {
    return clone(c -> ((AbstractNeuralNetwork<?, ?, ?>) c)._trainingProgressLoggingFrequency = frequency);
  }

  /**
   * Returns a copy of this instance that will log information about training computational performance (such as the
   * clock time per minibatch) at the given frequency.  The exact set of information logged (and its format) will vary
   * by implementation.
   *
   * By default, this logging is disabled (frequency == {@link TrainingAmount#INFINITY}).
   *
   * @param frequency how much training should transpire between subsequent logging events
   * @return a copy of this instance that will log at the specified frequency
   */
  public S withTrainingPerformanceLoggingFrequency(TrainingAmount frequency) {
    return clone(c -> ((AbstractNeuralNetwork<?, ?, ?>) c)._trainingPerformanceLoggingFrequency = frequency);
  }

  /**
   * Returns a copy of this instance that will log information about the current model parameters during training
   * at the given frequency.  The exact set of information logged (and its format) will vary by implementation.
   *
   * Please note that the resultant logging will typically be <strong>very</strong> verbose.
   *
   * By default, this logging is disabled (frequency == {@link TrainingAmount#INFINITY}).
   *
   * @param frequency how much training should transpire between subsequent logging events
   * @return a copy of this instance that will log at the specified frequency
   */
  public S withTrainingParametersLoggingFrequency(TrainingAmount frequency) {
    return clone(c -> ((AbstractNeuralNetwork<?, ?, ?>) c)._trainingParametersLoggingFrequency = frequency);
  }

  /**
   * Returns a copy of this instance that will log the network layers' activations for a small number of arbitrarily
   * sampled training examples at the given frequency.  The exact set of information logged (and its format) will vary
   * by implementation.
   *
   * Please note that, especially for large networks, the logging may be verbose.
   *
   * By default, this logging is disabled (frequency == {@link TrainingAmount#INFINITY}).
   *
   * @param frequency how much training should transpire between subsequent logging events
   * @return a copy of this instance that will log at the specified frequency
   */
  public S withTrainingSampledActivationsLoggingFrequency(TrainingAmount frequency) {
    return clone(c -> ((AbstractNeuralNetwork<?, ?, ?>) c)._trainingSampledActivationsLoggingFrequency = frequency);
  }

  /**
   * Returns a copy of this instance that will log information about the training progress (such as the current training
   * loss, percentage complete, estimated time remaining, etc.) at the given frequency.  The exact set of information
   * logged (and its format) will vary by implementation.
   *
   * By default, this logging is disabled (frequency == {@link TrainingAmount#INFINITY}).
   *
   * @param frequencyInEpochs how much training should transpire between subsequent logging events
   * @return a copy of this instance that will log at the specified frequency
   */
  public S withTrainingProgressLoggingFrequency(double frequencyInEpochs) {
    return withTrainingProgressLoggingFrequency(TrainingAmount.epochs(frequencyInEpochs));
  }

  /**
   * Returns a copy of this instance that will log information about training computational performance (such as the
   * clock time per minibatch) at the given frequency.  The exact set of information logged (and its format) will vary
   * by implementation.
   *
   * By default, this logging is disabled (frequency == {@link TrainingAmount#INFINITY}).
   *
   * @param frequencyInEpochs how much training should transpire between subsequent logging events
   * @return a copy of this instance that will log at the specified frequency
   */
  public S withTrainingPerformanceLoggingFrequency(double frequencyInEpochs) {
    return withTrainingPerformanceLoggingFrequency(TrainingAmount.epochs(frequencyInEpochs));
  }

  /**
   * Returns a copy of this instance that will log information about the current model parameters during training
   * at the given frequency.  The exact set of information logged (and its format) will vary by implementation.
   *
   * Please note that the resultant logging will typically be <strong>very</strong> verbose.
   *
   * By default, this logging is disabled (frequency == {@link TrainingAmount#INFINITY}).
   *
   * @param frequencyInEpochs how much training should transpire between subsequent logging events
   * @return a copy of this instance that will log at the specified frequency
   */
  public S withTrainingParametersLoggingFrequency(double frequencyInEpochs) {
    return withTrainingParametersLoggingFrequency(TrainingAmount.epochs(frequencyInEpochs));
  }

  /**
   * Returns a copy of this instance that will log the network layers' activations for a small number of arbitrarily
   * sampled training examples at the given frequency.  The exact set of information logged (and its format) will vary
   * by implementation.
   *
   * Please note that, especially for large networks, the logging may be verbose.
   *
   * By default, this logging is disabled (frequency == {@link TrainingAmount#INFINITY}).
   *
   * @param frequencyInEpochs how much training should transpire between subsequent logging events
   * @return a copy of this instance that will log at the specified frequency
   */
  public S withTrainingSampledActivationsLoggingFrequency(double frequencyInEpochs) {
    return withTrainingSampledActivationsLoggingFrequency(TrainingAmount.epochs(frequencyInEpochs));
  }

  /**
   * Returns a map of layers to distinct names for each provided layer.  The assigned layer name will always end in
   * a numerical suffix; this is a useful invariant if an implementation needs to create additional layers (conflicting
   * names can be avoided by simply not ending the new name with a numerical suffix).
   *
   * @param layers a list of layers for which unique names should be found
   * @return a map from each layer to a unique name for that layer
   */
  static Map<NNLayer<?, ?>, String> uniqueLayerNames(Collection<NNLayer<?, ?>> layers) {
    HashMap<NNLayer<?, ?>, String> result = new HashMap<>(layers.size());
    HashSet<String> usedNames = new HashSet<>(layers.size());

    for (NNLayer<?, ?> layer : layers) {
      if (result.containsKey(layer)) {
        continue; // ignore duplicate layers
      }
      String baseName = layer.getName() == null ? layer.getClass().getSimpleName() : layer.getName();
      String name = baseName + "-0";
      for (int i = 1; !usedNames.add(name); i++) {
        name = baseName + "-" + i;
      }
      result.put(layer, name);
    }

    return result;
  }

  protected FloatingPointPrecision getFloatingPointPrecision() {
    return _floatingPointPrecision;
  }

  /**
   * Returns a copy of this instance that will use the specified floating point precision for the inputs and parameters
   * of this network.  This can have a dramatic impact on the performance of the network, especially when using a GPU
   * (which have traditionally favored single-precision arithmetic).
   *
   * The default precision is {@link FloatingPointPrecision#SINGLE} (32-bit float values) which is strongly recommended
   * unless you have a compelling reason to change it.
   *
   * @param floatingPointPrecision the floating point precision to be used by the neural network
   * @return a copy of this instance that will use the specified precision
   */
  public S withFloatingPointPrecision(FloatingPointPrecision floatingPointPrecision) {
    return clone(c -> ((AbstractNeuralNetwork<?, ?, ?>) c)._floatingPointPrecision = floatingPointPrecision);
  }

  /**
   * @return the random seed to be used by this neural network during training.
   */
  protected long getRandomSeed() {
    return _randomSeed;
  }

  /**
   * Returns a copy of this instance that will use the specified random seed during training (e.g. to set the initial
   * parameter values).
   *
   * Training a specific neural network with a specific seed should in theory always yield the same parameter values,
   * although this may not <em>always</em> hold true in practice in a particular neural network implementation for
   * reasons such as:
   * (1) subtle differences between CPU and GPU computation, or computation on different architectures
   * (2) dependence on global statics for generating random values, potentially creating non-determinism when training
   *     multiple neural networks in parallel
   *
   * The default seed is 1.
   *
   * @param randomSeed the random seed to use
   * @return a copy of this neural network that will use the specified random seed
   */
  public S withRandomSeed(long randomSeed) {
    return clone(c -> ((AbstractNeuralNetwork<?, ?, ?>) c)._randomSeed = randomSeed);
  }

  /**
   * @return the maximum number of passes over the data during training
   */
  protected long getMaxEpochs() {
    return _maxEpochs;
  }

  /**
   * @return the number of examples in each training minibatch
   */
  protected int getMinibatchSize() {
    return _minibatchSize;
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
  public S withMaxEpochs(long maxEpochs) {
    return clone(c -> ((AbstractNeuralNetwork<?, ?, ?>) c)._maxEpochs = maxEpochs);
  }

  /**
   * Returns a copy of this instance that will use the specified minibatch size.  The minibatch size can affect
   * both training time and resulting model quality, and it serves as an important hyperparameter to the model.
   *
   * The default minibatch size is 64.
   *
   * @param minibatchSize the size of the minibatches to use for training
   * @return a copy of this instance that will use the specified minibatch size
   */
  public S withMinibatchSize(int minibatchSize) {
    return clone(c -> ((AbstractNeuralNetwork<?, ?, ?>) c)._minibatchSize = minibatchSize);
  }

  /**
   * Gets the output layers for this neural network.  By default, these are the loss layers.
   *
   * @return the output layers
   */
  @SuppressWarnings("unchecked") // cast is safe because LossLayer extends NNOutputLayer
  protected List<NNChildLayer<?, ?>> getOutputLayers() {
    return _outputLayers == null ? (List) _lossLayers : _outputLayers;
  }

  /**
   * Gets a producer that will provide the output corresponding to the specified layer.  The layer must be an
   * <strong>output layer</strong>: by default, (only) the loss layers are output layers, but a different set of outputs
   * may be specified via {@link #withOutputLayers(NNChildLayer[])} .  Attempting to get the output of a layer not
   * designated as an output layer will throw an {@link IllegalArgumentException}.
   *
   * @param outputLayer an output layer in this neural network whose activations or label (for loss layers) is to be
   *                    obtained
   * @param <T> the type of the output of the layer
   * @return a producer that will yield the output corresponding to the specified layer
   */
  public <T> Producer<T> asLayerOutput(NNChildLayer<T, ?> outputLayer) {
    int index = getOutputLayers().indexOf(outputLayer);
    if (index < 0) {
      throw new IllegalArgumentException("The layer " + outputLayer + " is not an output layer in this neural "
          + "network.  By default, the output layers are the same as the loss layers; to specify different output "
          + "layers, use withOutputLayers(...)?");
    }

    // get the index'th output
    return outputLayer.internalAPI().outputFromNNResult(this, index);
  }

  /**
   * Gets a producer that will provide the parameters of the specified layer in this neural network as a {@link Map}
   * from parameter set names to their corresponding {@link MDArray}s.  The names, quantities and layouts of parameters
   * will vary depending on the underlying neural network implementation; see {@link Prepared#getParameters(NNLayer)}
   * for more information.
   *
   * For layers lacking parameters, the resulting {@link Map} will be empty.
   *
   * @param layer the layer in this neural network whose parameters should be obtained
   * @return a producer that will yield the parameters corresponding to the specified layer
   */
  public Producer<Map<String, ? extends MDArray>> asLayerParameters(NNLayer<?, ?> layer) {
    Arguments.check(_parentToChildrenMap.containsKey(layer),
        () -> "The layer " + layer.toString() + " is not present in the neural network");
    return new ParameterViewer<>(this, layer.getHandle());
  }

  /**
   * @return the loss layers for this neural network.
   */
  protected List<NNChildLayer<?, ? extends LossLayer>> getLossLayers() {
    return _lossLayers;
  }

  /**
   * @return the parent-to-children layer map for this neural network
   */
  protected Map<NNLayer<?, ?>, Set<NNChildLayer<?, ?>>> getParentToChildrenMap() {
    return _parentToChildrenMap;
  }

  /**
   * @return the root layers for this neural network
   */
  protected List<NNRootLayer<?, ?>> getRootLayers() {
    return _rootLayers;
  }

  /**
   * @return a topographically-sorted list of all layers in the neural network (starting with a root)
   */
  public List<NNLayer<?, ?>> getTopographicallySortedLayers() {
    return _topographicallySortedLayers;
  }

  protected Map<NNLayer<?, ?>, String> getLayerNames() {
    return _layerNames;
  }

  /**
   * Sets the loss layers for this instance and updates other fields to reflect the new implied NN graph.
   *
   * @param lossLayers a collection of loss layers; these must be distinct (with no duplicates)
   */
  private void setLossLayers(Collection<NNChildLayer<?, ? extends LossLayer>> lossLayers) {
    Arguments.check(_outputLayers == null,
        "New loss layers cannot be set because output layers have been set by a previous call to "
            + "withOutputLayer(s).  To set new loss layers, the explicit output layers must first be cleared by "
            + "calling 'withOutputLayers()' [with no arguments].");

    _lossLayers = new ArrayList<>(lossLayers);

    // calculate the parent-to-children map (this also validates all layers)
    _parentToChildrenMap = parentToChildrenMap(_lossLayers);

    // find root layers
    _rootLayers = _parentToChildrenMap.keySet()
        .stream()
        .filter(layer -> layer instanceof NNRootLayer)
        .map(layer -> (NNRootLayer<?, ?>) layer)
        .collect(Collectors.toList());

    // calculate a topographically-sorted list of layers
    _topographicallySortedLayers = topographicSort(_parentToChildrenMap);

    // find a unique name for each layer
    _layerNames = uniqueLayerNames(_topographicallySortedLayers);

    // update our input list
    setInputs();
  }

  /**
   * Recomputes all the inputs to this neural network and updates the _inputs and _originalInputs fields.
   */
  private void setInputs() {
    Set<Producer<?>> networkInputSet = computeInputSet(); // find Producer inputs in the encapsulating DAG
    ArrayList<Producer<?>> inputList = new ArrayList<>(networkInputSet.size() + 1);
    inputList.add(_isEvaluationExampleInput); // this is always the first input
    inputList.addAll(networkInputSet);
    _inputs = inputList;
    _originalInputs = inputList; // store the original list in case _inputs is later changed by withInputsUnsafe()

  }

  // combine the set of inputs needed for dynamic configuration and the per-example inputs needed by the neural network
  // implementation
  private Set<Producer<?>> computeInputSet() {
    Set<? extends Producer<?>> dynamicConfigInputProducers = getDynamicConfigInputProducers();
    Set<? extends Producer<?>> exampleInputProducers = getExampleInputProducers();

    HashSet<Producer<?>> result =
        new HashSet<>(dynamicConfigInputProducers.size() + exampleInputProducers.size());
    result.addAll(dynamicConfigInputProducers);
    result.addAll(exampleInputProducers);

    return result;
  }

  // collect all the producers required to calculate the dynamic configurations of the layers
  private Set<? extends Producer<?>> getDynamicConfigInputProducers() {
    return _parentToChildrenMap.keySet()
        .stream()
        .flatMap(layer -> layer.internalAPI().getDynamicConfigurationInputProducers().stream())
        .collect(Collectors.toSet());
  }

  /**
   * Gets a {@link Set} of all the {@link Producer}s required to provide the per-example inputs, labels, or other
   * values needed by this neural network.
   *
   * The default implementation simply uses the original per-example {@link Producer}s, but derived classes may want
   * to transform or otherwise supplement these.  For example, {@code NNIntegerPlaceholderLayer} accepts a
   * {@link Number} input, but a derived implementation may want to convert that to a single-element vector or some
   * other, proprietary format.
   *
   * If an original/untransformed per-example producer is not needed, it should not be included in the returned list.
   *
   * The inputs for the neural network as a whole will consist of those returned by this method as well as those
   * retuned by invoking {@link NNLayer.InternalAPI#getDynamicConfigurationInputProducers()} on all the network's
   * layers.
   *
   * @return the list of inputs required by this neural network
   */
  protected Set<? extends Producer<?>> getExampleInputProducers() {
    PerExampleInputsLayerVisitor perExampleInputsLayerVisitor = new PerExampleInputsLayerVisitor();

    return _parentToChildrenMap.keySet()
        .stream()
        .flatMap(layer -> layer.accept(perExampleInputsLayerVisitor).stream())
        .collect(Collectors.toSet());
  }

  /**
   * Returns a copy of this instance that will optimize the specified loss layers.  As leaves in the neural network
   * DAG, the loss layers also define the graph, and a neural network must have at least one loss layer.
   *
   * By default, the {@link NNResult} produced by the neural network will include (only) the labels inferred via the
   * loss layers.  To specify different outputs for this instance, call {@link #withOutputLayers(NNChildLayer[])} after this
   * method.
   *
   * @param lossLayers the loss layers to optimize
   * @return a copy of this instance that will optimize the specified loss layers
   */
  @SafeVarargs
  public final S withLossLayers(NNChildLayer<?, ? extends LossLayer>... lossLayers) {
    Arguments.check(lossLayers.length > 0, "At least one loss layer must be provided");
    List<NNChildLayer<?, ? extends LossLayer>> lossLayerList = Arrays.asList(lossLayers.clone());
    Arguments.distinct(lossLayerList,
        (index, item) -> "The array of provided loss layers contains a duplicate at index " + index + " (" + item
            + ")");
    return clone(c -> ((AbstractNeuralNetwork<?, ?, ?>) c).setLossLayers(lossLayerList));
  }

  /**
   * Returns a copy of this instance where the outputs of the specified layers are made available as part of the
   * {@link NNResult} produced by this neural network during inference.  By default, the outputs of the neural network
   * are the labels inferred from the loss layers.
   *
   * Output layers must belong to the neural network this instance represents (and since a network is defined by its
   * loss layers, this means you must set them (e.g. via {@link #withLossLayers(NNChildLayer[])})
   * <strong>before</strong> calling this method.
   *
   * Attempting to specify an output layer outside the network will throw an {@link IllegalArgumentException}.
   * Additionally, an {@link IllegalArgumentException} exception will be thrown from a subsequent call to
   * {@link #withLossLayers(NNChildLayer[])} if you've previously called this method with a non-empty array of layers.
   * Calling this method with no arguments (or an empty array) will "unset" the output layers, and the loss layer labels
   * will then again be used as the outputs (per the default behavior).
   *
   * Note that there is potentially a non-trivial computational cost to including layers in the neural networks
   * {@link NNResult}; only the output layers that are actually needed should be specified as outputs.
   *
   * @param outputLayers the layers whose activations/outputs should be made available during inference
   * @return a copy of this instance that will include the output of the specified layers in its {@link NNResult}
   */
  public S withOutputLayers(NNChildLayer<?, ?>... outputLayers) {
    List<NNChildLayer<?, ?>> outputLayerList = Arrays.asList(outputLayers.clone());

    Arguments.distinct(outputLayerList,
        (index, item) -> "The array of provided visible layers contains a duplicate at index " + index + " (" + item
            + ")");
    Arguments.subset(outputLayerList, _parentToChildrenMap.keySet(),
        (index, layer) -> "The provided to-be-visible layer #" + index + " (" + layer
            + ") does not exist in the neural network; this may be because you called withVisibleLayers() prior to "
            + "defining the graph via withLossLayers().");

    return clone(
        c -> ((AbstractNeuralNetwork<?, ?, ?>) c)._outputLayers = outputLayerList.isEmpty() ? null : outputLayerList);
  }

  @Override
  public void validate() {
    Arguments.check(!_lossLayers.isEmpty(), "The neural network must have at least one loss layer");
    super.validate();
  }

  /**
   * Sorts the layers of a graph in topographical order, starting from the roots (no layer will precede one of its
   * ancestors in the resulting sorted list)
   *
   * @param parentToChildrenMap the parent-to-children layer map for the neural network
   * @return a list of layers in topographical order
   */
  private static List<NNLayer<?, ?>> topographicSort(Map<NNLayer<?, ?>, Set<NNChildLayer<?, ?>>>  parentToChildrenMap) {
    // we proceed by sorting into *reverse* topographical order, starting with the leaves (loss layers) and then
    // adding parent layers after we have processed all their children
    ArrayList<NNLayer<?, ?>> result = new ArrayList<>(parentToChildrenMap.size());

    // create a copy of the parentToChildrenMap
    Map<NNLayer<?, ?>, Set<NNChildLayer<?, ?>>> unsatisfiedDependenciesMap = new HashMap<>(parentToChildrenMap);
    unsatisfiedDependenciesMap.replaceAll((parent, children) -> new HashSet<>(children));

    // initialize the queue with all the leaf layers (these will be the loss layers of the network)
    ArrayDeque<NNLayer<?, ?>> queue = unsatisfiedDependenciesMap.entrySet()
        .stream()
        .filter(entry -> entry.getValue().isEmpty())
        .map(Map.Entry::getKey)
        .collect(Collectors.toCollection(ArrayDeque::new));

    while (!queue.isEmpty()) {
      NNLayer<?, ?> next = queue.poll();

      // add to reverse-sorted result
      result.add(next);

      // update unsatisfied dependency sets of its parents (if applicable)
      if (next instanceof NNChildLayer) {
        for (NNLayer<?, ?> parent : ((NNChildLayer<?, ?>) next).internalAPI().getInputLayers()) {
          Set<NNChildLayer<?, ?>> unsatisfied = unsatisfiedDependenciesMap.get(parent);
          assert unsatisfied.contains(next);
          unsatisfied.remove(next);
          if (unsatisfied.isEmpty()) { // if we've seen all its children, we can examine the parent
            queue.add(parent);
          }
        }
      }
    }

    // sanity check the result
    if (result.size() != parentToChildrenMap.size()) {
      throw new IllegalStateException("Topographical sort found a different number of layers than expected; "
          + "either the neural network is malformed or there is a bug in Dagli");
    }

    Collections.reverse(result);

    // further sanity checks
    assert result.get(0) instanceof NNRootLayer; // we should always start with a root
    assert result.get(result.size() - 1) instanceof LossLayer; // and end with a loss layer

    return result;
  }

  /**
   * Creates a {@link java.util.Map} from each layer to its children in the NN graph (with the graph defined by its
   * loss layers).
   *
   * Note that a layer can potentially act as a parent to a child layer multiple times; however, each child will only
   * occur once in the parent's set of children (this is enforced by the semantics of a {@link Set}).
   *
   * @param lossLayers the leaves that define the NN graph
   * @return a map from each layer to its set of children (this set is empty for loss layers)
   */
  private static Map<NNLayer<?, ?>, Set<NNChildLayer<?, ?>>> parentToChildrenMap(
      Collection<? extends NNChildLayer<?, ? extends LossLayer>> lossLayers) {
    Map<NNLayer<?, ?>, Set<NNChildLayer<?, ?>>> parentToChildMap = new HashMap<>();
    lossLayers.forEach(ll -> parentToChildMap.put(ll, Collections.emptySet())); // loss layers have no children

    // initialize our queue of nodes
    ArrayDeque<NNChildLayer<?, ?>> queue = new ArrayDeque<>(lossLayers);

    while (!queue.isEmpty()) {
      NNChildLayer<?, ?> next = queue.poll();
      next.internalAPI().validate(); // this is a good time to validate, before invalid layers can cause trouble

      for (NNLayer<?, ?> parentLayer : next.internalAPI().getInputLayers()) {
        assert parentLayer != null;

        Set<NNChildLayer<?, ?>> childrenSet = parentToChildMap.computeIfAbsent(parentLayer, k -> new HashSet<>());

        // we can be clever here and realize that if childrenSet is empty, this is the first time we've seen parentLayer
        if (childrenSet.isEmpty() && parentLayer instanceof NNChildLayer) {
          queue.add((NNChildLayer<?, ?>) parentLayer); // we'll need to explore this layer's parents
        }

        childrenSet.add(next); // record this child in it's parent's childrenSet
      }
    }

    return parentToChildMap;
  }

  protected Optimizer getOptimizer() {
    return _optimizer;
  }

  /**
   * Returns a copy of this instance that will use the specified optimization algorithm to train the parameters of this
   * network.  The default optimizer is {@link StochasticGradientDescent}.
   *
   * @param optimizer the optimizer to use
   * @return a copy of this instance that will use the specified optimization algorithm to train the parameters of this
   *         network
   */
  public S withOptimizer(Optimizer optimizer) {
    return clone(c -> ((AbstractNeuralNetwork<?, ?, ?>) c)._optimizer = optimizer);
  }

  /**
   * Base class for neural network preparers.
   *
   * Implementations will need to implement {@link #getMode()}, which determines whether the preparer operates in
   * "batch" or "streaming" mode; the former is preferable when it means you can use the encapsulating DAG to prepare
   * your data into a format amenable to your framework and then use the {@link ObjectReader} passed to
   * {@link #finish(ObjectReader, ObjectReader)} to iterate over the data as many times as needed for training.
   * With "streaming" mode you will need to store the data yourself for repeated iterations.  Of course, you may choose
   * to adopt "streaming" mode when only one pass over the data is required and "batching" mode otherwise.
   *
   * @param <R> the type of {@link NNResult} that will be the result of the prepared transformer
   * @param <N> the prepared neural network transformer
   * @param <S> the type of the preparable neural network transformer
   */
  static abstract protected class Preparer<R extends NNResult, N extends Prepared<R, N>, S extends AbstractNeuralNetwork<R, N, S>>
      extends AbstractPreparerDynamic<R, N> {

    private boolean _hasInitialized = false;
    private final S _neuralNetwork;

    private long _trainingExampleCount = 0;
    private long _evaluationExampleCount = 0;

    private ConcurrentLinkedDeque<InteractiveCommand> _pendingCommands = new ConcurrentLinkedDeque<>();
    private final StandardInputListener.Token _stdInListenerToken;

    /**
     * @return the queue of all waiting interactive commands for the Preparer to act on
     */
    protected ConcurrentLinkedDeque<InteractiveCommand> getPendingCommands() {
      return _pendingCommands;
    }

    /**
     * Gets the number of training examples <strong>seen so far</strong>, which may be less than the total number if
     * this method is called prior to {@code finish(...)}.
     *
     * @return the number of training examples seen so far
     */
    public long getTrainingExampleCount() {
      return _trainingExampleCount;
    }

    /**
     * Gets the number of held-out evaluation examples <strong>seen so far</strong>, which may be less than the total
     * number if this method is called prior to {@code finish(...)}.
     *
     * @return the number of training examples seen so far
     */
    public long getEvaluationExampleCount() {
      return _evaluationExampleCount;
    }

    /**
     * Called to initialize neural network training and actually construct the network in the underlying neural network
     * framework from the abstracted {@link NNLayer}s.
     *
     * This method is called during DAG execution, before the first call to
     * {@link #processTrainingExample(Object[])} or {@link #processEvaluationExample(Object[])}.
     *
     * @param layerToDynamicConfigMap a {@link Map} from each layer in the neural network to its
     *                                {@link DynamicLayerConfig}
     * @param dynamicInputs the {@link DynamicInputs} that may be used to find the {@link DynamicInputs.Accessor}
     *                  tokens that can be cached and used to performantly get the input values for each layer during
     *                  the subsequent preparation of the neural network
     * @param constantInputs provides type-safe access to the constant-result inputs provided to the neural network
     */
    protected abstract void initialize(HashMap<NNLayer<?, ?>, DynamicLayerConfig> layerToDynamicConfigMap,
        DynamicInputs dynamicInputs, DynamicInputs.ConstantInputs constantInputs);

    /**
     * Processes each training example.
     *
     * Ideally, the first pass of training will occur here (to allow training to proceed in parallel with the
     * calculation of later inputs earlier in the DAG), but this is not required.
     *
     * @param values an Object array of the input values for the example
     */
    protected abstract void processTrainingExample(Object[] values);

    /**
     * Processes each held-out evaluation example.
     *
     * Held-out evaluation examples may be used for logging training progress or early stopping.
     *
     * @param values an Object array of the input values for the example
     */
    protected abstract void processEvaluationExample(Object[] values);

    /**
     * After each example has been processed via {@link #processTrainingExample(Object[])} and
     * {@link #processEvaluationExample(Object[])}, finalizes preparation of this neural network.  All training (or all
     * training after the first pass) will occur here.  If {@link #getMode()} returned
     * {@link com.linkedin.dagli.preparer.PreparerMode#STREAM}, {@code trainingValuesReader} and
     * {@code evaluationValuesReader} will be null.
     *
     * @param trainingValuesReader if this is a BATCH-mode preparer, an {@link ObjectReader} over all the training
     *                             example input values (expressed as Object[]); if this is a STREAM-mode preparer, null
     * @param evaluationValuesReader if this is a BATCH-mode preparer, an {@link ObjectReader} over all the held-out
     *                               evaluation example input values (expressed as Object[]); if this is a STREAM-mode
     *                               preparer, null
     */
    protected abstract N finish(ObjectReader<Object[]> trainingValuesReader,
        ObjectReader<Object[]> evaluationValuesReader);

    /**
     * Called just before the preparer begins listening for interactive commands from stdin.
     */
    protected void startInteractiveCommands() {
      System.out.println("Interactive commands have been enabled.  Currently, only one command is available:");
      System.out.println("\"stop\": end training early, as soon as possible");
    }

    /**
     * Called when an interactive debugging command is received from stdin.
     *
     * @param input the line entered by the user
     */
    private void handleInteractiveCommand(String input) {
      InteractiveCommand command = parseInteractiveCommand(input);
      if (command != null) {
        _pendingCommands.add(command);
      }
    }

    /**
     * Called when an interactive debugging command is received from stdin.
     *
     * @param input the line entered by the user
     */
    protected InteractiveCommand parseInteractiveCommand(String input) {
      input = input.trim();
      if (input.isEmpty()) {
        return null;
      }

      switch (input) {
        case "stop":
          System.out.println("\"stop\" command received: neural network training will stop at the end of the current "
              + "epoch (or earlier).");
          return new Stop();
        default:
          System.out.println("The neural network preparer does not recognize this command: " + input);
          return null;
      }
    }

    /**
     * Creates a new preparer.
     *
     * @param neuralNetwork the neural network to be prepared
     */
    protected Preparer(S neuralNetwork) {
      _neuralNetwork = neuralNetwork;
      if (((AbstractNeuralNetwork<?, ?, ?>) neuralNetwork)._interactiveCommands) {
        startInteractiveCommands();
        _stdInListenerToken = StandardInputListener.register(this::handleInteractiveCommand);
      } else {
        _stdInListenerToken = null;
      }
    }

    private static Object2IntOpenHashMap<Producer<?>> producerToIndexMap(List<? extends Producer<?>> producers) {
      return new Object2IntOpenHashMap<Producer<?>>(producers.toArray(new Producer[producers.size()]),
          IntStream.range(0, producers.size()).toArray());
    }

    /**
     * @return the neural network being prepared
     */
    protected final S getNeuralNetwork() {
      return _neuralNetwork;
    }

    @Override
    public final void processUnsafe(Object[] values) {
      if (!_hasInitialized) {
        _hasInitialized = true;

        // We need to know what index in our array of input values corresponds with each of the producers specified
        // by the layers in the neural network; we don't actually need to know (or care) what the actual input producers
        // are *now*, so we use  the original inputs list (rather than the current list stored in _inputs).
        // Background information: the "original" and "current" lists may diverge due to calls to withInputsUnsafe(...).
        List<Producer<?>> originalInputs = ((AbstractNeuralNetwork<?, ?, ?>) _neuralNetwork)._originalInputs;

        DynamicInputs dynamicInputs = new DynamicInputs(originalInputs);
        DynamicInputs.ConstantInputs constantInputs = dynamicInputs.constantInputs(values);

        HashMap<NNLayer<?, ?>, DynamicLayerConfig> layerToDynamicConfigMap = new HashMap<>();
        for (NNLayer<?, ?> layer : _neuralNetwork.getTopographicallySortedLayers()) {
          layerToDynamicConfigMap.put(layer,
              layer.internalAPI().getDynamicConfig(layerToDynamicConfigMap, dynamicInputs, constantInputs));
        }

        initialize(layerToDynamicConfigMap, dynamicInputs, constantInputs);
      }

      if ((Boolean) values[IS_EVALUATION_EXAMPLE_INPUT_INDEX]) {
        _evaluationExampleCount++;
        processEvaluationExample(values);
      } else {
        _trainingExampleCount++;
        processTrainingExample(values);
      }
    }

    @Override
    public final PreparerResultMixed<? extends PreparedTransformer<R>, N> finishUnsafe(
        ObjectReader<Object[]> inputs) {
      try {
        if (_evaluationExampleCount > 0) {
          return new PreparerResult<>(
              finish(inputs.lazyFilter(vals -> !((Boolean) vals[IS_EVALUATION_EXAMPLE_INPUT_INDEX])),
                  inputs.lazyFilter(vals -> ((Boolean) vals[IS_EVALUATION_EXAMPLE_INPUT_INDEX]))));
        } else {
          return new PreparerResult<>(finish(inputs, ObjectReader.empty()));
        }
      } finally {
        if (_stdInListenerToken != null) {
          _stdInListenerToken.close();
        }
      }
    }
  }

  /**
   * The interface for all layer-oriented prepared neural networks.
   *
   * @param <R> the type of {@link NNResult} the neural network produces
   * @param <S> the derived type of the prepared neural network transformer
   */
  public interface Prepared<R extends NNResult, S extends Prepared<R, S>>
      extends PreparedTransformerDynamic<R>, ParameterStore<LayerHandle<?>> {

    /**
     * Gets the parameters for the specified layer in this neural network as an {@link MDArray}.
     *
     * Parameter arrays are specific to each neural network implementation and are not standardized; the layout
     * (and number) of parameters may differ between implementations.
     *
     * @param layer the layer whose parameters should be retrieved
     * @return an {@link MDArray} containing the layer's parameters
     */
    default Map<String, ? extends MDArray> getParameters(NNLayer<?, ?> layer) {
      return getParameters(layer.getHandle());
    }

    /**
     * Gets the parameters for the specified layer in this neural network as an {@link Map} from names to their
     * corresponding parameter tables.
     *
     * Parameter names and layout are specific to each neural network implementation and are not standardized; the
     * names, organization, and quantity of parameters may differ between implementations.
     *
     * Many layers have no parameters; these will yield an empty map.
     *
     * @param layerHandle a handle to the layer whose parameters should be retrieved
     * @return an {@link MDArray} containing the layer's parameters
     */
    @Override
    Map<String, ? extends MDArray> getParameters(LayerHandle<?> layerHandle);
  }
}
