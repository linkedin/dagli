package com.linkedin.dagli.dl4j;

import com.linkedin.dagli.nn.layer.AttentionLayerConfig;
import com.linkedin.dagli.nn.layer.Bidirectionality;
import com.linkedin.dagli.nn.layer.DynamicLayerConfig;
import com.linkedin.dagli.nn.layer.IntegerInputLayerConfig;
import com.linkedin.dagli.nn.layer.NNActivationLayer;
import com.linkedin.dagli.nn.layer.NNBatchNormalizedLayer;
import com.linkedin.dagli.nn.layer.NNChildLayer;
import com.linkedin.dagli.nn.layer.NNClassification;
import com.linkedin.dagli.nn.layer.NNDenseLayer;
import com.linkedin.dagli.nn.layer.NNDotProductLayer;
import com.linkedin.dagli.nn.layer.NNEmbeddingLayer;
import com.linkedin.dagli.nn.layer.NNIntegerInputLayer;
import com.linkedin.dagli.nn.layer.NNIntegerSequenceInputLayer;
import com.linkedin.dagli.nn.layer.NNLSTMLayer;
import com.linkedin.dagli.nn.layer.NNLastVectorInSequenceLayer;
import com.linkedin.dagli.nn.layer.NNLayer;
import com.linkedin.dagli.nn.layer.NNLayerVisitor;
import com.linkedin.dagli.nn.layer.NNLearnedSelfAttentionLayer;
import com.linkedin.dagli.nn.layer.NNLinearizedVectorSequenceLayer;
import com.linkedin.dagli.nn.layer.NNMaxPoolingLayer;
import com.linkedin.dagli.nn.layer.NNMeanPoolingLayer;
import com.linkedin.dagli.nn.layer.NNPNormPoolingLayer;
import com.linkedin.dagli.nn.layer.NNPositionalEncodedLayer;
import com.linkedin.dagli.nn.layer.NNRecurrentAttentionLayer;
import com.linkedin.dagli.nn.layer.NNRegression;
import com.linkedin.dagli.nn.layer.NNRootLayer;
import com.linkedin.dagli.nn.layer.NNSelfAttentionLayer;
import com.linkedin.dagli.nn.layer.NNSequentialDenseLayer;
import com.linkedin.dagli.nn.layer.NNSequentialEmbeddingLayer;
import com.linkedin.dagli.nn.layer.NNSplitVectorSequenceLayer;
import com.linkedin.dagli.nn.layer.NNSumPoolingLayer;
import com.linkedin.dagli.nn.layer.NNVectorConcatenationLayer;
import com.linkedin.dagli.nn.layer.NNVectorConcatenationSequenceLayer;
import com.linkedin.dagli.nn.layer.NNVectorHadamardProductLayer;
import com.linkedin.dagli.nn.layer.NNVectorInputLayer;
import com.linkedin.dagli.nn.layer.NNVectorMeanLayer;
import com.linkedin.dagli.nn.layer.NNVectorSequenceHadamardProductLayer;
import com.linkedin.dagli.nn.layer.NNVectorSequenceInputLayer;
import com.linkedin.dagli.nn.layer.NNVectorSequenceMeanLayer;
import com.linkedin.dagli.nn.layer.NNVectorSequenceSumLayer;
import com.linkedin.dagli.nn.layer.NNVectorSumLayer;
import com.linkedin.dagli.nn.layer.NonTerminalLayer;
import com.linkedin.dagli.util.array.ArraysEx;
import java.util.List;
import java.util.Map;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.graph.ElementWiseVertex;
import org.deeplearning4j.nn.conf.graph.MergeVertex;
import org.deeplearning4j.nn.conf.graph.SubsetVertex;
import org.deeplearning4j.nn.conf.graph.rnn.LastTimeStepVertex;
import org.deeplearning4j.nn.conf.layers.ActivationLayer;
import org.deeplearning4j.nn.conf.layers.BatchNormalization;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.EmbeddingLayer;
import org.deeplearning4j.nn.conf.layers.EmbeddingSequenceLayer;
import org.deeplearning4j.nn.conf.layers.GlobalPoolingLayer;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.LearnedSelfAttentionLayer;
import org.deeplearning4j.nn.conf.layers.LossLayer;
import org.deeplearning4j.nn.conf.layers.PoolingType;
import org.deeplearning4j.nn.conf.layers.RecurrentAttentionLayer;
import org.deeplearning4j.nn.conf.layers.SelfAttentionLayer;
import org.deeplearning4j.nn.conf.layers.recurrent.Bidirectional;
import org.deeplearning4j.nn.conf.layers.recurrent.TimeDistributed;
import org.deeplearning4j.nn.conf.layers.samediff.SameDiffLayerUtils;
import org.deeplearning4j.nn.conf.layers.util.MaskLayer;
import org.nd4j.linalg.activations.impl.ActivationIdentity;


