package com.linkedin.dagli.nn.activation;

import com.linkedin.dagli.annotation.struct.Struct;
import com.linkedin.dagli.annotation.struct.TrivialPublicConstructor;
import com.linkedin.dagli.annotation.visitor.VisitedBy;


/**
 * tanh(x) = (e^{2x} - 1) / (e^{2x} + 1)
 */
@Struct("HyperbolicTangent")
@TrivialPublicConstructor
@VisitedBy("ActivationFunctionVisitor")
abstract class HyperbolicTangentBase extends ActivationFunction {
  private static final long serialVersionUID = 1;
}
