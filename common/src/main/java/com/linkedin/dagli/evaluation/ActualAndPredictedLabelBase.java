package com.linkedin.dagli.evaluation;

import com.linkedin.dagli.annotation.struct.Struct;
import java.io.Serializable;


/**
 * Represents the pair of an actual, true label for an example and a corresponding predicted label.  This is useful
 * when, e.g., using a {@link java.util.Map} to represent a confusion matrix.
 */
@Struct("ActualAndPredictedLabel")
class ActualAndPredictedLabelBase implements Serializable {
  private static final long serialVersionUID = 1;

  /**
   * The actual, "true" label of an example.
   */
  Object _actualLabel;

  /**
   * The label predicted for an example.
   */
  Object _predictedLabel;
}
