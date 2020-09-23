package com.linkedin.dagli.nn.activation;

import com.linkedin.dagli.annotation.struct.Struct;
import com.linkedin.dagli.annotation.struct.TrivialPublicConstructor;
import com.linkedin.dagli.annotation.visitor.VisitedBy;


/**
 * f(x) = max(0, x)
 *
 * Used for rectified linear units (ReLUs).
 */
@Struct("RectifiedLinear")
@TrivialPublicConstructor
@VisitedBy("ActivationFunctionVisitor")
abstract class RectifiedLinearBase extends ActivationFunction {
  private static final long serialVersionUID = 1;
}
