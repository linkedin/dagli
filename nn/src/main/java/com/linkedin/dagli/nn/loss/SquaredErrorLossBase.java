package com.linkedin.dagli.nn.loss;

import com.linkedin.dagli.annotation.struct.Struct;
import com.linkedin.dagli.annotation.struct.TrivialPublicConstructor;
import com.linkedin.dagli.annotation.visitor.VisitedBy;


/**
 * Squared error loss (L2)
 *
 * Loss(p, q) = \sum_i (p_i - q_i)^2
 */
@Struct("SquaredErrorLoss")
@TrivialPublicConstructor
@VisitedBy("LossFunctionVisitor")
abstract class SquaredErrorLossBase extends LossFunction {
  private static final long serialVersionUID = 1;
}
