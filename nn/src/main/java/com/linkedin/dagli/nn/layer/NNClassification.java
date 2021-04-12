package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.visitor.VisitedBy;
import com.linkedin.dagli.distribution.BinaryDistributionFromProbability;
import com.linkedin.dagli.distribution.DistributionFromVector;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.math.vector.SparseIndexArrayVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.nn.activation.ActivationFunction;
import com.linkedin.dagli.nn.activation.Sigmoid;
import com.linkedin.dagli.nn.activation.SoftMax;
import com.linkedin.dagli.nn.loss.BinaryCrossEntropyLoss;
import com.linkedin.dagli.nn.loss.LossFunction;
import com.linkedin.dagli.nn.loss.MultinomialCrossEntropyLoss;
import com.linkedin.dagli.nn.result.NNResult;
import com.linkedin.dagli.object.ConditionalValue;
import com.linkedin.dagli.object.Index;
import com.linkedin.dagli.object.Indices;
import com.linkedin.dagli.object.UnknownItemPolicy;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.DynamicInputs;
import com.linkedin.dagli.vector.ManyHotVector;
import com.linkedin.dagli.vector.VectorElementAtIndex;
import java.util.Map;
import java.util.Objects;


/**
 * Represents a multinomial or multilabel classification loss "layer" used to train the neural network (and provides a
 * convenient way to extract {@link com.linkedin.dagli.math.distribution.DiscreteDistribution}s over the predicted
 * labels), with the true labels provided by a {@link com.linkedin.dagli.producer.Producer} in the encapsulating DAG.
 *
 * In multinomial classification, there is exactly one true label for every example, and the probabilities in the
 * predicted distribution over the possible labels will sum to 1 (modulo minor floating point imprecision), or possibly
 * less than 1 if the model allows for it (this can be achieved using an appropriately configured "direct" input layer
 * specified with {@link #withPredictionInput(NNLayer)}).
 *
 * In multilabel classification, there can be any number of true labels (possibly none) for every example, and the
 * probabilities in the predicted {@link DiscreteDistribution} can potentially sum to any value between 0 and the number
 * of distinct possible labels.
 *
 * Normally, {@link NNClassification} automatically creates a hidden dense layer with either a softmax (for
 * multinomial classification) or logistic/sigmoid (for multilabel classification) activation function so that the
 * provided input layer's width (the size of the vector of numbers it provides to this node) does not need to match the
 * number of labels.  This layer is created as an intermediary between the {@link NNClassification} instance and the
 * nominal input layer provided via the {@link #withFeaturesInput(NNLayer)} or other "withFeaturesInputFrom____(...)"
 * methods.
 *
 * This can be avoided using the {@link #withPredictionInput(NNLayer)} method, in which case the provided input
 * layer is used directly, with no new intermediate perceptron layer.  However, this does entail some constraints on the
 * input layer:
 * (1) the number of distinct labels <strong>must not</strong> exceed the width of the input layer.  If the number of
 *     labels does exceed the width of the input layer, which can only be detected when the containing DAG is being
 *      prepared, a runtime exception will be thrown.
 * (2) Care must be applied that the values provided by the input layer are valid [0, 1] probabilities and, in the case
 *     of multinomial classification, these probabilities should sum to approximately 1 (or less).  Violating this
 *     assumption (e.g. by using a perceptron input layer with an identity activation function) could result in logic
 *     bugs or exceptions.
 *
 * When computing the loss, the activations from the network are compared to a one-hot vector corresponding to the
 * "true" label in multinomial classification or a many-hot vector corresponding to the "true" labels in multilabel
 * classification.
 *
 * It is possible to treat binary problems as either:
 * (1) multinomial if the labels are provided using {@link #withMultinomialLabelInput(Producer)}, in which case the
 * parent layer should provide two input values summing to ~1, one for "true" and one for "false").
 * (2) multilabel if the labels are provided using {@link #withBinaryLabelInput(Producer)}, in which case the parent
 * layer should provide a single input value (call it y'), and the inferred probability for true will be taken as y'
 * while the inferred probability for false will be taken as (1 - y').
 *
 * In general, using {@link #withBinaryLabelInput(Producer)} should be preferred for binary problems as it reduces the
 * number of parameters in the network without much cost to the model's expressiveness.
 *
 * @param <L> the type of the label used for classification
 */
