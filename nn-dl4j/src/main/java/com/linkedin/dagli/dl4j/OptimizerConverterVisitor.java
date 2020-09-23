package com.linkedin.dagli.dl4j;

import com.linkedin.dagli.nn.optimizer.AdaDelta;
import com.linkedin.dagli.nn.optimizer.AdaGrad;
import com.linkedin.dagli.nn.optimizer.AdaMax;
import com.linkedin.dagli.nn.optimizer.Adam;
import com.linkedin.dagli.nn.optimizer.Nadam;
import com.linkedin.dagli.nn.optimizer.OptimizerVisitor;
import com.linkedin.dagli.nn.optimizer.RMSProp;
import com.linkedin.dagli.nn.optimizer.StochasticGradientDescent;
import org.nd4j.linalg.learning.config.IUpdater;
import org.nd4j.linalg.learning.config.RmsProp;
import org.nd4j.linalg.learning.config.Sgd;


/**
 * Visitor for converting a Dagli NN optimizer to the corresponding DL4J {@link IUpdater}.
 */
class OptimizerConverterVisitor implements OptimizerVisitor<IUpdater> {
  @Override
  public IUpdater visit(Adam visitee) {
    return org.nd4j.linalg.learning.config.Adam.builder()
        .beta1(visitee.getBeta1())
        .beta2(visitee.getBeta2())
        .learningRate(visitee.getLearningRate())
        .build();
  }

  @Override
  public IUpdater visit(Nadam visitee) {
    return org.nd4j.linalg.learning.config.Nadam.builder()
        .beta1(visitee.getBeta1())
        .beta2(visitee.getBeta2())
        .learningRate(visitee.getLearningRate())
        .build();
  }

  @Override
  public IUpdater visit(RMSProp visitee) {
    return RmsProp.builder().learningRate(visitee.getLearningRate()).rmsDecay(visitee.getRho()).build();
  }

  @Override
  public IUpdater visit(StochasticGradientDescent visitee) {
    return Sgd.builder().learningRate(visitee.getLearningRate()).build();
  }

  @Override
  public IUpdater visit(AdaDelta visitee) {
    // note that DL4J does not support setting an initial learning rate
    return org.nd4j.linalg.learning.config.AdaDelta.builder().rho(visitee.getRho()).build();
  }

  @Override
  public IUpdater visit(AdaMax visitee) {
    org.nd4j.linalg.learning.config.AdaMax adamax = new org.nd4j.linalg.learning.config.AdaMax();
    adamax.setBeta1(visitee.getBeta1());
    adamax.setBeta2(visitee.getBeta2());
    adamax.setLearningRate(visitee.getLearningRate());
    return adamax;
  }

  @Override
  public IUpdater visit(AdaGrad visitee) {
    return org.nd4j.linalg.learning.config.AdaGrad.builder().learningRate(visitee.getLearningRate()).build();
  }
}