/**
 * Visits layers of the neural network and adds the corresponding DL4J layers/vertices to a graph builder.
 */
class NetworkBuilderLayerVisitor implements NNLayerVisitor<Void> {
  private static final ActivationFunctionConverterVisitor ACTIVATION_CONVERTER =
      new ActivationFunctionConverterVisitor();
  private final NeuralNetwork _neuralNetwork;
  private final ComputationGraphConfiguration.GraphBuilder _graphBuilder;
  private final Map<NNLayer<?, ?>, String> _layerNames;
  private final Map<NNLayer<?, ?>, DynamicLayerConfig> _dynamicConfigs;

  /**
   * Creates a new visitor that will build the graph
   *
   * @param neuralNetwork the neural network for whom the graph is being built
   * @param graphBuilder a graph builder to which layers and vertices will be added
   * @param layerNames the unique layer names
   * @param dynamicConfigs dynamic configuration for each layer
   */
  public NetworkBuilderLayerVisitor(NeuralNetwork neuralNetwork, ComputationGraphConfiguration.GraphBuilder graphBuilder,
      Map<NNLayer<?, ?>, String> layerNames, Map<NNLayer<?, ?>, DynamicLayerConfig> dynamicConfigs) {
    _neuralNetwork = neuralNetwork;
    _graphBuilder = graphBuilder;
    _layerNames = layerNames;
    _dynamicConfigs = dynamicConfigs;
  }

  private String[] getParentNames(NNChildLayer<?, ?> node) {
    return node.internalAPI().getInputLayers().stream().map(_layerNames::get).toArray(String[]::new);
  }

  private double dropoutValue(double dropoutProbability) {
    // DL4J's dropout value is the probability of *retaining* an input, whereas ours is the probability of dropping it
    return Double.isNaN(dropoutProbability) ? dropoutValue(_neuralNetwork.getDropoutProbability())
        : (1.0 - dropoutProbability);
  }

  @Override
  public <L> Void visit(NNClassification<L> node) {
    _graphBuilder.addLayer(_layerNames.get(node), new LossLayer.Builder().lossFunction(node.getLossFunction()
            .accept(new LossFunctionConverterVisitor(_dynamicConfigs.get(node).getOutputElementCount()))).build(),
        getParentNames(node));
    return null;
  }

  private static Bidirectional.Mode getBidirectionalityMode(Bidirectionality bidirectionality) {
    switch (bidirectionality) {
      case FORWARD_ONLY:
        throw new IllegalArgumentException(
            "Forward-only has no corresponding DL4J bidrectional mode; this is a bug in Dagli");
      case CONCATENATED:
        return Bidirectional.Mode.CONCAT;
      case SUMMED:
        return Bidirectional.Mode.ADD;
      case MULITIPLIED:
        return Bidirectional.Mode.MUL;
      case MEAN:
        return Bidirectional.Mode.AVERAGE;
      default:
        throw new IllegalArgumentException("Unknown bidirectionality: " + bidirectionality);
    }
  }

