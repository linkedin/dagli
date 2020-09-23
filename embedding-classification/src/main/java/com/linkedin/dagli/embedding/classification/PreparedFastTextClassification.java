package com.linkedin.dagli.embedding.classification;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import java.io.Serializable;


/**
 * A prepared FastText model that predicts a distribution over candidate labels.
 *
 * @param <L>
 */
@ValueEquality
public class PreparedFastTextClassification<L extends Serializable>
    extends Embedded.Classifier<L, CharSequence, PreparedFastTextClassification<L>> {
  private static final long serialVersionUID = 1;

  public PreparedFastTextClassification(FastTextInternal.Model<L> classifier, int topK) {
    super(classifier, topK);
  }

  @Override
  @SuppressWarnings("unchecked")
  public FastTextInternal.Model<L> getModel() {
    return (FastTextInternal.Model) super.getModel();
  }
}
