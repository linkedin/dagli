package com.linkedin.dagli.nn.optimizer;

import com.linkedin.dagli.annotation.struct.Optional;
import com.linkedin.dagli.annotation.struct.Struct;
import com.linkedin.dagli.annotation.struct.TrivialPublicConstructor;
import com.linkedin.dagli.annotation.visitor.VisitedBy;


@Struct("Adam")
@TrivialPublicConstructor
@VisitedBy("OptimizerVisitor")
abstract class AdamBase extends Optimizer {
  private static final long serialVersionUID = 1;

  @Optional
  double _learningRate = 0.001;

  @Optional
  double _beta1 = 0.9;

  @Optional
  double _beta2 = 0.999;
}
