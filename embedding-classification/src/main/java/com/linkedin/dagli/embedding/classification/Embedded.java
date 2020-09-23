package com.linkedin.dagli.embedding.classification;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.function.FunctionResult1;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer2;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import java.io.Serializable;
import java.util.List;


/**
 * Container class hosting nested classes for inference with embedding models.
 */
public class Embedded {
  private Embedded() {
  }

  /**
   * PreparedTransformer that applies an AbstractEmbeddingClassifier classifier to its input.
   * This class should be specialized for each model type.
   *
   * @param <L> the type of label that the classifier accepts.
   * @param <T> the type of "feature" that the classifier accepts as input
   * @param <R> the type of thing returned by the classifier
   */
  private abstract static class Abstract<L extends Serializable, T, R, S extends Abstract<L, T, R, S>>
      extends AbstractPreparedTransformer2<Iterable<? extends L>, Iterable<? extends T>, R, S> {
    private static final long serialVersionUID = 1;

    private final AbstractEmbeddingClassifier<L, T> _classifier;
    private final int _topK;

    public Abstract(AbstractEmbeddingClassifier<L, T> classifier, int topK) {
      _classifier = classifier;
      _topK = topK;
    }

    /**
     * Gets the underlying model being used
     *
     * @return the model
     */
    public AbstractEmbeddingClassifier<L, T> getModel() {
      return _classifier;
    }

    /**
     * Gets the prediction limit (i.e. the maximum number of labels that will be predicted)
     *
     * @return the max number of predicted labels
     */
    public int getPredictionLimit() {
      return _topK;
    }
  }

  abstract static class Result<L extends Serializable, T, S>
      extends Abstract<L, T, EmbeddingClassification<L>, Result<L, T, S>> {
    private static final long serialVersionUID = 1;

    public Result(AbstractEmbeddingClassifier<L, T> classifier, int topK) {
      super(classifier, topK);
    }

    /**
     * Gets the {@link EmbeddingClassification} for the given inputs.
     *
     * @param labels the labels of the example, or null if no labels are available
     * @param features the features of the example
     * @return an inferred {@link EmbeddingClassification} for the example
     */
    @Override
    public EmbeddingClassification<L> apply(Iterable<? extends L> labels, Iterable<? extends T> features) {
      return getModel().createResult(features, getPredictionLimit(), labels);
    }
  }

  /**
   * Base class for an embedding-based classifier transformer.
   *
   * @param <L> the type of label (must be {@link Serializable})
   * @param <T> the type of the features
   * @param <S> the type of the derived class extending this one
   */
  public abstract static class Classifier<L extends Serializable, T, S extends Classifier<L, T, S>>
      extends Abstract<L, T, DiscreteDistribution<L>, Classifier<L, T, S>> {
    private static final long serialVersionUID = 1;

    /**
     * Creates a new instance that will wrap the provided classifier.
     *
     * @param classifier the classifier to use
     * @param topK the maximum number of labels to return for any particular prediction
     */
    public Classifier(AbstractEmbeddingClassifier<L, T> classifier, int topK) {
      super(classifier, topK);
    }

    /**
     * Gets the {@link EmbeddingClassification} for the given inputs.
     *
     * @param labels the labels of the example, or null if no labels are available
     * @param features the features of the example
     * @return an inferred {@link EmbeddingClassification} for the example
     */
    @Override
    public DiscreteDistribution<L> apply(Iterable<? extends L> labels, Iterable<? extends T> features) {
      return getModel().createResult(features, getPredictionLimit(), labels).getClassification();
    }
  }