  @Override
  public Void visit(NNLSTMLayer visited) {
    // the number of units is not always the same as the output width:
    int outputWidth = Math.toIntExact(_dynamicConfigs.get(visited).getOutputShape()[1]);
    int units = (visited.getBidirectionality() == Bidirectionality.CONCATENATED) ? outputWidth / 2 : outputWidth;

    LSTM layer = new LSTM.Builder().activation(visited.getActivation().accept(ACTIVATION_CONVERTER))
        .gateActivationFunction(visited.getRecurrentActivation().accept(ACTIVATION_CONVERTER))
        .units(units)
        .nIn(_dynamicConfigs.get(visited.internalAPI().getInputLayer()).getOutputShape()[1])
        .dropOut(dropoutValue(visited.getDropoutProbability()))
        .build();

    _graphBuilder.addLayer(_layerNames.get(visited),
        visited.getBidirectionality() == Bidirectionality.FORWARD_ONLY ? layer
            : new Bidirectional(getBidirectionalityMode(visited.getBidirectionality()), layer),
        getParentNames(visited)[0]);

    return null;
  }

  @Override
  public Void visit(NNVectorConcatenationLayer node) {
    _graphBuilder.addVertex(_layerNames.get(node), new MergeVertex(), getParentNames(node));
    return null;
  }

  @Override
  public Void visit(NNVectorConcatenationSequenceLayer node) {
    _graphBuilder.addVertex(_layerNames.get(node), new MergeVertex(), getParentNames(node));
    return null;
  }

  @Override
  public Void visit(NNEmbeddingLayer node) {
    _graphBuilder.addLayer(_layerNames.get(node), new EmbeddingLayer.Builder().activation(new ActivationIdentity())
        .nIn(((IntegerInputLayerConfig) _dynamicConfigs.get(node.internalAPI().getInputLayer())).getMaxIntegerValue()
            + 1)
        .nOut(_dynamicConfigs.get(node).getOutputElementCount())
        .build(), getParentNames(node));
    return null;
  }

  @Override
  public Void visit(NNVectorSequenceInputLayer visited) {
    // input "layers" are just names in the DL4J graph
    return null;
  }

  @Override
  public Void visit(NNIntegerInputLayer node) {
    // input "layers" are just names in the DL4J graph
    return null;
  }

  @Override
  public Void visit(NNIntegerSequenceInputLayer node) {
    // input "layers" are just names in the DL4J graph
    return null;
  }

  @Override
  public Void visit(NNDenseLayer visited) {
    _graphBuilder.addLayer(_layerNames.get(visited),
        new DenseLayer.Builder().activation(visited.getActivation().accept(ACTIVATION_CONVERTER))
            .nIn(_dynamicConfigs.get(visited.internalAPI().getInputLayer()).getOutputElementCount())
            .nOut(_dynamicConfigs.get(visited).getOutputElementCount())
            .dropOut(dropoutValue(visited.getDropoutProbability()))
            //.hasLayerNorm(visited.getLayerNormalization())
            .build(), getParentNames(visited));
    return null;
  }

  @Override
  public Void visit(NNRegression node) {
    _graphBuilder.addLayer(_layerNames.get(node), new LossLayer.Builder().lossFunction(node.getLossFunction()
            .accept(new LossFunctionConverterVisitor(_dynamicConfigs.get(node).getOutputElementCount()))).build(),
        getParentNames(node));
    return null;
  }

  @Override
  public Void visit(NNSequentialEmbeddingLayer visited) {
    _graphBuilder.addLayer(_layerNames.get(visited), new EmbeddingSequenceLayer.Builder()
        .activation(new ActivationIdentity())
        .nIn(((IntegerInputLayerConfig) _dynamicConfigs.get(visited.internalAPI().getInputLayer())).getMaxIntegerValue()
            + 1)
        .nOut(_dynamicConfigs.get(visited).getOutputShape()[1])
        .build(), getParentNames(visited));

    return null;
  }