@VisitedBy("NNLayerVisitor")
public class NNClassification<L> extends AbstractVectorLossLayer<DiscreteDistribution<L>, NNClassification<L>> {
  private static final long serialVersionUID = 1;

  private NNLayer<DenseVector, ? extends NonTerminalLayer> _nominalInput = null;
  private boolean _directInput = false;

  private Producer<Integer> _widthProvider = null;
  private DistributionFromVector<L> _vectorToDistributionTransformer = null; // always null for binary classification

  private LossFunction _lossFunction = null; // => default
  private ClassificationType _classificationType = null;

  /**
   * Varying types of classification supported by this layer.
   */
  private enum ClassificationType {
    MULTINOMIAL(new MultinomialCrossEntropyLoss(), new SoftMax()),
    BINARY(new BinaryCrossEntropyLoss(), new Sigmoid()),
    MULTILABEL(new BinaryCrossEntropyLoss(), new Sigmoid());

    final LossFunction _defaultLossFunction;
    final ActivationFunction _defaultParentLayerActivationFunction; // used only if user doesn't specify a "direct" input
    ClassificationType(LossFunction defaultLossFunction, ActivationFunction defaultParentLayerActivationFunction) {
      _defaultLossFunction = defaultLossFunction;
      _defaultParentLayerActivationFunction = defaultParentLayerActivationFunction;
    }

    /**
     * @return the defalt loss function to be used for this classification type
     */
    LossFunction getDefaultLossFunction() {
      return _defaultLossFunction;
    }

    /**
     * Gets the activation function that should be adopted by the automatically-generated dense parent layer when this
     * classification type is used.
     *
     * Not applicable if a "direct input" is provided.
     *
     * @return the activation function that should be adopted by the automatically-generated dense parent layer
     */
    ActivationFunction getDefaultParentLayerActivationFunction() {
      return _defaultParentLayerActivationFunction;
    }
  }

  @Override
  DynamicLayerConfig getDynamicConfig(Map<NNLayer<?, ?>, DynamicLayerConfig> ancestorConfigs,
      DynamicInputs dynamicInputs, DynamicInputs.ConstantInputs constantInputs) {
    return DynamicLayerConfig.Builder
        .setOutputShape(ancestorConfigs.get(_inputLayer).getOutputShape().clone()).build();
  }

  @Override
  @SuppressWarnings("unchecked") // due to semantics of Producer and our knowledge L = Boolean, cast below is safe
  Producer<DiscreteDistribution<L>> outputFromNNResult(Producer<? extends NNResult> nnResultProducer, int outputIndex) {
    Producer<DenseVector> outputVectorProvider = NNResult.InternalAPI.toVector(nnResultProducer, (outputIndex));

    if (_vectorToDistributionTransformer == null) {
      // binary classification--this implies that L == Boolean
      assert _classificationType == ClassificationType.BINARY;
      return (Producer<DiscreteDistribution<L>>) (Producer<? extends DiscreteDistribution<L>>)
          new BinaryDistributionFromProbability().withClippedOutOfRangeProbabilities()
              .withInput(new VectorElementAtIndex().withVectorInput(outputVectorProvider).withIndex(0));
    } else {
      // probabilities are clipped to [0, 1] and, for multinomial classification, normalized such that they sum to <= 1
      return _vectorToDistributionTransformer.withNormalization(_classificationType == ClassificationType.MULTINOMIAL
          ? DistributionFromVector.Normalization.SUM_TO_ONE_OR_LESS : DistributionFromVector.Normalization.CLIPPED)
          .withVectorInput(outputVectorProvider);
    }
  }