  /**
   * Given an {@link AbstractEmbeddingClassifier} model and a collection of features, embeds the features into a single {@link DenseFloatArrayVector}.
   *
   * @param <T> the type of feature used by the EmbeddingClassifier.
   */
  @ValueEquality
  public static class Features<T>
      extends AbstractPreparedTransformer2<AbstractEmbeddingClassifier<?, T>, Iterable<? extends T>, DenseFloatArrayVector, Features<T>> {

    private static final long serialVersionUID = 1;

    public Features() { }

    /**
     * Returns a copy of this instance that will obtain its embeddings from the given classifier.
     *
     * @param classifierInput an input supplying a prepared classifier
     * @return a copy of this instance that will obtain its embeddings from the given classifier.
     */
    public Features<T> withClassifierInput(Producer<? extends Classifier<?, T, ?>> classifierInput) {
      return withEmbeddingModelInput(
          new FunctionResult1<Classifier<?, T, ?>, AbstractEmbeddingClassifier<?, T>>(
              Abstract::getModel).withInput(classifierInput));
    }

    /**
     * Returns a copy of this instance that will obtain its embeddings from the given embedding model.
     *
     * @param modelInput an input supplying an embedding model
     * @return a copy of this instance that will obtain its embeddings from the given classifier.
     */
    public Features<T> withEmbeddingModelInput(Producer<? extends AbstractEmbeddingClassifier<?, T>> modelInput) {
      return withInput1(modelInput);
    }

    /**
     * Sets the producer that will provide the features to be embedded.
     *
     * @param featuresInput the input providing the features
     * @return a copy of this instance that will use the specified input
     */
    public Features<T> withFeaturesInput(Producer<? extends Iterable<? extends T>> featuresInput) {
      return withInput2(featuresInput);
    }

    @Override
    public DenseFloatArrayVector apply(AbstractEmbeddingClassifier<?, T> model, Iterable<? extends T> features) {
      return model.embedFeatures(features).getFeaturesEmbedding();
    }
  }

  /**
   * Given an {@link AbstractEmbeddingClassifier} model and a collection of features, gets the number of features that
   * that the model has in its vocabulary (out-of-vocab features are ignored).
   *
   * @param <T> the type of feature used by the EmbeddingClassifier.
   */
  @ValueEquality
  public static class FeaturesInVocabularyCount<T>
      extends AbstractPreparedTransformer2<AbstractEmbeddingClassifier<?, T>, Iterable<? extends T>, Integer, FeaturesInVocabularyCount<T>> {

    private static final long serialVersionUID = 1;

    public FeaturesInVocabularyCount() { }

    /**
     * Returns a copy of this instance that will obtain its vocabulary from the given classifier.
     *
     * @param classifierInput an input supplying a prepared classifier
     * @return a copy of this instance that will obtain its vocabulary from the given classifier.
     */
    public FeaturesInVocabularyCount<T> withClassifierInput(Producer<? extends Classifier<?, T, ?>> classifierInput) {
      return withEmbeddingModelInput(
          new FunctionResult1<Classifier<?, T, ?>, AbstractEmbeddingClassifier<?, T>>(
              Abstract::getModel).withInput(classifierInput));
    }

    /**
     * Returns a copy of this instance that will obtain its vocabulary from the given model.
     *
     * @param modelInput an input supplying a model
     * @return a copy of this instance that will obtain its vocabulary from the given model.
     */
    public FeaturesInVocabularyCount<T> withEmbeddingModelInput(
        Producer<? extends AbstractEmbeddingClassifier<?, T>> modelInput) {
      return withInput1(modelInput);
    }

    /**
     * Sets the producer that will provide the features to be checked.
     *
     * @param featuresInput the input providing the features
     * @return a copy of this instance that will use the specified input
     */
    public FeaturesInVocabularyCount<T> withFeaturesInput(Producer<? extends Iterable<? extends T>> featuresInput) {
      return withInput2(featuresInput);
    }

    @Override
    public Integer apply(AbstractEmbeddingClassifier<?, T> model, Iterable<? extends T> features) {
      return model.embedFeatures(features).getFeaturesInVocabularyCount();
    }
  }

