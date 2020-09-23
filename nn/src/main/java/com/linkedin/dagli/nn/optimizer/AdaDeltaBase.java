package com.linkedin.dagli.nn.optimizer;

import com.linkedin.dagli.annotation.struct.Optional;
import com.linkedin.dagli.annotation.struct.Struct;
import com.linkedin.dagli.annotation.struct.TrivialPublicConstructor;
import com.linkedin.dagli.annotation.visitor.VisitedBy;


@Struct("AdaDelta")
@TrivialPublicConstructor
@VisitedBy("OptimizerVisitor")
abstract class AdaDeltaBase extends Optimizer {
  private static final long serialVersionUID = 1;

  @Optional
  double _initialLearningRate = 1.0;

  @Optional
  double _rho = 0.95;
}