  @Override
  void validate() {
    Objects.requireNonNull(_supervisionProvider,
        "Labels have not been specified for the classification layer " + getName());
    Objects.requireNonNull(_nominalInput,
        "An input layer has not been specified for the classification layer " + getName());

    super.validate();
  }

  @Override
  protected NNClassification<L> clone() {
    NNClassification<L> clone = super.clone();

    // clear our _input: we need to make sure that calling getInput() on each instance of this class always returns
    // the same object; otherwise we could conceivably introduce a logic bug as creating duplicate
    // semantically-indistinguishable layers is not idempotent in neural networks.
    clone._inputLayer = null;

    return clone;
  }

  @Override
  NNLayer<DenseVector, ? extends NonTerminalLayer> getInputLayer() {
    // do we need to provide a new input instance?
    if (_inputLayer == null) {
      // do we have enough information to generate the input instance yet?
      if (_directInput) {
        _inputLayer = _nominalInput;
      } else {
        _inputLayer = new NNDenseLayer().withInput(_nominalInput)
            .withActivationFunction(_classificationType.getDefaultParentLayerActivationFunction())
            .withUnitCount(_widthProvider);
      }
    }
    return _inputLayer; // may still be null
  }

  @Override
  public <R> R accept(NNLayerVisitor<R> visitor) {
    return visitor.visit(this);
  }

  /**
   * Returns a copy of this instance that will perform multinomial classification using the labels (which determine the
   * label space and are used to calculate the loss) from the specified {@link Producer} to supervise training.
   *
   * @param labelInput the producer that will provide the labels used to train the neural network
   * @return a copy of this instance that will obtain its labels from the specified source
   */
  public NNClassification<L> withMultinomialLabelInput(Producer<? extends L> labelInput) {
    return clone(c -> {
      // the policy we use doesn't matter so long as it doesn't result in the indexed count being different than the
      // number of unique items being encountered (which the default policy does): this is because the labels are not
      // used later in the prepared DAG when new values could be encountered.
      Index<L> labelIndex =
          new Index<L>().withUnknownItemPolicy(UnknownItemPolicy.MOST_FREQUENT).withInput(labelInput);
      c._supervisionProvider = new ManyHotVector().withInputs(labelIndex);
      c._widthProvider = labelIndex.asIndexedCount();
      c._vectorToDistributionTransformer =
          new DistributionFromVector<L>().withIndexToLabelMapInput(labelIndex.asLongIndexToObjectMap());
      c._classificationType = ClassificationType.MULTINOMIAL;
    });
  }

  /**
   * Returns a copy of this instance that will perform binary classification using the labels (which are used to
   * calculate the loss) from the specified {@link Producer} to supervise training.
   *
   * @param labelInput the producer that will provide the labels used to train the neural network
   * @return a copy of this instance that will obtain its labels from the specified source
   */
  @SuppressWarnings("unchecked") // a NNClassification with a Boolean label is always a NNClassification<Boolean>
  public NNClassification<Boolean> withBinaryLabelInput(Producer<Boolean> labelInput) {
    return (NNClassification<Boolean>) clone(c -> {
      c._supervisionProvider = new ConditionalValue<Vector>()
          .withConditionInput(labelInput)
          .withValueIfConditionTrue(new SparseIndexArrayVector(new long[] {0}, 1.0))
          .withValueIfConditionFalse(Vector.empty());
      c._widthProvider = new Constant<>(1);
      c._vectorToDistributionTransformer = null; // always null for boolean labels
      c._classificationType = ClassificationType.BINARY;
    });
  }

