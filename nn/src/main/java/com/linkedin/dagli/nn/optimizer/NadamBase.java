package com.linkedin.dagli.nn.optimizer;

import com.linkedin.dagli.annotation.struct.Optional;
import com.linkedin.dagli.annotation.struct.Struct;
import com.linkedin.dagli.annotation.struct.TrivialPublicConstructor;
import com.linkedin.dagli.annotation.visitor.VisitedBy;


@Struct("Nadam")
@TrivialPublicConstructor
@VisitedBy("OptimizerVisitor")
abstract class NadamBase extends Optimizer {
  private static final long serialVersionUID = 1;

  @Optional
  double _learningRate = 0.002;

  @Optional
  double _beta1 = 0.9;

  @Optional
  double _beta2 = 0.999;
}
