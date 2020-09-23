package com.linkedin.dagli.nn.loss;

import com.linkedin.dagli.annotation.struct.Struct;
import com.linkedin.dagli.annotation.struct.TrivialPublicConstructor;
import com.linkedin.dagli.annotation.visitor.VisitedBy;


/**
 * Multiclass cross-entropy loss, also known as log-loss, also known as negative log-likelihood...
 *
 * Loss(p, q) = \sum_i p(x_i) * log(q(x_i))
 * (where p(x_i) is the true probability of label i, 1 or 0 for classification problems, and q(x_i) is the
 * predicted probability of label i, a [0, 1] value corresponding to the i'th value from the loss-constrained
 * layer's output).
 */
@Struct("MultinomialCrossEntropyLoss")
@TrivialPublicConstructor
@VisitedBy("LossFunctionVisitor")
abstract class MultinomialCrossEntropyLossBase extends LossFunction {
  private static final long serialVersionUID = 1;
}
