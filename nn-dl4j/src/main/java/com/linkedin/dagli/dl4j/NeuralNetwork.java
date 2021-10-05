package com.linkedin.dagli.dl4j;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.nn.AbstractNeuralNetwork;
import com.linkedin.dagli.nn.TrainingUnit;
import com.linkedin.dagli.nn.interactive.commands.InteractiveCommand;
import com.linkedin.dagli.nn.interactive.commands.InteractiveCommandVisitor;
import com.linkedin.dagli.nn.interactive.commands.Stop;
import com.linkedin.dagli.nn.layer.DynamicLayerConfig;
import com.linkedin.dagli.nn.layer.LayerHandle;
import com.linkedin.dagli.nn.layer.NNLayer;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerDynamic;
import com.linkedin.dagli.preparer.PreparerMode;
import com.linkedin.dagli.transformer.DynamicInputs;
import com.linkedin.dagli.util.collection.Maps;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.optimize.listeners.PerformanceListener;
import org.nd4j.linalg.dataset.api.iterator.MultiDataSetIterator;


/**
 * An implementation of a Dagli neural network, backed by the Deep Learning for Java (DL4J) library.
 *
 * Direct modification to some of the DL4J internals (e.g. to add a custom listener) can be accomplished by extending
 * this class to override the {@link #getPreparer(PreparerContext)} method to return a new preparer extending
 * {@link Preparer} and overriding one or more of the "on___()" methods called during configuration
 * and training.  This is much easier to accomplish than it probably sounds like.
 */