  @Override
  public Void visit(NNVectorInputLayer node) {
    // input "layers" are just names in the DL4J graph
    return null;
  }

  private String createSubsetLayer(String parentName, int desiredWidth) {
    String subLayerName = parentName + "[0:" + desiredWidth + "]";
    // only create new vertex if necessary
    if (!_graphBuilder.getVertices().containsKey(subLayerName)) {
      _graphBuilder.addVertex(subLayerName, new SubsetVertex(0, desiredWidth - 1), parentName);
    }

    return subLayerName;
  }

  @Override
  public Void visit(NNDotProductLayer visited) {
    NNLayer<?, ?> parent1 = visited.internalAPI().getFirstInputLayer();
    NNLayer<?, ?> parent2 = visited.internalAPI().getSecondInputLayer();

    long width1 = _dynamicConfigs.get(parent1).getOutputElementCount();
    long width2 = _dynamicConfigs.get(parent2).getOutputElementCount();

    String parent1Name = _layerNames.get(parent1);
    String parent2Name = _layerNames.get(parent2);

    if (width1 > width2) {
      parent1Name = createSubsetLayer(parent1Name, Math.toIntExact(width2));
    } else if (width1 < width2) {
      parent2Name = createSubsetLayer(parent2Name, Math.toIntExact(width1));
    }

    _graphBuilder.addVertex(_layerNames.get(visited), new DotProductVertex(), parent1Name, parent2Name);
    return null;
  }

  private void addElementWiseLayer(NNChildLayer<?, ?> visited, ElementWiseVertex.Op op) {
    // we depend on the layers themselves to verify that their parents' dimensions match where this is required
    int minWidth = Math.toIntExact(visited.internalAPI()
        .getInputLayers()
        .stream()
        .map(_dynamicConfigs::get)
        .mapToLong(DynamicLayerConfig::getOutputElementCount)
        .min().orElseThrow(() -> new IllegalStateException("Child layer has no parents")));

    String[] parentNames = visited.internalAPI()
        .getInputLayers()
        .stream()
        .map(layer -> _dynamicConfigs.get(layer).getOutputElementCount() > minWidth ? createSubsetLayer(
            _layerNames.get(layer), minWidth) : _layerNames.get(layer))
        .toArray(String[]::new);

    _graphBuilder.addVertex(_layerNames.get(visited), new ElementWiseVertex(op), parentNames);
  }

  private void addElementWiseSequenceLayer(NNChildLayer<?, ?> visited, ElementWiseVertex.Op op) {
    // we depend on the layers themselves to verify that their parents' dimensions match
    _graphBuilder.addVertex(_layerNames.get(visited), new ElementWiseVertex(op), getParentNames(visited));
  }

  @Override
  public Void visit(NNVectorHadamardProductLayer visited) {
    addElementWiseLayer(visited, ElementWiseVertex.Op.Product);
    return null;
  }

  @Override
  public Void visit(NNVectorSequenceHadamardProductLayer visited) {
    addElementWiseSequenceLayer(visited, ElementWiseVertex.Op.Product);
    return null;
  }

  @Override
  public Void visit(NNVectorMeanLayer visited) {
    addElementWiseLayer(visited, ElementWiseVertex.Op.Average);
    return null;
  }

  @Override
  public Void visit(NNVectorSequenceSumLayer visited) {
    addElementWiseSequenceLayer(visited, ElementWiseVertex.Op.Add);
    return null;
  }

  @Override
  public Void visit(NNVectorSequenceMeanLayer visited) {
    addElementWiseSequenceLayer(visited, ElementWiseVertex.Op.Average);
    return null;
  }

  @Override
  public Void visit(NNVectorSumLayer visited) {
    addElementWiseLayer(visited, ElementWiseVertex.Op.Add);
    return null;
  }

