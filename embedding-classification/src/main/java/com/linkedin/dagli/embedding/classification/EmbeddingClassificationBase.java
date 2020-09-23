package com.linkedin.dagli.embedding.classification;

import com.linkedin.dagli.annotation.struct.Struct;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import java.io.Serializable;
import java.util.List;

@Struct("EmbeddingClassification")
abstract class EmbeddingClassificationBase<L> implements Serializable {
  private static final long serialVersionUID = 1;

  /**
   * An embedding of the features
   */
  DenseFloatArrayVector _featuresEmbedding;

  /**
   * The number of features that have embeddings/are known to the model
   */
  int _featuresInVocabularyCount;

  /**
   * The classification itself, expressed as a discrete distribution.
   */
  DiscreteDistribution<L> _classification;

  /**
   * A list of the embeddings of the most likely labels.  The order will match that in the _classification (most likely
   * to least likely)
   */
  List<DenseFloatArrayVector> _mostLikelyLabelEmbeddings;

  /**
   * The embeddings of the true labels provided as input to the model, if applicable
   */
  List<DenseFloatArrayVector> _trueLabelEmbeddings;
}
