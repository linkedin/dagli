package com.linkedin.dagli.nn.activation;

import com.linkedin.dagli.annotation.struct.Struct;
import com.linkedin.dagli.annotation.struct.TrivialPublicConstructor;
import com.linkedin.dagli.annotation.visitor.VisitedBy;


/**
 * f(x) = 1 / (1 + e^{-x})
 */
@Struct("Sigmoid")
@TrivialPublicConstructor
@VisitedBy("ActivationFunctionVisitor")
abstract class SigmoidBase extends ActivationFunction {
  private static final long serialVersionUID = 1;
}
