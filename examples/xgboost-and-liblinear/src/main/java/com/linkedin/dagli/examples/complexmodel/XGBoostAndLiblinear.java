package com.linkedin.dagli.examples.complexmodel;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG1x1;
import com.linkedin.dagli.data.dsv.DSVReader;
import com.linkedin.dagli.distribution.MostLikelyLabelFromDistribution;
import com.linkedin.dagli.evaluation.MultinomialEvaluation;
import com.linkedin.dagli.evaluation.MultinomialEvaluationResult;
import com.linkedin.dagli.function.FunctionResult1;
import com.linkedin.dagli.liblinear.LiblinearClassification;
import com.linkedin.dagli.list.NgramVector;
import com.linkedin.dagli.list.Size;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.object.BucketIndex;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.objectio.SampleSegment;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.text.token.Tokens;
import com.linkedin.dagli.vector.CategoricalFeatureVector;
import com.linkedin.dagli.vector.CompositeSparseVector;
import com.linkedin.dagli.vector.DensifiedVector;
import com.linkedin.dagli.xgboost.XGBoostClassification;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import org.apache.commons.csv.CSVFormat;


/**
 * This is an example of a popular pipelined classifier architecture in industry: training an XGBoost model and then
 * using the leaves that are active for each example as (some) of the features in a logistic regression model.  The idea
 * is that logistic regression can handle a vast number of features but XGBoost is able to learn (non-linear) feature
 * conjunctions.
 *
 * For this example, our task is to predict character (e.g. Caesar, Hamlet, etc.) from a line the character utters in a
 * Shakespearean play.  This model does relatively well, with 20.7% accuracy.
 *
 * IntelliJ users: you must have annotation processing enabled to compile this example in the IDE.  Please see
 * {@code dagli/documentation/structs.md} for instructions.
 */
public abstract class XGBoostAndLiblinear {
  private XGBoostAndLiblinear() { } // nobody should be creating instances of this class

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
    CharacterDialog.Placeholder example = new CharacterDialog.Placeholder();

    // We're given the raw text, so we need to tokenize it (break it up into a list of words):
    Tokens dialogTokens = new Tokens().withLocale(Locale.ENGLISH).withTextInput(example.asDialog());

    // We'd like to feed some text features into our XGBoost model, but feeding a huge number of ngrams into XGBoost
    // would be too expensive.  To keep training fast, we'll just give it unigrams.  The easiest way to create a
    // feature vector from ngrams is to use NgramVector (which will, by default, extract only unigrams):
    NgramVector unigramFeatures = new NgramVector().withInput(dialogTokens);

    // Now we can set up our XGBoost model.  Properly speaking, we should be using different data to train XGBoost
    // than we use to train our subsequent logistic regression model, since otherwise we risk having our pipelined model
    // overfit (the XGBoost model will be generated active-leaf features for examples it was trained with, which will be
    // more predictable/less noisy than they would be in reality).
    //
    // Normally, we could handle this elegantly with a KFoldCrossTrained transformer, but that doesn't help us here
    // because the learned trees we're using to generate our features wouldn't be consistent between XGBoost models
    // trained with different subsets of data.
    //
    // Alternatively, we could just train a DAG containing (only) the XGBoost model on a separate data set.  However,
    // for now, we'll just accept the additional risk of overfitting and use the same examples to train both XGBoost and
    // the downstream logistic regression model.
    XGBoostClassification<String> xgboostClassifier = new XGBoostClassification<String>()
        .withFeaturesInput().fromVectors(unigramFeatures)
        .withLabelInput(example.asCharacter())
        .withRounds(10); // the number trees learned will be the number of rounds times the number of labels

    // Of course, we don't want the XGBoost classification itself; rather, we want to get a feature vector corresponding
    // to the leaves that are active in each tree in the forest for each example, so we can feed these to our logistic
    // regression classifier:
    Producer<Vector> leafFeatures = xgboostClassifier.asLeafFeatures();

    // Now let's get a bag of unigram, bigram and trigram features (of which there would be too many to use directly in
    // our XGBoost model)
    NgramVector ngramFeatures = new NgramVector().withMaxSize(3).withInput(dialogTokens);

    // We'd also like to use two additional, bucketized features: the dialog length (in chars) and the length in tokens.
    // The bucket boundaries/sizes will be chosen to minimize the squared error relative to a uniform distribution of
    // items per bucket; the default number of buckets (which we'll use here) is 10.
    BucketIndex dialogLengthBucket =
        new BucketIndex().withInput(new FunctionResult1<>(String::length).withInput(example.asDialog()));
    BucketIndex averageTokenLengthBucket =
        new BucketIndex().withInput(new Size().withInput(dialogTokens));

    // Feed all our features into our logistic regression classifier:
    LiblinearClassification<String> lrClassifier = new LiblinearClassification<String>()
        .withLabelInput(example.asCharacter())
        .withFeaturesInput().combining()
            .fromCategoricalValues(dialogLengthBucket, averageTokenLengthBucket) // each bucket is a categorical feature
            .fromVectors(ngramFeatures, leafFeatures)
        .done();

    // Finally, get the most likely label:
    MostLikelyLabelFromDistribution<String> predictedLabel =
        new MostLikelyLabelFromDistribution<String>().withInput(lrClassifier);

    // Build the DAG by specifying the output and the required placeholder:
    return DAG.withPlaceholder(example).withOutput(predictedLabel);
  }
}
