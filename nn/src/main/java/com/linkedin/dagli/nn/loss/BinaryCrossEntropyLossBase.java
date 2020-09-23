package com.linkedin.dagli.nn.loss;

import com.linkedin.dagli.annotation.struct.Struct;
import com.linkedin.dagli.annotation.struct.TrivialPublicConstructor;
import com.linkedin.dagli.annotation.visitor.VisitedBy;


/**
 * Binary cross-entropy loss, also known as logistic loss, also known as log-loss, also known as negative
 * log-likelihood...
 *
 * This loss is appropriate for binary classification as well as general multilabel classification (where multiple
 * labels may apply simultaneously for a given example).  In the latter case, the problem can be viewed as an array
 * of binary classifications (one for each label), and the binary cross entropy calculates the sum over the loss
 * for all labels.
 *
 * Loss(p, q) = -\sum_i p_i * log(q_i) + (1 - p_i) * log(1 - q_i)
 * (where p(x_i) \in {0, 1} depending on whether label i applies to the example or not, and q(x_i) \in [0, 1] is
 * the predicted probability for label i, corresponding to the i'th value from the loss-constrained layer's output.)
 */
@Struct("BinaryCrossEntropyLoss")
@TrivialPublicConstructor
@VisitedBy("LossFunctionVisitor")
abstract class BinaryCrossEntropyLossBase extends LossFunction {
  private static final long serialVersionUID = 1;
}
