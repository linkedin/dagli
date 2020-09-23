package com.linkedin.dagli.nn.activation;

import com.linkedin.dagli.annotation.struct.Struct;
import com.linkedin.dagli.annotation.struct.TrivialPublicConstructor;
import com.linkedin.dagli.annotation.visitor.VisitedBy;


/**
 * f(x_i) = e^{x_i} / \sum_j e^{x_j}
 */
@Struct("SoftMax")
@TrivialPublicConstructor
@VisitedBy("ActivationFunctionVisitor")
abstract class SoftMaxBase extends ActivationFunction {
  private static final long serialVersionUID = 1;
}
