package com.linkedin.dagli.dl4j;

import java.io.Serializable;
import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.optimize.api.BaseTrainingListener;


class LossAccumulationListener extends BaseTrainingListener implements Serializable {
  private static final long serialVersionUID = 1;

  private double _accumulatedLoss = 0;

  public double getAndClearLoss() {
    double res = _accumulatedLoss;
    _accumulatedLoss = 0;
    return res;
  }

  @Override
  public void iterationDone(Model model, int iteration, int epoch) {
    _accumulatedLoss += model.score() * model.batchSize();
  }
}