  @Override
  public Void visit(NNActivationLayer visited) {
    _graphBuilder.addLayer(_layerNames.get(visited), new ActivationLayer.Builder().activation(
        visited.getActivation().accept(ACTIVATION_CONVERTER))
        .dropOut(dropoutValue(visited.getDropoutProbability()))
        .build(), getParentNames(visited));
    return null;
  }

  /**
   * Adds a global pooling layer.
   *
   * @param visited the Dagli pooling layer being processed
   * @param poolingType the type of pooling being done
   * @param p the p-value for the p-norm, if applicable; otherwise can be any legal value (such as 2)
   * @param dropoutProbability the dropout probability for the pooling layer
   * @return null
   */
  private Void addPoolingLayer(NNChildLayer<?, ?> visited, PoolingType poolingType, int p, double dropoutProbability) {
    _graphBuilder.addLayer(_layerNames.get(visited), new GlobalPoolingLayer.Builder().poolingType(poolingType)
        .pnorm(p)
        .dropOut(dropoutValue(dropoutProbability))
        .build(), getParentNames(visited));
    return null;
  }

  @Override
  public Void visit(NNSumPoolingLayer visited) {
    return addPoolingLayer(visited, PoolingType.SUM, 2, visited.getDropoutProbability());
  }

  @Override
  public Void visit(NNMaxPoolingLayer visited) {
    return addPoolingLayer(visited, PoolingType.MAX, 2, visited.getDropoutProbability());
  }

  @Override
  public Void visit(NNMeanPoolingLayer visited) {
    return addPoolingLayer(visited, PoolingType.AVG, 2, visited.getDropoutProbability());
  }

  @Override
  public Void visit(NNPNormPoolingLayer visited) {
    return addPoolingLayer(visited, PoolingType.PNORM, visited.getP(), visited.getDropoutProbability());
  }

  @Override
  public Void visit(NNLastVectorInSequenceLayer visited) {
    // to create LastTimeStepVertex we need to figure out the name of the input that ultimately provided the sequence
    // we're transforming (and note that this implicitly assumes no unary operations can change the sequence length,
    // which is true for the moment):
    NNLayer<?, ?> ancestor = visited.internalAPI().getInputLayer();
    while (ancestor instanceof NNChildLayer) {
      List<? extends NNLayer<?, ? extends NonTerminalLayer>> furtherAncestors =
          ((NNChildLayer<?, ?>) ancestor).internalAPI().getInputLayers();

      if (furtherAncestors.size() != 1) {
        throw new UnsupportedOperationException("Dagli's DL4J network adapter does not currently support "
            + "NNLastVectorInSequenceLayers that have an ancestor with multiple input layers; found such a layer: "
            + ancestor);
      }
      ancestor = furtherAncestors.get(0);
    }

    assert ancestor instanceof NNRootLayer;

    // the name of our (root) ancestor is the name of the corresponding (mask) input we need
    String maskInputName = _layerNames.get(ancestor);

    _graphBuilder.addVertex(_layerNames.get(visited), new LastTimeStepVertex(maskInputName), getParentNames(visited));
    return null;
  }

  @Override
  public Void visit(NNLinearizedVectorSequenceLayer visited) {
    String maskedName = _layerNames.get(visited) + "-Masked";
    if (!_graphBuilder.getVertices().containsKey(maskedName)) {
      _graphBuilder.addLayer(maskedName, new MaskLayer(), getParentNames(visited));
    }
    _graphBuilder.addVertex(_layerNames.get(visited), new ReshapeMasklessVertex(
        ArraysEx.concat(new long[]{-1}, _dynamicConfigs.get(visited).getOutputShape())), maskedName);
    return null;
  }

  @Override
  public Void visit(NNSplitVectorSequenceLayer visited) {
    long[] newShape = _dynamicConfigs.get(visited).getOutputShape();
    _graphBuilder.addVertex(_layerNames.get(visited), new ReshapeMasklessVertex(
        new long[]{-1, newShape[1], newShape[0]}), getParentNames(visited));
    return null;
  }

