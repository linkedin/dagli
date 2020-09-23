package com.linkedin.dagli.examples.neuralnetwork;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG1x1;
import com.linkedin.dagli.data.dsv.DSVReader;
import com.linkedin.dagli.distribution.MostLikelyLabelFromDistribution;
import com.linkedin.dagli.dl4j.NeuralNetwork;
import com.linkedin.dagli.evaluation.MultinomialEvaluation;
import com.linkedin.dagli.evaluation.MultinomialEvaluationResult;
import com.linkedin.dagli.list.Size;
import com.linkedin.dagli.list.TruncatedList;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.nn.activation.Identity;
import com.linkedin.dagli.nn.activation.RectifiedLinear;
import com.linkedin.dagli.nn.layer.NNBatchNormalizedLayer;
import com.linkedin.dagli.nn.layer.NNChildLayer;
import com.linkedin.dagli.nn.layer.NNClassification;
import com.linkedin.dagli.nn.layer.NNLayer;
import com.linkedin.dagli.nn.layer.NNLinearizedVectorSequenceLayer;
import com.linkedin.dagli.nn.layer.NNMeanPoolingLayer;
import com.linkedin.dagli.nn.layer.NNPositionalEncodedLayer;
import com.linkedin.dagli.nn.layer.NNSelfAttentionLayer;
import com.linkedin.dagli.nn.layer.NNSequentialDenseLayer;
import com.linkedin.dagli.nn.layer.NNSequentialEmbeddingLayer;
import com.linkedin.dagli.nn.layer.NNSplitVectorSequenceLayer;
import com.linkedin.dagli.nn.layer.NNVectorSequenceSumLayer;
import com.linkedin.dagli.nn.layer.NonTerminalLayer;
import com.linkedin.dagli.nn.optimizer.AdaMax;
import com.linkedin.dagli.object.Indices;
import com.linkedin.dagli.object.OrderStatistic;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.objectio.SampleSegment;
import com.linkedin.dagli.text.LowerCased;
import com.linkedin.dagli.text.token.Tokens;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;


/**
 * This is an example of a neural network model that uses "pseudo-transformer-encoders" (DL4J lacks Layer Normalization
 * as of the time of this writing, so we can't exactly recreate a Transformer encoder) to learn to predict the character
 * (e.g. Caesar, Hamlet, etc.) from a line the character utters in a Shakespearean play.
 *
 * Transformer encoder "layers" combine multiheaded attention with dense layers applied to each timestep in the input
 * sequence.
 *
 * This model will achieve ~17% accuracy as currently parameterized.  There are many, many hyperparameters (including
 * the model architecture itself) that could be further tuned to improve this, but as with {@link CharLSTMExample} the
 * main issue is that we have too few examples for too many model parameters, and a more constrained approach (e.g. bag-
 * of-ngrams) would likely work better than a sequence model.
 *
 * IntelliJ users: you must have annotation processing enabled to compile this example in the IDE.  Please see
 * {@code dagli/documentation/structs.md} for instructions.
 */
public abstract class SelfAttentionExample {
  private SelfAttentionExample() { } // nobody should be creating instances of this class

