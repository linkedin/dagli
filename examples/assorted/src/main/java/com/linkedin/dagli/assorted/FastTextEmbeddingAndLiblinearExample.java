package com.linkedin.dagli.assorted;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG1x1;
import com.linkedin.dagli.distribution.MostLikelyLabelFromDistribution;
import com.linkedin.dagli.liblinear.LiblinearClassification;
import com.linkedin.dagli.embedding.classification.Embedded;
import com.linkedin.dagli.fasttext.FastTextClassification;
import com.linkedin.dagli.text.token.Tokens;
import java.util.Locale;


/**
 * In this example we predict the character who uttered a given line of dialog from a Shakespearean play by using a
 * FastText model to produce embeddings which we then feed to logistic regression in a simple pipeline.
 */
public class FastTextEmbeddingAndLiblinearExample {
  private FastTextEmbeddingAndLiblinearExample() { }

  /**
   * Creates the DAG that will be prepared (trained) to predict Shakespeare character from dialog text.
   *
   * The DAG accepts a {@link CharacterDialog} with both the character name and a line of their dialog; the character
   * name will be null during inference.
   *
   * @return the preparable DAG
   */
  public static DAG1x1<CharacterDialog, String> createDAG() {
    // Define the "placeholder" of the DAG.  When the DAG is executed, we'll provide the placeholder values as a list of
    // inputs.  If you think of the DAG as consuming a list of "rows", where each row is an example, placeholders are
    // the "columns" (although in this case we have just one--using the CharacterDialog @Struct simplifies things over
    // feeding in the dialog and character name separately).

    // Using CharacterDialog.Placeholder, which derives from Placeholder<CharacterDialog>, allows us to use convenience
    // methods to access the fields of the CharacterDialog @Struct:
    CharacterDialog.Placeholder example = new CharacterDialog.Placeholder();

    // FastText requires text to be broken into a list of tokens (words and punctuation symbols); we can use Tokens:
    Tokens dialogTokens = new Tokens().withTextInput(example.asDialog()).withLocale(Locale.UK);

    // Now configure the FastText classifier, from which we will later pull our supervised embeddings.  Note that we're
    // not actually going to use this classifier to classify anything!  All we care about are the embeddings.  When this
    // DAG runs, the FastText classifier will be trained, but it won't actually infer labels on anything (this is an
    // important point if you have a large set of labels since inference cost [but not training cost!] in FastText
    // scales with the size of the label set).

    // Since transformers are immutable, with____(...) creates copies of the transformer with the specified setting
    // changed. This "in-place builder" pattern is the standard for configuring transformers.
    FastTextClassification<String> fastTextClassification =
        new FastTextClassification<String>()
            .withLabelInput(example.asCharacter())
            .withTokensInput(dialogTokens)
            .withEpochCount(100)
            .withBucketCount(200000); // fewer buckets than default to save RAM; don't use this value for real problems!

    // Of course, we don't really care about the label predicted by FastText--we just want to use the trained model to
    // create an embedding for our text, which we'll use as features.  Fortunately, FastTextClassification has a
    // asEmbeddedTokens() method, which provides a "view" of the trained model as a transformer that takes in a sequence
    // of tokens and produces an embedding.

    // This embedding is actually a (feature) vector, and Embedded.Features<CharSequence> implements
    // Producer<? extends DenseVector> (i.e. it produces a DenseVector we can pass to our downstream model):
    Embedded.Features<CharSequence> fastTextEmbedding = fastTextClassification.asEmbeddedTokens();

    // An aside: we're only using the trained text embeddings from our FastText model, but you can also use
    // asEmbeddedLabel() on your FastText classifier to get a transformer that will embed labels according to the
    // learned label embeddings, too!

    // Note that we're *not* going to cross-train in this DAG, which we should normally do when using one model as an
    // input to another to avoid overfitting.  We won't go into too much detail here, but (1) with embeddings,
    // cross-training is impractical, and (2) overfitting is less of a concern because the ability of the model to
    // "memorize" examples is somewhat limited (this would be especially true if FastText could ignore rare ngrams, but
    // currently this is not supported).
    LiblinearClassification<String> classification = new LiblinearClassification<String>()
        .withLabelInput(example.asCharacter())
        .withFeatureInput(fastTextEmbedding);



    // Our classification is actually a distribution over possible characters; we just want the most likely:
    MostLikelyLabelFromDistribution<String> mostLikelyCharacter =
        new MostLikelyLabelFromDistribution<String>().withInput(classification);

    // build the DAG by specifying the placeholder used and the desired output:
    return DAG.withPlaceholder(example).withOutput(mostLikelyCharacter);
  }

  public static void main(String[] args) {
    Experiment.run(createDAG());
  }
}