  /**
   * Returns a copy of this instance that will perform multinomial classification using the labels (which determine the
   * label space and are used to calculate the loss) from the specified {@link Producer} to supervise training.
   *
   * @param labelInput the producer that will provide the labels used to train the neural network
   * @return a copy of this instance that will obtain its labels from the specified source
   */
  public NNClassification<L> withMultilabelLabelsInput(Producer<? extends Iterable<? extends L>> labelInput) {
    return clone(c -> {
      // the policy we use doesn't matter so long as it doesn't result in the indexed count being different than the
      // number of unique items being encountered (which the default policy does): this is because the labels are not
      // used later in the prepared DAG when new values could be encountered.
      Indices<L> labelIndices =
          new Indices<L>().withUnknownItemPolicy(UnknownItemPolicy.MOST_FREQUENT).withInput(labelInput);
      c._supervisionProvider = new ManyHotVector().withInputList(labelIndices);
      c._widthProvider = labelIndices.asIndexedCount();
      c._vectorToDistributionTransformer =
          new DistributionFromVector<L>().withIndexToLabelMapInput(labelIndices.asLongIndexToObjectMap());
      c._classificationType = ClassificationType.MULTILABEL;
    });
  }

  @Override
  protected NNClassification<L> withInput(NNLayer<DenseVector, ? extends NonTerminalLayer> input) {
    return clone(c -> {
      c._nominalInput = input;
      c._directInput = false;
    });
  }

  /**
   * Returns a copy of this instance that will use the specified layer as its immediate input, without an intervening
   * softmax perceptron layer.  The values from this input will be taken as the literal prediction of the network.  This
   * means that the width of the provided input layer should be at least as large as the number of distinct labels
   * (except when binary labels are specified via {@link #withBinaryLabelInput(Producer)}; in this case, the input layer
   * should provide a single value, the probability that the label is "true").
   *
   * Additionally, input values should be [0, 1], although because clipping (and, for multinomial prediction,
   * normalization) will be applied to the resultant discrete distribution this is not a hard requirement except insofar
   * as being outside this range may break some loss functions.
   *
   * @param input the layer on which the classification loss is to be (directly) calculated
   * @return a copy of this instance that will use the specified input layer
   */
  public NNClassification<L> withPredictionInput(NNLayer<DenseVector, ? extends NonTerminalLayer> input) {
    return clone(c -> {
      c._nominalInput = input;
      c._directInput = true;
    });
  }

  /**
   * @return a configurator that will configure the "direct" prediction input to this loss layer with no intervening
   *         softmax perceptron layer (see {@link #withPredictionInput(NNLayer)} for more information)
   */
  public DenseLayerInput<NNClassification<L>> withPredictionInput() {
    return new DenseLayerInput<>(this::withPredictionInput);
  }

  /**
   * @return the loss function used by this layer
   */
  public LossFunction getLossFunction() {
    return _lossFunction == null ? _classificationType.getDefaultLossFunction() : _lossFunction;
  }

  /**
   * Returns a copy of this instance that will use the specified loss function.  The default loss function is
   * {@link MultinomialCrossEntropyLoss} for multinomial classification, and
   * {@link BinaryCrossEntropyLoss} for binary and multilabel classification.
   *
   * @param lossFunction the loss function to be used
   * @return a copy of this instance that will use the specified loss function
   */
  public NNClassification<L> withLossFunction(LossFunction lossFunction) {
    return clone(c -> c._lossFunction = lossFunction);
  }

  /**
   * Returns a copy of this layer that will accept the output of the provided layer as its input.
   *
   * The input values will be passed through an automatically-created dense layer whose number of outputs will
   * match the number of labels (except if {@link #withBinaryLabelInput(Producer)} is used to create a binary
   * classifier, in which case the automatically-created layer will have a single output value).
   *
   * @param inputLayer the layer that will provide a vector of input values to this one
   * @return a copy of this layer that will accept the specified input layer
   */
  public NNClassification<L> withFeaturesInput(NNLayer<DenseVector, ? extends NonTerminalLayer> inputLayer) {
    return withInput(inputLayer);
  }

  /**
   * @return a configurator that will configure the features input to this layer
   */
  public DenseLayerInput<NNClassification<L>> withFeaturesInput() {
    return new DenseLayerInput<>(this::withFeaturesInput);
  }
}
