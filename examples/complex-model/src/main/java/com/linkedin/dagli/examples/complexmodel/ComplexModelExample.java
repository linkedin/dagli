package com.linkedin.dagli.examples.complexmodel;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG1x1;
import com.linkedin.dagli.data.dsv.DSVReader;
import com.linkedin.dagli.distribution.MostLikelyLabelFromDistribution;
import com.linkedin.dagli.distribution.SparseVectorFromDistribution;
import com.linkedin.dagli.evaluation.MultinomialEvaluation;
import com.linkedin.dagli.evaluation.MultinomialEvaluationResult;
import com.linkedin.dagli.function.FunctionResult1;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import com.linkedin.dagli.meta.BestModel;
import com.linkedin.dagli.fasttext.FastTextClassification;
import com.linkedin.dagli.xgboost.XGBoostClassification;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.objectio.SampleSegment;
import com.linkedin.dagli.text.token.Tokens;
import com.linkedin.dagli.vector.DenseVectorFromNumbers;
import com.linkedin.dagli.vector.DensifiedVector;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.csv.CSVFormat;


/**
 * This is an example of a reasonably complex Dagli ML pipeline that learns a model for predicting the character (e.g.
 * Caesar, Hamlet, etc.) from a line the character utters in a Shakespearean play.
 *
 * This task is analogous to any number of simple multinomial text classification tasks such as language classification,
 * topic detection, etc.
 *
 * The goal here is to show what typical code for training a model using "off-the-shelf" components might look like, not
 * to explore, e.g. creating your own transformers (see the other examples or the documentation in
 * {@code dagli/documentation/} to learn more about this).
 *
 * If you're solving a similar problem you can use this example as a template to get started, but we suggest considering
 * simpler DAGs at first as our choice of model architecture here is pedagogical rather than practical.
 *
 * More specifically, we create a pipelined model using a combination of FastText, XGBoost and simple features.  It's
 * undoubtedly over-engineered (and overly complex) for our straightforward text classification task.
 *
 * IntelliJ users: you must have annotation processing enabled to compile this example in the IDE.  Please see
 * {@code dagli/documentation/structs.md} for instructions.
 */
public abstract class ComplexModelExample {
  private ComplexModelExample() { } // nobody should be creating instances of this class

