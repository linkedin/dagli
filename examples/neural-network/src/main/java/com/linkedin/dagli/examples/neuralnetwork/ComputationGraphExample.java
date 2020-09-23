package com.linkedin.dagli.examples.neuralnetwork;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG1x1;
import com.linkedin.dagli.data.dsv.DSVReader;
import com.linkedin.dagli.distribution.DistributionFromVector;
import com.linkedin.dagli.distribution.MostLikelyLabelFromDistribution;
import com.linkedin.dagli.dl4j.CustomNeuralNetwork;
import com.linkedin.dagli.evaluation.MultinomialEvaluation;
import com.linkedin.dagli.evaluation.MultinomialEvaluationResult;
import com.linkedin.dagli.list.TruncatedList;
import com.linkedin.dagli.object.Index;
import com.linkedin.dagli.object.Indices;
import com.linkedin.dagli.object.UnknownItemPolicy;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.objectio.SampleSegment;
import com.linkedin.dagli.text.LowerCased;
import com.linkedin.dagli.text.token.Tokens;
import com.linkedin.dagli.vector.ManyHotVector;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.csv.CSVFormat;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.EmbeddingSequenceLayer;
import org.deeplearning4j.nn.conf.layers.GlobalPoolingLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.PoolingType;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.optimize.listeners.PerformanceListener;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.learning.config.AdaMax;


/**
 * This is an example of a neural network model that uses learned bag-of-ngram embeddings to learn to predict the
 * character (e.g. Caesar, Hamlet, etc.) from a line the character utters in a Shakespearean play.
 *
 * The {@link com.linkedin.dagli.dl4j.CustomNeuralNetwork} in this example allows for any neural network supported by
 * DL4J (expressed as a {@link org.deeplearning4j.nn.graph.ComputationGraph}) to be used, which is primarily helpful
 * when Dagli's {@link com.linkedin.dagli.nn.layer.NNLayer} abstraction does not (yet) accommodate your neural network
 * architecture.
 *
 * IntelliJ users: you must have annotation processing enabled to compile this example in the IDE.  Please see
 * {@code dagli/documentation/structs.md} for instructions.
 */
public abstract class ComputationGraphExample {
  private ComputationGraphExample() { } // nobody should be creating instances of this class

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
    // We need to define several constants to help us construct our computation graph/neural network.  If we were more
    // ambitious we could calculate these from the data.
    final int maxTokenEmbeddings = 10000; // only the K globally most common tokens are used; the rest will be ignored
    final int maxLabels = 50; // the maximum number of labels (the actual number of labels is actually about half this)
    final int maxTokenLength = 50; // the maximum number of tokens to keep per example (few lines are longer than this)

    // Define the "placeholder" of our DAG.  When the DAG is trained, the placeholder values will be provdied as inputs.
    // If you think of the DAG as consuming a list of "rows", where each row is an example, each placeholder is a
    //"column".  Here we use CharacterDialog.Placeholder, which derives from Placeholder<CharacterDialog> and adds
    // some convenience methods (like asDialog(), which--under the hood--creates a transformer that extracts the dialog
    // string from the placeholder's CharacterDialog value.)
    CharacterDialog.Placeholder example = new CharacterDialog.Placeholder();

    // Get the tokenization of the (lower-cased) dialog line
    Tokens dialogTokens = new Tokens().withTextInput(new LowerCased().withInput(example.asDialog()));

    // To embed our tokens, we need to assign them consecutive integers from 0...[total number of unigrams - 1].
    // Only the [maxTokenEmbeddings] globally most common unigrams are included; the rest are ignored.  The result of
    // this transformer is a list of the (non-ignored) unigram indices corresponding to the dialog.
    Indices<String> tokenIndices = new Indices<String>().withMinimumFrequency(5)
        .withMaxUniqueObjects(maxTokenEmbeddings)
        .withInput(dialogTokens)
        .withUnknownItemPolicy(UnknownItemPolicy.IGNORE);

    // The list of token indices could have any arbitrary length, so we truncate it if the list is too long:
    TruncatedList<Integer> truncatedTokenIndices =
        new TruncatedList<Integer>().withListInput(tokenIndices).withMaxSize(maxTokenLength);

    // Because we're using the ComputationGraph directly, we need to assign each unique multinomial label to a
    // [0...#labels - 1] integer.
    Index<String> labelIndex = new Index<String>()
        .withMaxUniqueObjects(maxLabels)
        .withUnknownItemPolicy(UnknownItemPolicy.LEAST_FREQUENT)
        .withInput(example.asCharacter());

    // Create the DL4J computation graph configuration:
    ComputationGraphConfiguration conf = new NeuralNetConfiguration.Builder()
        .updater(new AdaMax())
        .seed(12345) // specifying a seed helps ensure consistency between runs
        .graphBuilder()
        .addInputs("tokenIndices")
        .addLayer("embeddings", new EmbeddingSequenceLayer.Builder().nIn(maxTokenEmbeddings).units(64).build(),
            "tokenIndices")
        .addLayer("maxPooled", new GlobalPoolingLayer.Builder().poolingType(PoolingType.MAX).build(), "embeddings")
        .addLayer("dense1", new DenseLayer.Builder().units(64).nIn(64).build(), "maxPooled")
        .addLayer("dense2", new DenseLayer.Builder().units(64).nIn(64).build(), "dense1")
        .addLayer("classification", new OutputLayer.Builder().nIn(64).nOut(maxLabels).build(), "dense2")
        .setOutputs("classification")
        .build();

    // Next, create the computation graph itself and add a listener so we can log the training progress:
    ComputationGraph graph = new ComputationGraph(conf);
    graph.addListeners(new PerformanceListener(1000, true));

    // Create the neural network from our computation graph, specifying its feature and label inputs.
    //
    // In reality, the data type we use for the tokenIndices input *should* be INT32, not INT64.  However, when INT32 is
    // used, the current version of DL4J has a bug that causes data to be read past the end of the final minibatch in an
    // epoch when that minibatch has fewer examples than others.  The workaround we adopt is to use INT64, which seems
    // to force DL4J to create a (properly sized) copy of the original data.  This is obviously less efficient than
    // ideal but should have minimal practical impact.
    CustomNeuralNetwork neuralNetwork = new CustomNeuralNetwork().withComputationGraph(graph)
        .withFeaturesInputFromNumberSequence("tokenIndices", tokenIndices, maxTokenLength, DataType.INT64)
        .withLabelInputFromVector("classification", new ManyHotVector().withNumbersAsInput(labelIndex), maxLabels,
            DataType.FLOAT)
        .withMaxEpochs(50);

    // The output of our classification layer will be a vector of probabilities, which needs to be converted to a
    // distribution over the labels:
    DistributionFromVector<String> distribution = new DistributionFromVector<String>()
        .withIndexToLabelMapInput(labelIndex.asLongIndexToObjectMap())
        .withMissingLabelsIgnored()
        .withVectorInput(neuralNetwork.asLayerOutputVector("classification"));

    // The classification can be obtained as a discrete distribution via
    // neuralNetwork.asLayerOutput(characterClassification), but we only care about the most likely label:
    MostLikelyLabelFromDistribution<String> predictedCharacter =
        new MostLikelyLabelFromDistribution<String>().withInput(distribution);

    // Build and return the DAG:
    return DAG.withPlaceholder(example).withOutput(predictedCharacter);
  }
}