  @Override
  public Void visit(NNPositionalEncodedLayer visited) {
    long[] shape = _dynamicConfigs.get(visited).getOutputShape();
    _graphBuilder.addVertex(_layerNames.get(visited), new PositionalEncodingVertex(shape[0], shape[1]),
        getParentNames(visited));
    return null;
  }

  @Override
  public Void visit(NNSelfAttentionLayer visited) {
    AttentionLayerConfig config = (AttentionLayerConfig) _dynamicConfigs.get(visited);
    _graphBuilder.addLayer(_layerNames.get(visited), new SelfAttentionLayer.Builder()
        .nIn(Math.toIntExact(_dynamicConfigs.get(visited.internalAPI().getInputLayer()).getOutputShape()[1]))
        .headSize(Math.toIntExact(config.getHeadSize()))
        .nHeads(Math.toIntExact(config.getHeadCount()))
        .nOut(Math.toIntExact(config.getOutputSize()))
        .projectInput(config.getIsProjected())
        .dropOut(dropoutValue(visited.getDropoutProbability()))
        .build(), getParentNames(visited));
    return null;
  }

  @Override
  public Void visit(NNRecurrentAttentionLayer visited) {
    AttentionLayerConfig config = (AttentionLayerConfig) _dynamicConfigs.get(visited);
    _graphBuilder.addLayer(_layerNames.get(visited), new RecurrentAttentionLayer.Builder()
        .nIn(Math.toIntExact(_dynamicConfigs.get(visited.internalAPI().getInputLayer()).getOutputShape()[1]))
        .headSize(Math.toIntExact(config.getHeadSize()))
        .nHeads(Math.toIntExact(config.getHeadCount()))
        .nOut(Math.toIntExact(config.getOutputSize()))
        .projectInput(config.getIsProjected())
        .activation(SameDiffLayerUtils.fromIActivation(visited.getActivationFunction().accept(ACTIVATION_CONVERTER)))
        .dropOut(dropoutValue(visited.getDropoutProbability()))
        .build(), getParentNames(visited));
    return null;
  }

  @Override
  public Void visit(NNLearnedSelfAttentionLayer visited) {
    AttentionLayerConfig config = (AttentionLayerConfig) _dynamicConfigs.get(visited);
    _graphBuilder.addLayer(_layerNames.get(visited), new LearnedSelfAttentionLayer.Builder()
        .nIn(Math.toIntExact(_dynamicConfigs.get(visited.internalAPI().getInputLayer()).getOutputShape()[1]))
        .headSize(Math.toIntExact(config.getHeadSize()))
        .nHeads(Math.toIntExact(config.getHeadCount()))
        .nOut(Math.toIntExact(config.getOutputSize()))
        .nQueries(Math.toIntExact(config.getQueryCount()))
        .projectInput(config.getIsProjected())
        .dropOut(dropoutValue(visited.getDropoutProbability()))
        .build(), getParentNames(visited));
    return null;
  }

  @Override
  public Void visit(NNSequentialDenseLayer visited) {
    _graphBuilder.addLayer(_layerNames.get(visited), new TimeDistributed(
        new DenseLayer.Builder().activation(visited.getActivation().accept(ACTIVATION_CONVERTER))
            .nIn(_dynamicConfigs.get(visited.internalAPI().getInputLayer()).getOutputShape()[1])
            .nOut(_dynamicConfigs.get(visited).getOutputShape()[1])
            .dropOut(dropoutValue(visited.getDropoutProbability()))
            //.hasLayerNorm(visited.getLayerNormalization())
            .build()), getParentNames(visited));
    return null;
  }

  @Override
  public Void visit(NNBatchNormalizedLayer visited) {
    long outputs = _dynamicConfigs.get(visited).getOutputElementCount();
    _graphBuilder.addLayer(_layerNames.get(visited),
        new BatchNormalization.Builder().nIn(outputs).nOut(outputs).decay(1 - visited.getGlobalDecayRate()).build(),
        getParentNames(visited));
    return null;
  }
}
