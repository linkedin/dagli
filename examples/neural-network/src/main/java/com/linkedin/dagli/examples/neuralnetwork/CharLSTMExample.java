package com.linkedin.dagli.examples.neuralnetwork;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG1x1;
import com.linkedin.dagli.data.dsv.DSVReader;
import com.linkedin.dagli.distribution.MostLikelyLabelFromDistribution;
import com.linkedin.dagli.dl4j.NeuralNetwork;
import com.linkedin.dagli.evaluation.MultinomialEvaluation;
import com.linkedin.dagli.evaluation.MultinomialEvaluationResult;
import com.linkedin.dagli.function.FunctionResult1;
import com.linkedin.dagli.list.Size;
import com.linkedin.dagli.list.TruncatedList;
import com.linkedin.dagli.nn.TrainingAmount;
import com.linkedin.dagli.nn.layer.Bidirectionality;
import com.linkedin.dagli.nn.layer.NNClassification;
import com.linkedin.dagli.nn.layer.NNDenseLayer;
import com.linkedin.dagli.nn.layer.NNLSTMLayer;
import com.linkedin.dagli.nn.layer.NNLastVectorInSequenceLayer;
import com.linkedin.dagli.nn.layer.NNSequentialEmbeddingLayer;
import com.linkedin.dagli.nn.optimizer.AdaGrad;
import com.linkedin.dagli.object.OrderStatistic;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.objectio.SampleSegment;
import com.linkedin.dagli.util.array.ArraysEx;
import it.unimi.dsi.fastutil.chars.CharArrayList;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.csv.CSVFormat;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;


/**
 * This is an example of a neural network model (specifically, a neural network with an LSTM layer over chars [letters
 * and punctuation]) that learns to predict the character (e.g. Caesar, Hamlet, etc.) from a line the character utters
 * in a Shakespearean play.  To reduce confusion, we will refer to the letter and punctuation processed by the LSTM
 * layer as "chars", distinct from the Shakespearean character names we are predicting.
 *
 * Note that this model takes a long time (2+ hours) to train using the naive CPU ND4J libraries specified in our
 * build.gradle file.  Switching to the <a href="http://deeplearning4j.org/cpu">AVX-2</a> (supported by virtually all
 * modern x86 CPUs) or <a href="https://deeplearning4j.konduit.ai/config/backends/config-cudnn">GPU</a> variants may
 * dramatically improve this (you may want to also tune the minibatch size).
 *
 * Also, it's worth mentioning that this model is actually not very good!  Other examples that solve this same
 * classification problem can achieve up to 20% accuracy (or higher, with diligent tuning).  This model will achieve
 * 15-16% accuracy, which is not that much better we'd do just choosing the most popular character.  We surmise this is
 * likely due to simpler, bag-of-ngram models having an advantage in being able to test for certain keywords that
 * strongly hint at the speaker (e.g. "capulet"), whereas our network overfits long before being able to pick up on
 * these simple rules.  Put another way, this model is probably not a very good choice given how limited the available
 * training data is.
 *
 * IntelliJ users: you must have annotation processing enabled to compile this example in the IDE.  Please see
 * {@code dagli/documentation/structs.md} for instructions.
 */
