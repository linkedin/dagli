package com.linkedin.dagli.nn.loss;

import com.linkedin.dagli.annotation.struct.Struct;
import com.linkedin.dagli.annotation.struct.TrivialPublicConstructor;
import com.linkedin.dagli.annotation.visitor.VisitedBy;


/**
 * Absolute error loss (L1)
 *
 * Loss(p, q) = \sum_i Abs(p_i - q_i)
 */
@Struct("AbsoluteErrorLoss")
@TrivialPublicConstructor
@VisitedBy("LossFunctionVisitor")
abstract class AbsoluteErrorLossBase extends LossFunction {
  private static final long serialVersionUID = 1;
}
