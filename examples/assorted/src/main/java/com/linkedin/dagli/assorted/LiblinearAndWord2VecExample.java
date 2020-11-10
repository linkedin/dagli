package com.linkedin.dagli.assorted;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG1x1;
import com.linkedin.dagli.distribution.MostLikelyLabelFromDistribution;
import com.linkedin.dagli.word2vec.Word2VecEmbedding;
import com.linkedin.dagli.liblinear.LiblinearClassification;
import com.linkedin.dagli.text.token.Tokens;
import com.linkedin.dagli.vector.AveragedDenseVector;
import com.linkedin.dagli.vector.CompositeSparseVector;
import com.linkedin.dagli.vector.DenseVectorFromNumbers;
import java.util.Locale;


/**
 * In this example we predict the character who uttered a given line of dialog from a Shakespearean play using logistic
 * regression over Word2Vec pretrained embeddings.
 *
 * <strong>This example will not run successfully without a Word2Vec data file in your resource path!</strong>  This can
 * be easily downloaded--please see the Javadoc for {@link Word2VecEmbedding} for details.
 */
public class LiblinearAndWord2VecExample {
  private LiblinearAndWord2VecExample() { }

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

    // Word2Vec embeds words, not free text, so we need to break the dialog text into a list of tokens (words and
    // punctuation symbols):
    Tokens dialogTokens = new Tokens().withTextInput(example.asDialog()).withLocale(Locale.UK);

    // embed our text snippet using pretrained Word2Vec embeddings
    // the result of this is a list of vectors, one for each word/phrase that has a known embedding in dictionary
    Word2VecEmbedding embeddings = new Word2VecEmbedding()
        .withMaxDictionarySize(1000000) // Shakespeare is esoteric--load more dictionary entries than default
        .withInput(dialogTokens);

    // Average all the embeddings of the individual word/phrases
    AveragedDenseVector averagedEmbedding = new AveragedDenseVector().withInputList(embeddings);

    // Aalso use the average token length as a feature:
    AverageTokenLength averageTokenLength = new AverageTokenLength().withInput(dialogTokens);

    // We need to convert this to a vector so we can combine it with the average embedding vector:
    DenseVectorFromNumbers averageTokenLengthVector = new DenseVectorFromNumbers().withInputs(averageTokenLength);

    // Now configure our liblinear logistic regression / max entropy model:
    LiblinearClassification<String> classification = new LiblinearClassification<String>()
        .withLabelInput(example.asCharacter())
        .withFeaturesInput().fromVectors(averagedEmbedding, averageTokenLengthVector);

    // our classification result is a distribution over possible characters; we just want the most likely:
    MostLikelyLabelFromDistribution<String> mostLikelyCharacter =
        new MostLikelyLabelFromDistribution<String>().withInput(classification);

    // build the DAG by specifying the output and the required placeholder:
    return DAG.withPlaceholder(example).withOutput(mostLikelyCharacter);
  }

  public static void main(String[] args) {
    Experiment.run(createDAG());
  }
}