  /**
   * Given an {@link AbstractEmbeddingClassifier} model and a collection of labels, embeds each label and returns a list
   * of the embeddings.  A null collection of labels will be embedded to null.
   *
   * @param <L> the type of label used by the EmbeddingClassifier.
   */
  @ValueEquality
  public static class Labels<L extends Serializable>
      extends AbstractPreparedTransformer2<AbstractEmbeddingClassifier<L, ?>, Iterable<? extends L>, List<DenseFloatArrayVector>, Labels<L>> {

    private static final long serialVersionUID = 1;

    public Labels() { }

    /**
     * Returns a copy of this instance that will obtain its embeddings from the given classifier.
     *
     * @param classifierInput an input supplying a prepared classifier
     * @return a copy of this instance that will obtain its embeddings from the given classifier.
     */
    public Labels<L> withClassifierInput(Producer<? extends Classifier<L, ?, ?>> classifierInput) {
      return withEmbeddingModelInput(
          new FunctionResult1<Classifier<L, ?, ?>, AbstractEmbeddingClassifier<L, ?>>(
              Abstract::getModel).withInput(classifierInput));
    }

    /**
     * Returns a copy of this instance that will obtain its embeddings from the given model.
     *
     * @param modelInput an input supplying a model
     * @return a copy of this instance that will obtain its embeddings from the given model.
     */
    public Labels<L> withEmbeddingModelInput(Producer<? extends AbstractEmbeddingClassifier<L, ?>> modelInput) {
      return withInput1(modelInput);
    }

    /**
     * Sets the producer that will provide the labels to be embedded.
     *
     * @param labelsInput the input providing the labels
     * @return a copy of this instance that will use the specified input
     */
    public Labels<L> withLabelsInput(Producer<? extends Iterable<? extends L>> labelsInput) {
      return withInput2(labelsInput);
    }

    @Override
    public List<DenseFloatArrayVector> apply(AbstractEmbeddingClassifier<L, ?> model, Iterable<? extends L> labels) {
      if (labels == null) {
        return null;
      }
      return model.getLabelEmbeddings(labels);
    }
  }

  /**
   * Given an {@link AbstractEmbeddingClassifier} model and a single label, gets the embedding for the label.
   * A null label will be embedded to null.
   *
   * @param <L> the type of label used by the EmbeddingClassifier.
   */
  @ValueEquality
  public static class Label<L extends Serializable>
      extends AbstractPreparedTransformer2<AbstractEmbeddingClassifier<L, ?>, L, DenseFloatArrayVector, Label<L>> {

    private static final long serialVersionUID = 1;

    public Label() { }

    /**
     * Returns a copy of this instance that will obtain its embeddings from the given classifier.
     *
     * @param classifierInput an input supplying a prepared classifier
     * @return a copy of this instance that will obtain its embeddings from the given classifier.
     */
    public Label<L> withClassifierInput(Producer<? extends Classifier<L, ?, ?>> classifierInput) {
      return withEmbeddingModelInput(
          new FunctionResult1<Classifier<L, ?, ?>, AbstractEmbeddingClassifier<L, ?>>(
              Abstract::getModel).withInput(classifierInput));
    }

    /**
     * Returns a copy of this instance that will obtain its embeddings from the given model.
     *
     * @param modelInput an input supplying a model
     * @return a copy of this instance that will obtain its embeddings from the given model.
     */
    public Label<L> withEmbeddingModelInput(Producer<? extends AbstractEmbeddingClassifier<L, ?>> modelInput) {
      return withInput1(modelInput);
    }

    /**
     * Sets the producer that will provide the label to be embedded.
     *
     * @param labelInput the input providing the label
     * @return a copy of this instance that will use the specified input
     */
    public Label<L> withLabelInput(Producer<? extends L> labelInput) {
      return withInput2(labelInput);
    }

    @Override
    public DenseFloatArrayVector apply(AbstractEmbeddingClassifier<L, ?> model, L label) {
      if (label == null) {
        return null;
      }
      return model.embedLabel(label);
    }
  }
}