  /**
   * Trains a model, evaluates it, and (optionally) saves it.  See ShakespeareCLI (in this directory) for code that
   * loads the saved model (by default, it loads a pre-trained version out of a resource file).
   *
   * If no arguments are provided, it reads the data from the /shakespeare.tsv resource file and prints out the
   * evaluation.  Otherwise, a path to write the model to may be given as an argument.  In a real application you'd
   * read your data from a normal file (or the network) rather than a resource file, of course.
   *
   * Here's a summary of what the program will do:
   * (1) Use the Dagli-bundled DSVReader to "read" our delimiter-separated value resource file.  Technically, no data
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
    final DSVReader<CharacterDialog> charDialogData = new DSVReader<>()
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

    // We're ready to create our model (DAG) and train it.  By calling prepareAndApply(...), we obtain both our trained
    // (prepared) DAG for subsequent inference on new examples as well as the predictions made for the training data
    // itself (which will be part of our evaluation).
    DAG1x1.Result<CharacterDialog, String> result =
        createDAG().prepareAndApply(charDialogData.sample(trainingSegment));

    // Let's evaluate how well we're predicting labels for the *training* examples.  This will almost invariably be
    // better than how well we do on the evaluation data, and is **not at all** a reliable gauge of how well our model
    // will really perform on examples it hasn't seen before (AKA the real world), but is useful for, e.g. determining
    // whether the model is over- or underfitting.  For instance, if the "auto-evaluation" results are much better than
    // the results on the held-out evaluation data this likely indicates overfitting, in which case either more training
    // data or a less complex model would be warranted.
    MultinomialEvaluationResult autoevaluation = MultinomialEvaluation.evaluate(
        charDialogData.lazyMap(CharacterDialog::getCharacter).sample(trainingSegment), result);

    // Let's print it out:
    System.out.println("\nAutoevaluation of model predicting labels for its own training data:");
    System.out.println("---------------------------------------------------------------------");
    System.out.println(autoevaluation.getSummary() + "\n\n");

    // Assign the trained model to a local variable for convenience
    DAG1x1.Prepared<CharacterDialog, String> predictor = result.getPreparedDAG();

    // Next, we'll predict labels for our held-out evaluation data, so we can see how well the model will do on examples
    // that it hasn't seen during training.  First, we get the predictions:
    ObjectReader<String> predictedEvaluationCharacterNames =
        predictor.applyAll(charDialogData.sample(evaluationSegment));
    // ^ ObjectReader extends (and enhances) Iterable; charDialogData (and our .sample(...) thereof) implement
    // ObjectReader too, but notice that applyAll(...) would also accept any other Iterable type, such as Lists.

    MultinomialEvaluationResult evaluation =
        MultinomialEvaluation.evaluate(charDialogData.lazyMap(CharacterDialog::getCharacter).sample(evaluationSegment),
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
    CharacterDialog.Placeholder characterDialog = new CharacterDialog.Placeholder();

    // We're given the raw text, so we need to tokenize it (break it up into a list of words):
    Tokens dialogTokens =
        new Tokens().withLocale(Locale.ENGLISH).withTextInput(characterDialog.asDialog());

    // Now configure our FastText classifier:
    FastTextClassification<String> fastTextClassification =
        new FastTextClassification<String>()
            .withLabelInput(characterDialog.asCharacter())
            .withTokensInput(dialogTokens)
            .withEmbeddingLength(64)
            .withMinTokenCount(1)
            .withEpochCount(100) // higher values yield better performance on this problem, but take longer to train
            .withBucketCount(200000); // fewer buckets than default to save RAM; don't use this value for real problems

    // Predicting Shakespeare characters is actually a hard task (characters in the same play tend to say similar
    // things.)  For reference, the configuration above yields accuracy of ~16%, and can be improved to >20% with some
    // manual hyperparameter tuning (and perhaps substantially higher; feel free to experiment!)
    //
    // However, manual hyperparameter tuning has two main drawbacks:
    // (1) It takes (human) time to run a series of experiments manually, interpret the results and then act on them.
    // (2) It's usually cheating, because people tend to use the final evaluation data as a guide; that is, they select
    //     the hyperparameters that seem to give the best numbers on a predetermined evaluation set, then report those
    //     metrics as the expected performance of the model on unseen data.  This is incorrect, because now the
    //     hyperparameters have been (manually) fit to the putatively "unseen" evaluation data and so evaluation metrics
    //     will overestimate the model's true performance.  Instead, a development set, independent of both the training
    //     and evaluation data, should be used to tune.
    //
    // The alternative is to let Dagli tune the hyperparameter automatically using the BestModel transformer, creating
    // multiple variants of the model and letting BestModel pick the best one.
    //
    // For each candidate model, BestModel will use two or more splits, train a model corresponding to each, and then
    // produce a composite evaluation over the collective results of the models; the candidate model with the best
    // candidate evaluation is selected as the best model and is retrained on final time, with all the training data.
    //
    // For example, if we specify a split count of 3, then a candidate model will be trained three times, each time
    // using 2/3 of the available data for training and 1/3 for evaluation (the evaluation splits are disjoint such that
    // every example is used for evaluation exactly once).  The higher the split count, the more accurately the
    // performance of the model can be estimated (i.e. a model trained with 80% of data is a better approximation of
    // a model trained with 100% of the data than one trained with just 50%) but, of course, comes with a linearly
    // increasing cost in training time (and memory).
    //
    // Incidentally, DiscreteDistribution<String> is the type parameter for BestModel because our FastText model
    // produces a discrete distribution that associates a probability with some set of labels (in our case, it gives
    // a probability for each Shakespearean character name).
    //
    // If you're not sure what Java type a Dagli node produces, you can quickly check its implementation (specifically,
    // its class signature) to see what producer/placeholder/generator/transformer/view type it extends or implements.
    // Ultimately, all nodes in a Dagli DAG derive from Producer<ResultType>, where ResultType is the type of thing
    // "produced" by the node.
    BestModel<DiscreteDistribution<String>> bestFastTextClassification =
        new BestModel<DiscreteDistribution<String>>()
        .withSplitCount(3) // approximately 33% of training data will serves as the development set in each split
        // withEvaluator(...) basically accepts a "factory function" that creates an evaluation transformer (here, a
        // MultinomialEvaluation) when passed an input that provides the predictions from a candidate model.  This is
        // complicated in principle but usually succinct in practice; see the Javadoc for withEvaluator() for details.
        .withEvaluator(new MultinomialEvaluation().withActualLabelInput(characterDialog.asCharacter())
            ::withPredictedDistributionInput)
        // CROSS_INFERENCE lets us combine finding the best model with cross-training, the latter of which allows us to
        // avoid overfitting when we use the best FastText model to generate features for a downstream statistical
        // model.  Please see the Javadoc on the BestModel and PreparationDataInferenceMode classes for more
        // information.  Incidentally, the related KFoldCrossTrained transformer can be used to cross-train a single
        // model without model selection, which can be useful for the same reason.
        .withPreparationDataInferenceMode(BestModel.PreparationDataInferenceMode.CROSS_INFERENCE)
        // Try a few variants of our FastText classifier with differing maximum ngram sizes as our candidate models:
        .withCandidates(IntStream.of(4, 6, 8).mapToObj(fastTextClassification::withMaxWordNgramLength).collect(
            Collectors.toList()));

    // bestFastTextClassification produces a discrete distribution over our labels, but what we really want is a feature
    // vector we can feed to a downstream model.  SparseVectorizedDistribution converts a distribution to a vector:
    SparseVectorFromDistribution fastTextFeatures =
        new SparseVectorFromDistribution().withInput(bestFastTextClassification);

    // We'd also like to create two additional, simple features:
    // (1) the String length of the line of dialog:
    FunctionResult1<String, Integer> dialogLength =
        new FunctionResult1<>(String::length).withInput(characterDialog.asDialog());

    // (2) the length in tokens:
    FunctionResult1<List<?>, Integer> dialogTokenCount =
        new FunctionResult1<List<?>, Integer>(List::size).withInput(dialogTokens);

    // But we need our features in a vector, not scalar integers, so let's put them in a vector:
    DenseVectorFromNumbers otherFeatures = new DenseVectorFromNumbers().withInputs(dialogLength, dialogTokenCount);

    // Now we have two vectors of features for ach example, but XGBoost accepts just one.  So we combine them with
    // DensifiedVector, which both combines vectors and "densifies" them, remapping the indicies of the vector elements
    // so that, if there are N distinct elements (across all vectors) that take non-zero values in any of the examples,
    // then these elements will have indices 0...N-1.  Many statistical models (like XGBoost) require a dense vector as
    // input, although others (like Liblinear) can accept any kind of vector, sparse or dense.
    DensifiedVector features = new DensifiedVector().withInputs(fastTextFeatures, otherFeatures);

    // The final classification will be determined by XGBoost (boosted decision trees) using our combined features:
    XGBoostClassification<String> finalClassification = new XGBoostClassification<String>()
        .withFeaturesInput(features)
        .withLabelInput(characterDialog.asCharacter());

    // Our classification is actually a distribution over possible characters; we just need the most likely:
    MostLikelyLabelFromDistribution<String> mostLikelyCharacter =
        new MostLikelyLabelFromDistribution<String>().withInput(finalClassification);

    // Build the DAG by specifying the output and the required placeholder:
    return DAG.withPlaceholder(characterDialog).withOutput(mostLikelyCharacter);
  }
}