public abstract class CharLSTMExample {
  private CharLSTMExample() { } // nobody should be creating instances of this class

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
  public static void main(String[] arguments) throws IOException {
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
    // Define the "placeholder" of our DAG.  When the DAG is trained, the placeholder values will be provdied as inputs.
    // If you think of the DAG as consuming a list of "rows", where each row is an example, each placeholder is a
    //"column".  Here we use CharacterDialog.Placeholder, which derives from Placeholder<CharacterDialog> and adds
    // some convenience methods (like asDialog(), which--under the hood--creates a transformer that extracts the dialog
    // string from the placeholder's CharacterDialog value.)
    CharacterDialog.Placeholder example = new CharacterDialog.Placeholder();

     // Extract the chars of the dialog as a list
    FunctionResult1<String, CharArrayList> dialogChars =
        new FunctionResult1<>(String::toCharArray).andThen(ArraysEx::asList).withInput(example.asDialog());

    // Some lines of Shakespeare are outliers that are quite long, and these can affect our LSTM's performance (both
    // in terms of computational time and learning ability); this is easily resolved by truncating the token list.
    // First, let's fine the number of the characters:
    Size dialogSize = new Size().withInput(dialogChars);

    // Then, let's find the 90th pecentile number of characters to use as our limit
    OrderStatistic<Integer> dialogSize90Percentile =
        new OrderStatistic<Integer>().withValuesInput(dialogSize).withPercentile(0.9);

    // Finally, we use this 90th percentile size to truncate the list of dialog chars
    TruncatedList<Character> truncatedDialogChars =
        new TruncatedList<Character>().withMaxSizeInput(dialogSize90Percentile).withListInput(dialogChars);


    // Now we start constructing the neural network, layer by layer, starting with a layer that embeds our sequence of
    // tokens.  Providing our tokens to withInputFromSequence(...) means that each unique token will map to a unique
    // embedding (the embeddings are not, however, position-dependent).  The output of the layer is a sequence of
    // embeddings.
    NNSequentialEmbeddingLayer sequenceEmbeddingLayer =
        new NNSequentialEmbeddingLayer().withEmbeddingSize(32).withInputFromSequence(truncatedDialogChars);

    // The ordering of the tokens (and their embeddings) is, of course, important.  We can capture this with two LSTM
    // layers: one forward, one backward.  An explanation of LSTMs is beyond the scope of this example, but please see
    // https://en.wikipedia.org/wiki/Long_short-term_memory to learn more.
    NNLSTMLayer lstmLayer = new NNLSTMLayer()
        .withUnitCount(32)
        .withInput(sequenceEmbeddingLayer)
        .withBidirectionality(Bidirectionality.CONCATENATED).stack(32, 32);

    // Ultimately we want to use a softmax layer to provide a classification; to do this, we need to get a (single)
    // vector that represents the entire sequence.  One way to do this through pooling:
    NNLastVectorInSequenceLayer poolingLayer = new NNLastVectorInSequenceLayer().withInput(lstmLayer);

    NNDenseLayer denseLayers = new NNDenseLayer().withInput(poolingLayer).stack(64, 64);

    // We're ready to classify; NNClassification is actually a "loss layer", which means it's output will be optimized
    // (along with those of any other loss layers) by the neural network during training.
    NNClassification<String> characterClassification =
        new NNClassification<String>().withFeaturesInput(denseLayers)
            .withMultinomialLabelInput(example.asCharacter());

    // Now we wrap everything up into a neural network, with the characterClassification layer acting as the sole
    // loss layer (and output) of the neural network.
    NeuralNetwork neuralNetwork = new NeuralNetwork()
        .withLossLayers(characterClassification)
        .withMinibatchSize(128) // larger minibatches allow for more examples/sec but may reduce the rate at which
                                // parameters are updated (per example)
        .withDropoutProbability(0.5)
        .withTrainingProgressLoggingFrequency(TrainingAmount.minibatches(50))
        .withEvaluationFrequency(1)
        .withEvaluationHoldoutProportion(0.2, 1337)
        .withMaxTrainingAmountWithoutImprovement(25)
        //.withTrainingPerformanceLoggingFrequency(TrainingAmount.minibatches(5))
        //.withTrainingSampledActivationsLoggingFrequency(TrainingAmount.minibatches(5))
        .withTrainingParametersLoggingFrequency(10000)
        .withMaxEpochs(500)
        .withOptimizer(new AdaGrad());

    // The classification can be obtained as a discrete distribution via
    // neuralNetwork.asLayerOutput(characterClassification), but we only care about the most likely label:
    MostLikelyLabelFromDistribution<String> predictedCharacter =
        new MostLikelyLabelFromDistribution<String>().withInput(neuralNetwork.asLayerOutput(characterClassification));

    // Build and return the DAG:
    return DAG.withPlaceholder(example).withOutput(predictedCharacter);
  }
}
