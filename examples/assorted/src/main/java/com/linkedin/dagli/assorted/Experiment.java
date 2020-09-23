package com.linkedin.dagli.assorted;

import com.linkedin.dagli.dag.DAG1x1;
import com.linkedin.dagli.evaluation.MultinomialEvaluation;
import com.linkedin.dagli.evaluation.MultinomialEvaluationResult;
import com.linkedin.dagli.objectio.ObjectReader;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


/**
 * Helper class used to run Shakespeare character classification experiments and eliminate duplicate code from examples.
 */
public class Experiment {
  private Experiment() { }

  private static final double TRAINING_FRACTION = 0.8; // fraction of data to use for training

  /**
   * Trains and evaluates a Shakespeare classification model that attempts to predict the character speaking a decagram
   * (10 sequential tokens from their dialog).  The model is expressed as a DAG accepting a CharacterDialog example as
   * input and producing the predicted character name as output.
   *
   * @param dag the model to train and evaluate
   */
  public static void run(DAG1x1<CharacterDialog, String> dag) {
    // We first need to read our Shakespeare data and shuffle it (we use a fixed random seed for repeatability).
    // Shuffling is useful for ensuring that examples do not appear in a predictable order, which is important for
    // online training methods like stochastic gradient descent (although it is usually irrelevant for batch training)
    List<CharacterDialog> exampleList = ShakespeareCorpus.readExamples();
    Collections.shuffle(exampleList, new Random(42));

    // Partition the data into training and evaluation examples.  Alternatively, we could use ObjectReader.wrap(...) to
    // create an ObjectReader from the list and then use ObjectReader::sample() to partition the data.
    List<CharacterDialog> trainingExamples = exampleList.subList(0, (int) (TRAINING_FRACTION * exampleList.size()));
    List<CharacterDialog> evaluationExamples = exampleList.subList(trainingExamples.size(), exampleList.size());

    // prepareAndApply(...) prepares the DAG and returns a DAG1x1.Result, which is an ObjectReader<String> we can use to
    // read all the predictions made for the *training* data as well as get the trained model via getPreparedDAG():
    DAG1x1.Result<CharacterDialog, String> dagResult = dag.prepareAndApply(trainingExamples);

    // We first evaluate how well we're predicting labels for the *training* examples.  This will almost invariably be
    // better than how well we do on the evaluation data, and is **not at all** a reliable gauge of how well our model
    // will really perform on examples it hasn't seen before (AKA the real world), but is useful for, e.g. determining
    // whether the model is over- or underfitting.  For instance, if the "auto-evaluation" results are much better than
    // the results on the held-out evaluation data this likely indicates overfitting, in which case either more training
    // data or a less complex model would be warranted.
    MultinomialEvaluationResult autoevaluation = MultinomialEvaluation.evaluate(
        trainingExamples.stream().map(CharacterDialog::getCharacter).collect(Collectors.toList()), dagResult);

    System.out.println("\nAutoevaluation of model predicting labels for its own training data:");
    System.out.println("---------------------------------------------------------------------");
    System.out.println(autoevaluation.getSummary() + "\n\n");

    // Assign the trained model to a local variable for convenience
    DAG1x1.Prepared<CharacterDialog, String> predictor = dagResult.getPreparedDAG();

    // Next, we predict labels for our held-out evaluation data, so we can see how well the model will do on examples
    // that it hasn't seen during training.  First, we get the predictions; we pass copies of our examples without
    // the character name to make sure the model can't "cheat":
    ObjectReader<String> predictedEvaluationCharacterNames = predictor.applyAll(
        evaluationExamples.stream().map(example -> example.withCharacter(null)).collect(Collectors.toList()));

    // Now we can evaluate the predictions against the true labels:
    MultinomialEvaluationResult evaluation = MultinomialEvaluation.evaluate(
        evaluationExamples.stream().map(CharacterDialog::getCharacter).collect(Collectors.toList()),
        predictedEvaluationCharacterNames);

    System.out.println("\nEvaluation of model predicting labels for evaluation (unseen) data:");
    System.out.println("-------------------------------------------------------------------");
    System.out.println(evaluation.getSummary() + "\n");

    // Finished!  In a real-world application, you'd also want to persist your model by serializing it, but we don't
    // need to do that here.
  }
}
