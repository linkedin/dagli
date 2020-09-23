package com.linkedin.dagli.nn.activation;

import com.linkedin.dagli.annotation.struct.Struct;
import com.linkedin.dagli.annotation.struct.TrivialPublicConstructor;
import com.linkedin.dagli.annotation.visitor.VisitedBy;


/**
 * f(x) = x
 */
@Struct("Identity")
@TrivialPublicConstructor
@VisitedBy("ActivationFunctionVisitor")
abstract class IdentityBase extends ActivationFunction {
  private static final long serialVersionUID = 1;
}
