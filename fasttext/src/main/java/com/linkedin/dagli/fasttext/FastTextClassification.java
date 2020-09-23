package com.linkedin.dagli.fasttext;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.distribution.MostLikelyLabelFromDistribution;
import com.linkedin.dagli.distribution.MostLikelyLabelsFromDistribution;
import com.linkedin.dagli.embedding.classification.Embedded;
import com.linkedin.dagli.embedding.classification.FastTextInternal;
import com.linkedin.dagli.embedding.classification.PreparedFastTextClassification;
import com.linkedin.dagli.preparer.AbstractStreamPreparer2;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.preparer.PreparerResultMixed;
import com.linkedin.dagli.transformer.PreparedTransformer2;
import com.linkedin.dagli.view.AbstractTransformerView;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import java.io.Serializable;


/**
 * FastText is a multinomial or multilabel text classification model that trains embeddings for both the text and
 * labels and then uses the dot-product of the text and label embeddings as a signal for how probable the label is for
 * that text.
 *
 * FastText classifiers are normally simple creatures that expect sentences consisting of string "words" and have string
 * labels.  FastTextClassification heavily generalizes the standard FastText model, allowing you to use any type of
 * label.
 *
 * FastText was built with natural text in mind, which brings with it some limitations:
 * - Weights are not supported by FastText at present, although you may repeat tokens and examples at your leisure
 * - Features ("sentences") must be strings, concretely, Iterable&lt;String&gt;.  However, for
 *   non-text binary features, you may easily pass the name of the feature, the stringified hash, etc.  Note that by
 *   default FastText will compute word ngrams for these strings, which is probably undesirable!
 *
 * In the input tokens, character c == ' ' || c == '\n' || c == '\r' || c == '\t' || c == '\v' || c == '\f' || c == '\0'
 * will be replaced by a "_".  TOKEN STRINGS MUST BE VALID UTF-16 SEQUENCES as they will be converted into UTF-8.
 *
 * Conflicts of tokens with special FastText strings are handled by prepending underscores:
 * The string "&lt;/s&gt;" is special to FastText and will be rewritten into "_&lt;/s&gt;".
 * The prefix "__label__" is also special and will be rewritten to "___label__".
 *
 * This is invisible to clients (you) except that it may cause tokens that would otherwise not collide to collide in
 * specially-constructed corner cases (e.g. if you had both word tokens "__label__" and "___label__" in your input.
 * The labels themselves, even if they are strings, have no restrictions and will not be altered in any way.
 *
 * FastText returns a {@link DiscreteDistribution} for each input, which gives the probabilities of the predicted
 * labels. Use the as____() convenience methods, such as asEmbeddedTokens(), to use the supervised model's embeddings
 * to embed text or labels, which can, but don't have to, be the text or labels used during training.
 *
 * @param <L> type of the label.  L must (1) have hashCode() and equals() implementations and (2) be serializable.
 */
@ValueEquality
public class FastTextClassification<L extends Serializable>
    extends
    AbstractFastTextModel<L, DiscreteDistribution<L>, PreparedFastTextClassification<L>, FastTextClassification<L>> {
  private static final long serialVersionUID = 1;

  @ValueEquality
  private static class ModelView<L extends Serializable>
      extends AbstractTransformerView<FastTextInternal.Model<L>, PreparedFastTextClassification<L>, ModelView<L>> {
    private static final long serialVersionUID = 1;

    public ModelView(FastTextClassification<L> fastTextClassification) {
      super(fastTextClassification);
    }

    @Override
    protected FastTextInternal.Model<L> prepare(PreparedFastTextClassification<L> preparedTransformerForNewData) {
      return preparedTransformerForNewData.getModel();
    }
  }

  /**
   * Creates a text embedder that uses this model (once trained) to embed sequences of tokens; by default, it embeds the
   * same token sequences used to train the model, but you may call withFeaturesInput(...) to change this to embed
   * any other text you'd like instead.
   *
   * @return a transformer that uses the supervised embeddings learned by the model to embed sequences of tokens.
   */
  public Embedded.Features<CharSequence> asEmbeddedTokens() {
    return new Embedded.Features<CharSequence>()
        .withEmbeddingModelInput(new ModelView<L>(this))
        .withFeaturesInput(getTokensInput());
  }

  /**
   * Creates a label embedder that uses this model (once trained) to embed collections of labels; by default, it embeds
   * the labels predicted by the model (in decreasing order of their predicted likelihood), but you may call
   * withLabelsInput(...) to change this to embed the training labels or any other labels, too.
   *
   * A null collection of labels will produce a null result.
   *
   * @return a transformer that uses the supervised embeddings learned by the model to embed sequences of labels.
   */
  public Embedded.Labels<L> asEmbeddedLabels() {
    return new Embedded.Labels<L>()
        .withEmbeddingModelInput(new ModelView<L>(this))
        .withLabelsInput(new MostLikelyLabelsFromDistribution<L>().withInput(this));
  }

  /**
   * Creates a label embedder that uses this model (once trained) to embed a label; by default, it embeds
   * the most likely label predicted by the model, returning null if no labels are predicted; however, you may call
   * withLabelInput(...) to change this to embed a training label or any other label of choice.
   *
   * A null label will yield a null result.
   *
   * @return a transformer that uses the supervised embeddings learned by the model to embed sequences of labels.
   */
  public Embedded.Label<L> asEmbeddedLabel() {
    return new Embedded.Label<L>()
        .withEmbeddingModelInput(new ModelView<L>(this))
        .withLabelInput(new MostLikelyLabelFromDistribution<L>().withDefaultLabel(null).withInput(this));
  }

  /**
   * Creates a transformer that determines how many of the inputted list of tokens are "in vocabulary" for this model;
   * i.e. those for which FastText has an embedding.  By default, it counts the in-vocabulary features of the
   * same token sequence used to train the model, but you may call withFeaturesInput(...) to change this to check
   * any other tokens you'd like instead.
   *
   * @return a transformer that checks how many of the inputted tokens are known to the model.
   */
  public Embedded.FeaturesInVocabularyCount<CharSequence> asTokensInVocabularyCount() {
    return new Embedded.FeaturesInVocabularyCount<CharSequence>()
        .withEmbeddingModelInput(new ModelView<L>(this))
        .withFeaturesInput(getTokensInput());
  }

  @Override
  protected Preparer<L> getPreparer(PreparerContext context) {
    return new Preparer<>(this);
  }

  private static class Preparer<L extends Serializable>
      extends AbstractStreamPreparer2<Iterable<? extends L>, Iterable<? extends CharSequence>, DiscreteDistribution<L>, PreparedFastTextClassification<L>> {
    private final Trainer<L> _trainer;
    private final int _topK;

    public Preparer(FastTextClassification<L> owner) {
      _trainer = new Trainer<>(owner);
      _topK = owner._topK;
    }

    @Override
    public PreparerResultMixed<
        ? extends PreparedTransformer2<Iterable<? extends L>, Iterable<? extends CharSequence>, DiscreteDistribution<L>>,
        PreparedFastTextClassification<L>> finish() {
      return new PreparerResult<>(new PreparedFastTextClassification<>(_trainer.finish(), _topK));
    }

    @Override
    public void process(Iterable<? extends L> labelsInput, Iterable<? extends CharSequence> tokensInput) {
      _trainer.process(labelsInput, tokensInput);
    }
  }
}
