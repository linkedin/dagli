package com.linkedin.dagli.assorted;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG1x2;
import com.linkedin.dagli.evaluation.MultinomialEvaluation;
import com.linkedin.dagli.liblinear.LiblinearClassification;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import com.linkedin.dagli.meta.BestModel;
import com.linkedin.dagli.embedding.classification.Embedded;
import com.linkedin.dagli.fasttext.FastTextClassification;
import com.linkedin.dagli.embedding.classification.PreparedFastTextClassification;
import com.linkedin.dagli.object.Convert;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.text.token.Tokens;
import com.linkedin.dagli.view.PreparedTransformerView;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * This example introduces an advanced Dagli feature: transformer views.
 *
 * Transformer views provide a way to inspect the {@link com.linkedin.dagli.transformer.PreparedTransformer} that
 * results from preparing (training) a {@link com.linkedin.dagli.transformer.PreparableTransformer} during the execution
 * of the DAG, or exposing the result of that inspection as an output.
 *
 * Transformer views are used internally to implement sophisticated functionality such as
 * {@link FastTextClassification#asEmbeddedTokens()} or more simple "retrieval" such as
 * {@link BestModel#asBestPreparedModel()}.
 *
 * In this example we'll use {@link BestModel#asBestPreparedModel()} to examine the result of {@link BestModel}'s
 * model selection and also illustrate the direct use of views to inspect aspects of the trained model.
 */
public class TransformerViewExample {
  private TransformerViewExample() { }

  /**
   * In this example we'll:
   * (1) Define a DAG that does model selection using {@link BestModel} to find the best-performing FastText classifier
   *     whose embeddings are then fed as features to a logistic regression classifier
   * (2) Use transformer views (both implicitly and explicitly) to observe interesting aspects of the prepared DAG
   */
  public static void main(String[] args) {
    CharacterDialog.Placeholder example = new CharacterDialog.Placeholder();

    Tokens dialogTokens = new Tokens().withTextInput(example.asDialog());

    // create a prototype for our candidate models (which will differ in epoch count)
    FastTextClassification<String> fastTextPrototype = new FastTextClassification<String>()
        .withLabelInput(example.asCharacter())
        .withTokensInput(dialogTokens)
        .withBucketCount(200000);

    BestModel<DiscreteDistribution<String>> bestFastTextClassification = new BestModel<DiscreteDistribution<String>>()
        .withEvaluator(new MultinomialEvaluation().withActualLabelInput(example.asCharacter())::withPredictedLabelInput)
        .withCandidates(
            IntStream.of(20, 40, 80, 160).mapToObj(fastTextPrototype::withEpochCount).collect(Collectors.toList()));

    // cast the view to a PreparedFastTextClassification
    @SuppressWarnings("unchecked") // Java doesn't know that PreparedFastTextClassification.class can safely refer to
                                   // PreparedFastTextClassification<?>, so we're forced to cast to (Class)
    Producer<PreparedFastTextClassification<?>> preparedFastTextModel = Convert.Object.toClass(
        bestFastTextClassification.asBestPreparedModel(),
        (Class) PreparedFastTextClassification.class);

    // FastText accepts tokens of type CharSequence, we we need an embedding for this same type
    Embedded.Features<CharSequence> embeddedFeatures = new Embedded.Features<CharSequence>()
        .withClassifierInput(preparedFastTextModel)
        .withFeaturesInput(dialogTokens);

    LiblinearClassification<String> predictedCharacter =
        new LiblinearClassification<String>().withLabelInput(example.asCharacter()).withFeaturesInput(embeddedFeatures);

    // Create a PreparedTransformerView, which is a very simple viewer that simply observes the prepared transformer
    // itself.  We'll use this to get the trained Liblinear classifier to we can inspect it after the DAG is trained.
    PreparedTransformerView<LiblinearClassification.Prepared<String>> liblinearClassifierView =
        new PreparedTransformerView<>(predictedCharacter);

    // Build the DAG by specifying the placeholder and our desired outputs; we actually won't be using the prediction
    // of the model at all in this example (we just want to look at the model), but we'll specify it anyway as we might
    // if this were a real problem.
    DAG1x2.Prepared<CharacterDialog, DiscreteDistribution<String>, LiblinearClassification.Prepared<String>> dag =
        DAG.withPlaceholder(example)
            .withOutputs(predictedCharacter, liblinearClassifierView)
            .prepare(ShakespeareCorpus.readExamples());

    // Now things get interesting.  Our second output is a transformer view, and transformer views always prepare to
    // a constant (in this case, the value is the prepared transformer we want to examine); this means we can extract it
    // from the DAG directly:
    LiblinearClassification.Prepared<String> liblinearModel = dag.getConstantResult().get1();

    // Print out the bias for each label (this will roughly correspond with the frequency of the label in the data)
    for (String label : liblinearModel.getLabels()) {
      System.out.println("Bias for " + label + ": " + liblinearModel.getBiasForLabel(label));
    }
  }
}