@ValueEquality
public class NeuralNetwork
    extends AbstractNeuralNetwork<DL4JResult, NeuralNetwork.Prepared, NeuralNetwork> {
  private static final long serialVersionUID = 1;

  @Override
  protected PreparerDynamic<DL4JResult, Prepared> getPreparer(PreparerContext context) {
    return new Preparer(this);
  }

  protected static class Preparer extends AbstractNeuralNetwork.Preparer<DL4JResult, Prepared, NeuralNetwork> {
    private final static Logger LOGGER = LogManager.getLogger();

    private ComputationGraph _computationGraph; // set during initialization
    private AbstractInputConverter<?, ?>[] _inputAccesors; // set during initialization
    private AbstractInputConverter<?, ?>[] _labelAccesors; // set during initialization

    private final LossAccumulationListener _lossAccumulationListener = new LossAccumulationListener();

    /**
     * Called when the {@link NeuralNetConfiguration.Builder} has been configured, before the graph builder is created
     * to add nodes to the computation graph.  This gives a derived class the chance to change the configuration if
     * desired.  The default implementation simply returns the original builder.
     *
     * @param layerToDynamicConfigMap a {@link Map} from each layer in the neural network to its
     *                                {@link DynamicLayerConfig}
     * @param constantInputs provides type-safe access to the constant-result inputs provided to the neural network
     * @param builder the neural network configuration builder; may be modified by this method
     * @return the neural network configuration builder that the neural network should use
     */
    protected NeuralNetConfiguration.Builder onConfigurationBuilderReady(
        HashMap<NNLayer<?, ?>, DynamicLayerConfig> layerToDynamicConfigMap, DynamicInputs.ConstantInputs constantInputs,
        NeuralNetConfiguration.Builder builder) {
      return builder;
    }

    /**
     * Called when the {@link ComputationGraphConfiguration.GraphBuilder} has been configured, with all layers added,
     * immediately before it is built and used to construct the {@link ComputationGraph}.  This gives a derived class the
     * chance to change the configuration if desired.  The default implementation simply returns the original builder.
     *
     * @param layerToDynamicConfigMap a {@link Map} from each layer in the neural network to its
     *                                {@link DynamicLayerConfig}
     * @param constantInputs provides type-safe access to the constant-result inputs provided to the neural network
     * @param builder the neural network graph configuration builder; may be modified by this method
     * @return the neural network graph configuration builder that the neural network should use
     */
    protected ComputationGraphConfiguration.GraphBuilder onGraphBuilderReady(
        HashMap<NNLayer<?, ?>, DynamicLayerConfig> layerToDynamicConfigMap, DynamicInputs.ConstantInputs constantInputs,
        ComputationGraphConfiguration.GraphBuilder builder) {
      return builder;
    }

    /**
     * Called when the {@link ComputationGraph} has been created, initialized, and is ready to train.  This method gives
     * a derived class the opportunity to change or replace the computation graph if desired.  The default
     * implementation simply returns the original graph.
     *
     * @param computationGraph the computation graph; may be modified by this method
     * @return the computation graph that the neural network should use
     */
    protected ComputationGraph onComputationGraphReady(ComputationGraph computationGraph) {
      return computationGraph;
    }

    /**
     * Called after each training epoch.  This method gives the derived class the opportunity to, for example, output
     * highly detailed logging of the network's parameters or even permute them if desired.
     *
     * The default implementation is a no-op.
     *
     * @param computationGraph the computation graph; may be modified by this method
     */
    protected void onTrainingEpoch(ComputationGraph computationGraph) { }

    /**
     * Called when training is complete.  This method provides a final opportunity to modify the (trained) computation
     * graph or log any salient information.
     *
     * The default implementation is a no-op.
     *
     * @param computationGraph the computation graph; may be modified by this method
     */
    protected void onTrainingComplete(ComputationGraph computationGraph) { }

    @Override
    protected void initialize(HashMap<NNLayer<?, ?>, DynamicLayerConfig> layerToDynamicConfigMap, DynamicInputs context,
        DynamicInputs.ConstantInputs constantInputs) {

      // DL4J seems to (undocumentedly) treat a seed of 0 as meaning "no predefined seed", so replace 0 with a different
      // (arbitrary) seed value when passing it to the network.
      long seed = getNeuralNetwork().getRandomSeed();
      seed = seed == 0 ? 0x96c0fa7af64a0bf1L : seed;

      // create a DL4J graph builder and do the initial configuration
      NeuralNetConfiguration.Builder nnBuilder = new NeuralNetConfiguration.Builder()
          .dataType(DL4JUtil.toDataType(getNeuralNetwork().getFloatingPointPrecision()))
          .updater(getNeuralNetwork().getOptimizer().accept(new OptimizerConverterVisitor()))
          .seed(seed);

      nnBuilder = onConfigurationBuilderReady(layerToDynamicConfigMap, constantInputs, nnBuilder);

      ComputationGraphConfiguration.GraphBuilder builder = nnBuilder.graphBuilder()
          .addInputs(getNeuralNetwork().getRootLayers()
              .stream()
              .map(getNeuralNetwork().getLayerNames()::get)
              .toArray(String[]::new))
          .setOutputs(getNeuralNetwork().getLossLayers()
              .stream()
              .map(getNeuralNetwork().getLayerNames()::get)
              .toArray(String[]::new));

      // walk through all the layers and add them to the builder
      NetworkBuilderLayerVisitor graphBuildingVisitor =
          new NetworkBuilderLayerVisitor(getNeuralNetwork(), builder, getNeuralNetwork().getLayerNames(),
              layerToDynamicConfigMap);
      getNeuralNetwork().getTopographicallySortedLayers().forEach(layer -> layer.accept(graphBuildingVisitor));

      builder = onGraphBuilderReady(layerToDynamicConfigMap, constantInputs, builder);

      _computationGraph = new ComputationGraph(builder.build());

      InputConverterLayerVisitor inputCreatorVisitor =
          new InputConverterLayerVisitor(context, layerToDynamicConfigMap, getNeuralNetwork().getFloatingPointPrecision());

      _inputAccesors = getNeuralNetwork().getRootLayers()
          .stream()
          .map(layer -> (AbstractInputConverter<?, ?>) layer.accept(inputCreatorVisitor))
          .toArray(AbstractInputConverter[]::new);
      _labelAccesors = getNeuralNetwork().getLossLayers()
          .stream()
          .map(layer -> (AbstractInputConverter<?, ?>) layer.accept(inputCreatorVisitor))
          .toArray(AbstractInputConverter[]::new);
    }

    @Override
    protected void processTrainingExample(Object[] values) {
      // no-op
    }

    @Override
    protected void processEvaluationExample(Object[] values) {
      // no-op
    }

    private void initializeGraph(TrainingUnit.Context trainingContext) {
      if (getNeuralNetwork().getTrainingParametersLoggingFrequency().isFinite()) {
        _computationGraph.addListeners(new ModelListener(Math.toIntExact(
            getNeuralNetwork().getTrainingParametersLoggingFrequency()
                .getAsLong(TrainingUnit.MINIBATCHES, trainingContext))));
      }
      if (getNeuralNetwork().getTrainingSampledActivationsLoggingFrequency().isFinite()) {
        _computationGraph.addListeners(new FFActivationListener(Math.toIntExact(
            getNeuralNetwork().getTrainingSampledActivationsLoggingFrequency()
                .getAsLong(TrainingUnit.MINIBATCHES, trainingContext)), 3));
      }
      if (getNeuralNetwork().getTrainingPerformanceLoggingFrequency().isFinite()) {
        _computationGraph.addListeners(new PerformanceListener(Math.toIntExact(
            getNeuralNetwork().getTrainingPerformanceLoggingFrequency()
                .getAsLong(TrainingUnit.MINIBATCHES, trainingContext)), false, true));
      }
      if (getNeuralNetwork().getTrainingProgressLoggingFrequency().isFinite()) {
        _computationGraph.addListeners(
            new ProgressListener(trainingContext.getExamplesPerEpoch(), getNeuralNetwork().getMinibatchSize(),
                getNeuralNetwork().getMaxEpochs(), Math.toIntExact(
                getNeuralNetwork().getTrainingProgressLoggingFrequency()
                    .getAsLong(TrainingUnit.MINIBATCHES, trainingContext))));
      }

      _computationGraph.addListeners(_lossAccumulationListener);

      _computationGraph.init();
      _computationGraph =
          onComputationGraphReady(_computationGraph);
    }

    @Override
    protected Prepared finish(ObjectReader<Object[]> trainingExampleReader,
        ObjectReader<Object[]> evaluationExampleReader) {
      TrainingUnit.Context trainingContext =
          new TrainingUnit.Context(getNeuralNetwork().getMinibatchSize(), getTrainingExampleCount());

      initializeGraph(trainingContext);

      if (getNeuralNetwork().getTrainingModelArchitectureLogging()) {
        LOGGER.info("Model Architecture Summary: \n" + _computationGraph.summary());
      }

      MinibatchingMultiDataSetIterator trainingDataIterator =
          new MinibatchingMultiDataSetIterator(trainingExampleReader, getNeuralNetwork().getMinibatchSize(),
              _inputAccesors, _labelAccesors);
      MultiDataSetIterator scoringDataIterator = trainingDataIterator;

      if (getEvaluationExampleCount() > 0) {
        // add a listener to log the performance on the evaluation data
        scoringDataIterator =
            new MinibatchingMultiDataSetIterator(evaluationExampleReader, getNeuralNetwork().getMinibatchSize(),
                _inputAccesors, _labelAccesors);
      }

      ComputationGraph bestModel = _computationGraph;
      double bestLoss = Double.MAX_VALUE;
      long epochsWithoutImprovement = 0;
      final long maxEpochsWithoutImprovement =
          getNeuralNetwork().getMaxTrainingAmountWithoutImprovement().getAsLong(TrainingUnit.EPOCHS, trainingContext);

      long endTimeInMillseconds =
          getNeuralNetwork().getMaxTrainingTimeInSeconds() < Long.MAX_VALUE ? System.currentTimeMillis() + (
              getNeuralNetwork().getMaxTrainingTimeInSeconds() * 1000) : Long.MAX_VALUE;

      final long evaluationEpochFrequency =
          getNeuralNetwork().getEvaluationFrequency().getAsLong(TrainingUnit.EPOCHS, trainingContext);
      final long maxEpochs = getNeuralNetwork().getMaxEpochs();
      for (long i = 0; i < maxEpochs; i++) {
        _computationGraph.fit(trainingDataIterator);
        onTrainingEpoch(_computationGraph);

        double epochLoss = _lossAccumulationListener.getAndClearLoss() / getTrainingExampleCount();

        if (i > 0 && i % evaluationEpochFrequency == 0) {
          if (getEvaluationExampleCount() > 0) {
            scoringDataIterator.reset();
            double[] evalLoss = new double[] { 0 };
            scoringDataIterator.forEachRemaining(
                dataSet -> evalLoss[0] += _computationGraph.score(dataSet) * _computationGraph.batchSize());
            epochLoss = evalLoss[0] / getEvaluationExampleCount();
          }

          LOGGER.info(epochLoss + " loss/example @ epoch " + (i + 1) + " on " + (getEvaluationExampleCount() > 0 ? (
              getEvaluationExampleCount() + " evaluation") : (getTrainingExampleCount() + " training")) + " examples");

          if (epochLoss < bestLoss) {
            LOGGER.info("New best model @ epoch " + i + " (new loss: " + epochLoss + "; old loss: " + bestLoss + ")");
            bestLoss = epochLoss;
            bestModel = _computationGraph.clone();
            epochsWithoutImprovement = 0;
          } else {
            epochsWithoutImprovement += evaluationEpochFrequency;
            if (epochsWithoutImprovement > maxEpochsWithoutImprovement) {
              LOGGER.info("Training terminated because of " + epochsWithoutImprovement
                  + " epochs without improvement (the configured limit is " + maxEpochsWithoutImprovement + ")");
              break;
            }
          }
        }

        if (System.currentTimeMillis() > endTimeInMillseconds) {
          LOGGER.info(
              "Training terminated because max training time of " + getNeuralNetwork().getMaxTrainingTimeInSeconds()
                  + " seconds was exceeded");
          break;
        }

        // Check for pending interactive commands
        boolean[] shouldStop = new boolean[] { false };
        InteractiveCommand next;
        while ((next = getPendingCommands().poll()) != null) {
           next.accept(new InteractiveCommandVisitor<Void>() {
             @Override
             public Void visit(Stop visited) {
               LOGGER.info("Training terminated because of interactive stop command.");
               shouldStop[0] = true;
               return null;
             }
           });
        }
        if (shouldStop[0]) {
          break;
        }
      }

      // use whatever our best model was
      _computationGraph = bestModel;
      onTrainingComplete(_computationGraph);

      // clear out the listeners from the graph--we're done training
      _computationGraph.setListeners(Collections.emptyList());

      return new Prepared(_computationGraph, getNeuralNetwork().getMinibatchSizeForInference(), _inputAccesors,
          getNeuralNetwork().getOutputLayers()
              .stream()
              .map(getNeuralNetwork().getLayerNames()::get)
              .collect(Collectors.toList()), Maps.replaceKeys(getNeuralNetwork().getLayerNames(), NNLayer::getHandle));
    }

    Preparer(NeuralNetwork neuralNetwork) {
      super(neuralNetwork);
    }

    @Override
    public final PreparerMode getMode() {
      return PreparerMode.BATCH;
    }
  }

  @ValueEquality
  static class Prepared extends AbstractCustomNeuralNetwork.AbstractPrepared<Prepared>
      implements AbstractNeuralNetwork.Prepared<DL4JResult, Prepared> {
    private static final long serialVersionUID = 1;

    private final Map<LayerHandle<?>, String> _layerNames;

    protected Prepared(ComputationGraph computationGraph, int preferredMinibatchSize,
        AbstractInputConverter<?, ?>[] inputConverters, List<String> outputs, Map<LayerHandle<?>, String> layerNames) {
      super(computationGraph, preferredMinibatchSize, inputConverters, outputs);
      _layerNames = layerNames;
    }

    @Override
    public Map<String, INDArrayAsMDArray> getParameters(LayerHandle<?> layerHandle) {
      return getParametersForLayerName(_layerNames.get(layerHandle));
    }

    @Override
    public ComputationGraph getComputationGraph() {
      return super.getComputationGraph();
    }
  }
}
