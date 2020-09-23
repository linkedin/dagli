package com.linkedin.dagli.embedding.classification;

import com.linkedin.dagli.math.distribution.DiscreteDistributions;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.util.cloneable.AbstractCloneable;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import com.linkedin.dagli.math.distribution.LabelProbability;
import com.linkedin.dagli.math.distribution.ArrayDiscreteDistribution;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


/**
 * Abstract type for averaged embedding classifiers.  This class should be derived only by subclasses within Dagli, but
 * it is public because it is also used in the API to encapsulate all classifiers of this type.
 *
 * To implement an averaged embedding classifier, one simply extends this class and overrides the abstract methods.
 *
 * @param <T> the type of thing that is a "feature" in the model
 * @param <L> the type of thing that is a label in the model
 */
public abstract class AbstractEmbeddingClassifier<L extends Serializable, T>
    extends AbstractCloneable<AbstractEmbeddingClassifier<L, T>> implements Serializable {
  private static final long serialVersionUID = 1;

  private final L[] _labels;
  private final DenseFloatArrayVector[] _labelEmbeddings;
  private final int _labelEmbeddingDimensions;
  private transient HashMap<L, DenseFloatArrayVector> _labelEmbeddingMap;
  private transient ThreadLocal<float[]> _rawScores;

  @Override
  public int hashCode() {
    return Arrays.hashCode(_labels) + Arrays.hashCode(_labelEmbeddings) + Integer.hashCode(_labelEmbeddingDimensions);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof AbstractEmbeddingClassifier)) {
      return false;
    }

    AbstractEmbeddingClassifier<?, ?> other = (AbstractEmbeddingClassifier<?, ?>) obj;
    return Arrays.equals(this._labels, other._labels) && Arrays.equals(this._labelEmbeddings, other._labelEmbeddings)
        && this._labelEmbeddingDimensions == other._labelEmbeddingDimensions;
  }

  protected int getLabelEmbeddingDimensions() {
    return _labelEmbeddingDimensions;
  }

  protected L[] getLabels() {
    return _labels;
  }

  protected DenseFloatArrayVector[] getLabelEmbeddings() {
    return _labelEmbeddings;
  }

  protected class FeaturesEmbeddingResult {
    private final DenseFloatArrayVector _featuresEmbedding;
    private final int _featuresInVocabularyCount;

    public FeaturesEmbeddingResult(DenseFloatArrayVector featuresEmbedding, int featuresInVocabularyCount) {
      _featuresEmbedding = featuresEmbedding;
      _featuresInVocabularyCount = featuresInVocabularyCount;
    }

    /**
     * Gets the vector embedding of the features.
     *
     * @return a {@link DenseFloatArrayVector} containing an embedding of the features, as defined by the model
     */
    public DenseFloatArrayVector getFeaturesEmbedding() {
      return _featuresEmbedding;
    }

    /**
     * Gets the number of in-vocabulary features with respect to the model.
     *
     * @return the number of in-vocabulary features, as determined by the model
     */
    public int getFeaturesInVocabularyCount() {
      return _featuresInVocabularyCount;
    }
  }

  /**
   * Gets the embedding of the features.
   *
   * @param features the features of the example
   * @return a FeaturesEmbeddingResult containing an embedding of the features, as defined by the model
   */
  protected abstract FeaturesEmbeddingResult embedFeatures(Iterable<? extends T> features);
  protected abstract void distributionalize(float[] labelProbabilities);

  public AbstractEmbeddingClassifier(L[] labels, DenseFloatArrayVector[] labelEmbeddings) {
    _labels = labels;
    _labelEmbeddings = labelEmbeddings;
    _labelEmbeddingDimensions = labelEmbeddings[0].getArray().length;

    _labelEmbeddingMap = buildLabelEmbeddingMap(labels, labelEmbeddings);
     _rawScores = ThreadLocal.withInitial(() -> new float[_labels.length]);
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    _labelEmbeddingMap = buildLabelEmbeddingMap(_labels, _labelEmbeddings);
    _rawScores = ThreadLocal.withInitial(() -> new float[_labels.length]);
  }

  private static <L> HashMap<L, DenseFloatArrayVector> buildLabelEmbeddingMap(L[] labels, DenseFloatArrayVector[] labelEmbeddings) {
    return new HashMap<>(new Object2ObjectArrayMap<>(labels, labelEmbeddings));
  }

  public DenseFloatArrayVector embedLabel(L label) {
    return _labelEmbeddingMap.get(label);
  }

  private static class ScoredLabel implements Comparable<ScoredLabel> {
    int _labelIndex;
    float _score;

    public ScoredLabel(float score, int labelIndex) {
      _score = score;
      _labelIndex = labelIndex;
    }

    @Override
    public int compareTo(ScoredLabel o) {
      return (int) Math.signum(this._score - o._score);
    }
  }

  private void rawScores(DenseFloatArrayVector embedding, float[] targetRawScores) {
    for (int i = 0; i < _labelEmbeddings.length; i++) {
      targetRawScores[i] = (float) embedding.dotProduct(_labelEmbeddings[i]);
    }
  }

  private PriorityQueue<ScoredLabel> topK(float[] probabilities, int k) {
    PriorityQueue<ScoredLabel> topK = new PriorityQueue<>(k);

    for (int i = 0; i < _labelEmbeddings.length; i++) {
      float score = probabilities[i];
      if (topK.size() < k) {
        topK.add(new ScoredLabel(score, i));
      } else if (score > topK.peek()._score) {
        // optimization: reuse ScoredLabel instance
        ScoredLabel instance = topK.poll();
        instance._labelIndex = i;
        instance._score = score;
        topK.add(instance);
      }
    }

    return topK;
  }

  private DiscreteDistribution<L> predict(DenseFloatArrayVector featuresEmbedding, int k) {
    if (k == 0) {
      return DiscreteDistributions.empty();
    }

    float[] rawScores = _rawScores.get();
    rawScores(featuresEmbedding, rawScores);
    distributionalize(rawScores);

    return topK(rawScores, k).stream()
        .map(sl -> new LabelProbability<>(_labels[sl._labelIndex], sl._score))
        .collect(ArrayDiscreteDistribution.collector());
  }

  public EmbeddingClassification<L> createResult(Iterable<? extends T> features, int topK,
      Iterable<? extends L> trueLabels) {
    FeaturesEmbeddingResult feResult = embedFeatures(features);
    DenseFloatArrayVector featuresEmbedding = feResult.getFeaturesEmbedding();
    DiscreteDistribution<L> prediction = predict(featuresEmbedding, topK);
    List<DenseFloatArrayVector> predictedLabelEmbeddings =
        prediction.stream().map(lp -> embedLabel(lp.getLabel())).collect(Collectors.toList());
    List<DenseFloatArrayVector> trueLabelEmbeddings = trueLabels == null ? null
        : StreamSupport.stream(trueLabels.spliterator(), false)
            .map(this::embedLabel)
            .collect(Collectors.toList());

    return EmbeddingClassification.Builder
        .<L>setFeaturesEmbedding(featuresEmbedding)
        .setFeaturesInVocabularyCount(feResult.getFeaturesInVocabularyCount())
        .setClassification(prediction)
        .setMostLikelyLabelEmbeddings(predictedLabelEmbeddings)
        .setTrueLabelEmbeddings(trueLabelEmbeddings)
        .build();
  }

  /**
   * Gets the predicted {@link DiscreteDistribution} over the labels for the given inputs.
   *
   * @param features the features of the example
   * @return an inferred {@link DiscreteDistribution} for the example
   */
  protected DiscreteDistribution<L> predictDistribution(Iterable<? extends T> features, int topK) {
    return createResult(features, topK, null).getClassification();
  }

  /**
   * Gets the embeddings of the most-likely predicted labels, in decreasing order of probability.
   *
   * @param features the features of the example
   * @return the embedding of the most-likely predicted labels
   */
  protected List<DenseFloatArrayVector> getMostLikelyLabelEmbeddings(Iterable<? extends T> features, int topK) {
    return createResult(features, topK, null).getMostLikelyLabelEmbeddings();
  }

  /**
   * Gets the embeddings of the given labels, in decreasing order of probability, or null if null is passed
   * to the method.
   *
   * @param labels the labels to embed, or null
   * @return the embedding of the labels, or null if the provided labels iterable was null
   */
  protected List<DenseFloatArrayVector> getLabelEmbeddings(Iterable<? extends L> labels) {
    if (labels == null) {
      return null;
    } else {
      final ArrayList<DenseFloatArrayVector> result;
      if (labels instanceof Collection) {
        result = new ArrayList<>(((Collection) labels).size());
      } else {
        result = new ArrayList<>();
      }

      for (L label : labels) {
        result.add(embedLabel(label));
      }
      return result;
    }
  }
}
