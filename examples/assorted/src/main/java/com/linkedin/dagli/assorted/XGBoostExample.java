package com.linkedin.dagli.assorted;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG1x1;
import com.linkedin.dagli.distribution.MostLikelyLabelFromDistribution;
import com.linkedin.dagli.list.NgramVector;
import com.linkedin.dagli.text.token.Tokens;
import com.linkedin.dagli.vector.DenseVectorFromNumbers;
import com.linkedin.dagli.vector.DensifiedVector;
import com.linkedin.dagli.vector.TopVectorElementsByMutualInformation;
import com.linkedin.dagli.xgboost.XGBoostClassification;
import java.util.Locale;


/**
 * In this example we predict the character who uttered a given line of dialog from a Shakespearean play by using
 * XGBoost gradient boosted decision trees.
 */
public class XGBoostExample {
  private XGBoostExample() { }

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

    // We want to create ngram features from the dialog, so we'll first need to break it into tokens:
    Tokens dialogTokens = new Tokens().withTextInput(example.asDialog()).withLocale(Locale.UK);

    // Our primary features will be unigrams, bigrams and trigrams:
    NgramVector ngramFeatures = new NgramVector().withMinSize(1).withMaxSize(3).withInput(dialogTokens);

    // However, this results in far too many features for a GBDT to handle; do feature selection to keep the top 500.
    // We're selecting an unusually low value here to keep the training time low, but it may still take several
    // minutes or more to train (depending on your machine).
    TopVectorElementsByMutualInformation topNgramFeatures = new TopVectorElementsByMutualInformation()
        .withMaxElementsToKeep(500)
        .withLabelInput(example.asCharacter())
        .withVectorInput(ngramFeatures);

    // We'll also use the average token length as a feature (not a very good feature, but this is just an example!):
    AverageTokenLength averageTokenLength = new AverageTokenLength().withInput(dialogTokens);

    // We need to convert this value a vector so we can combine it with our other features;
    DenseVectorFromNumbers averageTokenLengthVector = new DenseVectorFromNumbers().withInputs(averageTokenLength);

    // Now we combine our features into one vector and densify them, because XGBoost requires dense features:
    DensifiedVector denseFeatures = new DensifiedVector().withInputs(topNgramFeatures, averageTokenLengthVector);

    // Now configure our XGBoost classifier (a gradient boosted decision tree model):
    XGBoostClassification<String> classification =
        new XGBoostClassification<String>().withLabelInput(example.asCharacter()).withFeatureInput(denseFeatures);

    // Our classification result is a distribution over possible characters; we just want the most likely:
    MostLikelyLabelFromDistribution<String> mostLikelyCharacter =
        new MostLikelyLabelFromDistribution<String>().withInput(classification);

    // Build the DAG by specifying the output required placeholder:
    return DAG.withPlaceholder(example).withOutput(mostLikelyCharacter);
  }

  public static void main(String[] args) {
    Experiment.run(createDAG());
  }
}