  /**
   * Trains a model, evaluates it, and (optionally) saves it.  See ShakespeareCLI (in this directory) for code that
   * loads the saved model (by default, it loads a pre-trained version out of a resource file).
   *
   * If no arguments are provided, it reads the data from the /shakespeare.tsv resource file and prints out the
   * evaluation.  Otherwise, a path to write the model to may be given as an argument.  In a real application you'd
   * read your data from a normal file (or the network) rather than a resource file, of course.
   *
   * Here's a summary of what the program will do:
   * (1) Use the Dagli-bundled DSVReader to "read" our delimiter-separated value resource file.  In actuality, no data
   *     will be read until the model is being trained, and the data is streamed from disk, not fully read into RAM
   *     (this allows for training on very large datasets).
   * (2) Calls createDAG() to get the trainable model (a DAG).
   * (3) Trains the DAG.
   * (4) Evaluates the trained DAG.
   * (4) Serialized the trained DAG to disk.
   *
   * @param arguments empty array, or a single-element array with the path to which to write the model.  If this file
   *                  already exists the program will immediately terminate rather than overwrite it.
   */
  public static void main(String[] arguments) throws IOException, URISyntaxException {
    Configurator.setRootLevel(Level.INFO); // set the log level so we'll see progress updates as the NN trains

    // Get the model path, verify that it doesn't exist yet, and create the model file:
    final Path modelPath = modelPathFromArguments(arguments);

    // Now we'll create the DSVReader.  DSVReader reads "delimiter-separated value" files where each row is on
    // its own line and the columns are separated by a delimiter character, such as a tab or comma ("TSV" and "CSV"
    // files, respectively).  It also supports quote and escape characters as needed (leveraging Apache's
    // commons.csv.CSVFormat), and can be configured to handle almost any real-world DSV format.
    //
    // The format of each line of our Shakespeare data (in {@code resources/shakespeare.tsv}) is tab-delimited:
    // [Character name]\t[Line of dialog]
    //
    // CharacterDialog.Schema.builder() constructs the schema that tells DSVReader how to convert each row in the file
    // into a CharacterDialog object; we just need to tell it what the relevant column names are.  Ordinal column
    // indices are, of course, also supported, but names are preferred as they are more bug-resistant.
    final DSVReader<CharacterDialog> characterDialogData = new DSVReader<>()
        .withSchema(
          CharacterDialog.Schema.builder().setDialogColumnName("Dialog").setCharacterColumnName("Character").build())
        .withResourceFile("/shakespeare.tsv", StandardCharsets.UTF_8)
        .withFormat(CSVFormat.TDF.withQuote(null).withFirstRecordAsHeader());
        // ^ tab-delimited format without quoted values and the first row defining the header

    // Now we'd like to split our into training and evaluation data sets.  A convenient way to do that is using
    // "SampleSegments".  These randomly partition the space of examples in sets by assigning a random number between 0
    // and 1 to every example, and then figuring out if that number falls into the segment's range: if it is, it's part
    // of that segment, otherwise, it's not.  Sampling the same segment, with the same seed (here we use the default,
    // constant seed value), from the same data always yields the same sampled examples, which is important for the
    // repeatability of experiments.
    final SampleSegment trainingSegment = new SampleSegment(0.0, 0.8); // use 80% of the data for training
    final SampleSegment evaluationSegment = trainingSegment.complement(); // and the rest for evaluation

    // Neural networks are typically trained via some form of online gradient descent, which means that the parameters
    // are updated after each "minibatch" of training examples (rather than *all* examples, as done in batch training).
    // The practical consequence of this is that the ordering of the examples matters!  Specifically, we want it to be
    // random, which our source data most definitely is not (it's ordered by character).  ObjectReader::lazyShuffle(...)
    // provides an easy way to do this: by specifying a maximal buffer size (greater than the number of examples) we get
    // a full (but in-memory and thus RAM-heavy) shuffle of our training data, so long as there are fewer than
    // Integer.MAX_VALUE examples (which is the case here).
    ObjectReader<CharacterDialog> shuffledTrainingData =
        characterDialogData.sample(trainingSegment).lazyShuffle(Integer.MAX_VALUE);

    // We're ready to create our model (DAG) and train it.  By calling prepareAndApply(...), we obtain both our trained
    // (prepared) DAG for subsequent inference on new examples as well as the predictions made for the training data
    // itself (which will be part of our evaluation).
    DAG1x1.Result<CharacterDialog, String> result = createDAG().prepareAndApply(shuffledTrainingData);

    // Let's evaluate how well we're predicting labels for the *training* examples.  This will almost invariably be
    // better than how well we do on the evaluation data, and is **not at all** a reliable gauge of how well our model
    // will really perform on examples it hasn't seen before (AKA the real world), but is useful for, e.g. determining
    // whether the model is over- or underfitting.  For instance, if the "auto-evaluation" results are much better than
    // the results on the held-out evaluation data this likely indicates overfitting, in which case either more training
    // data, more regularization, or a less complex model would be warranted.
    MultinomialEvaluationResult autoevaluation = MultinomialEvaluation.evaluate(
        shuffledTrainingData.lazyMap(CharacterDialog::getCharacter), result);

    // Let's print it out:
    System.out.println("\nAutoevaluation of model predicting labels for its own training data:");
    System.out.println("---------------------------------------------------------------------");
    System.out.println(autoevaluation.getSummary() + "\n\n");

    // Assign the trained model to a local variable for convenience
    DAG1x1.Prepared<CharacterDialog, String> predictor = result.getPreparedDAG();

    // Next, we'll predict labels for our held-out evaluation data, so we can see how well the model will do on examples
    // that it hasn't seen during training.  First, we get the predictions:
    ObjectReader<String> predictedEvaluationCharacterNames =
        predictor.applyAll(characterDialogData.sample(evaluationSegment));
    // ^ note that applyAll(...) will also accept any other Iterable type, such as Lists.

    // Now we compute the evaluation:
    MultinomialEvaluationResult evaluation = MultinomialEvaluation.evaluate(
        characterDialogData.sample(evaluationSegment).lazyMap(CharacterDialog::getCharacter),
        predictedEvaluationCharacterNames);

    // And print out the evaluation we've calculated, just as before:
    System.out.println("\nEvaluation of model predicting labels for evaluation (unseen) data:");
    System.out.println("-------------------------------------------------------------------");
    System.out.println(evaluation.getSummary() + "\n");

    // Finally, we need to save the model for future inference.  DAGs are Java-serializable, so this is easy:
    if (modelPath != null) {
      try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(modelPath))) {
        oos.writeObject(predictor);
      }
      System.out.println("Saved model to " + modelPath.toString() + "\n");
    }

    // And we're done!  There are, of course, many ways to deploy your serialized model to production, but the easiest
    // it to just put it into a resource file and then load it up with Java deserialization.  See the ShakespeareCLI
    // class for an example of this.
  }

  /**
   * Gets the model path (if any) from the arguments, checks that it doesn't already exist, and creates the file that
   * will be written to.
   *
   * @param arguments the arguments passed to the program
   * @return a Path for the newly-created model file
   * @throws IOException if there is a problem creating the model file
   */
  private static Path modelPathFromArguments(String[] arguments) throws IOException {
    if (arguments.length == 0) {
      return null;
    } else {
      Path res = Paths.get(arguments[0]);
      if (Files.exists(res)) {
        System.out.println("The output path provided to save the model already exists: " + arguments[0]);
        System.out.println("For safety, this program will not overwrite an existing file.  Aborting.");
        System.exit(1);
      }
      Files.createFile(res); // create the file to make sure it's createable; we'll write to it later
      return res;
    }
  }

  /**
   * @return the classification model we'll train, expressed as a DAG
   */
  private static DAG1x1<CharacterDialog, String> createDAG() {
    final int vectorSize = 64; // must be divisible by attentionHeads
    final int attentionHeads = 1;

    // Define the "placeholder" of our DAG.  When the DAG is trained, the placeholder values will be provdied as inputs.
    // If you think of the DAG as consuming a list of "rows", where each row is an example, each placeholder is a
    //"column".  Here we use CharacterDialog.Placeholder, which derives from Placeholder<CharacterDialog> and adds
    // some convenience methods (like asDialog(), which--under the hood--creates a transformer that extracts the dialog
    // string from the placeholder's CharacterDialog value.)
    CharacterDialog.Placeholder example = new CharacterDialog.Placeholder();

    Tokens dialogTokens = new Tokens().withTextInput(new LowerCased().withInput(example.asDialog()));

    // Some lines of Shakespeare are outliers that are quite long, and these can affect our model's performance (both
    // in terms of computational time and learning ability); this is easily resolved by truncating the token list.
    // First, let's fine the number of the words:
    Size dialogSize = new Size().withInput(dialogTokens);

    // Then, let's find the 90th pecentile number of words to use as our limit
    OrderStatistic<Integer> dialogSize90Percentile =
        new OrderStatistic<Integer>().withValuesInput(dialogSize).withPercentile(0.9);

    // Finally, we use this 90th percentile size to truncate the lists of tokens
    TruncatedList<String> truncatedDialogTokens =
        new TruncatedList<String>().withMaxSizeInput(dialogSize90Percentile).withListInput(dialogTokens);

    // We also want to omit rare words for which we have few examples and increase our susceptibility to overfitting;
    // we can enforce this at the same time we convert our tokens into the indices we'll be feeding to the NN:
    Indices<String> tokenIndices = new Indices<String>().withMinimumFrequency(10).withInput(truncatedDialogTokens);

    // Now we start constructing the neural network, layer by layer.  The first layer will map our token indices into
    // (learned) embeddings:
    NNSequentialEmbeddingLayer sequenceEmbeddingLayer =
        new NNSequentialEmbeddingLayer().withEmbeddingSize(vectorSize).withInputFromNumberSequence(tokenIndices);

    // Positional encoding differentiates the embeddings by their location (so, e.g. the same token in the 1st and 5th
    // positions will correspond to related, but distinct, values).  This is done by adding a predefined vector of
    // values for each position to the corresponding token embedding.
    NNPositionalEncodedLayer positionalEncodingLayer = new NNPositionalEncodedLayer().withInput(sequenceEmbeddingLayer);

    // now we apply a sequence of (pseudo) Transformer encoder layers, five in total:
    NNChildLayer<List<DenseVector>, ? extends NonTerminalLayer> transformerLayer1 =
        pseudoTransformerEncoderLayer(vectorSize, attentionHeads, positionalEncodingLayer);

    NNChildLayer<List<DenseVector>, ? extends NonTerminalLayer> transformerLayer2 =
        pseudoTransformerEncoderLayer(vectorSize, attentionHeads, transformerLayer1);

    NNChildLayer<List<DenseVector>, ? extends NonTerminalLayer> transformerLayer3 =
        pseudoTransformerEncoderLayer(vectorSize, attentionHeads, transformerLayer2);

    NNChildLayer<List<DenseVector>, ? extends NonTerminalLayer> transformerLayer4 =
        pseudoTransformerEncoderLayer(vectorSize, attentionHeads, transformerLayer3);

    NNChildLayer<List<DenseVector>, ? extends NonTerminalLayer> transformerLayer5 =
        pseudoTransformerEncoderLayer(vectorSize, attentionHeads, transformerLayer4);

    // This leaves us with a sequence of embeddings, one for each position.  Average them together to get a single
    // vector representation we can use to do the final classification.
    NNMeanPoolingLayer pooling = new NNMeanPoolingLayer().withInput(transformerLayer5);

    // We're ready to classify; NNClassification is actually a "loss layer", which means it's output will be optimized
    // (along with those of any other loss layers) by the neural network during training.
    NNClassification<String> characterClassification =
        new NNClassification<String>().withFeaturesInput(pooling).withMultinomialLabelInput(example.asCharacter());

    // Now we wrap everything up into a neural network, with the characterClassification layer as the sole loss layer
    // (and output) of the neural network.
    NeuralNetwork neuralNetwork = new NeuralNetwork()
        .withLossLayers(characterClassification)
        .withMinibatchSize(128) // larger minibatches allow for more examples/sec but may reduce the rate at which
                                // parameters are updated (per example)
        .withDropoutProbability(0.5) // dropout mitigates overfitting (or, at least, delays it!)
        .withTrainingProgressLoggingFrequency(1) // log the current training status each epoch
        .withEvaluationHoldoutProportion(0.2, 1337) // used to identify the best model across epochs
        .withMaxTrainingAmountWithoutImprovement(10) // criteria for early stopping
        .withMaxEpochs(500) // we'll stop well before this
        .withInteractiveCommands(true) // you can type "stop" to stop training at any time
        .withTrainingModelArchitectureLogging(true) // log the model's architecture just before training begins
        .withOptimizer(new AdaMax());

    // The classification can be obtained as a discrete distribution via
    // neuralNetwork.asLayerOutput(characterClassification), but we only care about the most likely label:
    MostLikelyLabelFromDistribution<String> predictedCharacter =
        new MostLikelyLabelFromDistribution<String>().withInput(neuralNetwork.asLayerOutput(characterClassification));

    // Build and return the DAG:
    return DAG.withPlaceholder(example).withOutput(predictedCharacter);
  }

  /**
   * Utility function that creates something similar to a Transformer Encoder "layer".  Unfortunately, DL4J does not
   * currently support Layer Normalization, so we're forced to substitute batch normalization (which normalizes each
   * vector element against all the corresponding elements in the minibatch, rather than across the other elements in
   * the same example).
   *
   * The encoder formulation otherwise follows that provided by TensorFlow's
   * <a href="https://www.tensorflow.org/tutorials/text/transformer">excellent Transformer tutorial</a>, with the
   * exception of some other, less importance differences:
   * (1) DL4J's implementation of self-attention will use an output projection matrix
   * (2) To apply batch normalization, we need to linearize the input sequence, which loses masking information and
   *     allows all timesteps (including those corresponding to non-existent tokens) to participate in attention and the
   *     dense layers.
   *
   * @param vectorSize the size of vectors in both the input and output sequences at each timestep
   * @param attentionHeads must evenly divide outputVectorSize
   * @param input an input sequence to normalize
   * @return the last layer of the layers comprising the transformer encoder "layer"
   */
  private static NNChildLayer<List<DenseVector>, ? extends NonTerminalLayer> pseudoTransformerEncoderLayer(
      long vectorSize, long attentionHeads, NNLayer<List<DenseVector>, ? extends NonTerminalLayer> input) {
    // create a self-attention layer
    NNSelfAttentionLayer attentionLayer = new NNSelfAttentionLayer()
        .withHeadCount(attentionHeads)
        .withHeadSize(vectorSize / attentionHeads)
        .withProjection(true)
        .withInput(input);

    // sum the attended outputs and the original input
    NNVectorSequenceSumLayer sum1 = new NNVectorSequenceSumLayer().withInputs(attentionLayer, input);

    // batch-normalize it (since we don't have Layer Normalization available)
    NNChildLayer<List<DenseVector>, ? extends NonTerminalLayer> batchNormalized1 =
        batchNormalizedSequence(vectorSize, sum1);

    // apply a per-timestep dense layer with a ReLU activation followed by a purely linear layer
    NNSequentialDenseLayer sequentialFeedForwardLayer1 =
        new NNSequentialDenseLayer().withActivationFunction(new RectifiedLinear()).withInput(batchNormalized1);
    NNSequentialDenseLayer sequentialFeedForwardLayer2 =
        new NNSequentialDenseLayer().withActivationFunction(new Identity())
            .withInput(sequentialFeedForwardLayer1)
            .withDropoutProbability(0); // dropout for first FF layer only

    // sum the post-dense-layer outputs and the sequence we fed into them
    NNVectorSequenceSumLayer sum2 =
        new NNVectorSequenceSumLayer().withInputs(sequentialFeedForwardLayer2, batchNormalized1);

    // return the batch-normalizeD final result:
    return batchNormalizedSequence(vectorSize, sum2);
  }

  /**
   * Batch normalization isn't intended for sequences, but we linearize our sequence, batch normalize it, and then
   * convert it back to a sequence.
   *
   * @param vectorSize the size of the vectors in both the input and output sequences
   * @param input the input sequence to be batch-normalized
   * @return a layer that will output the batch-normalized sequence
   */
  private static NNChildLayer<List<DenseVector>, ? extends NonTerminalLayer> batchNormalizedSequence(
      long vectorSize, NNLayer<List<DenseVector>, ? extends NonTerminalLayer> input) {
    // batch-normalized the (linearized) input sequence
    NNBatchNormalizedLayer batchNormalized =
        new NNBatchNormalizedLayer().withInput(new NNLinearizedVectorSequenceLayer().withInput(input));
    // convert batch-normalized activations back to a sequence
    return new NNSplitVectorSequenceLayer().withSplitSize(vectorSize).withInput(batchNormalized);
  }
}
