package com.linkedin.dagli.dl4j;

import java.io.Serializable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.optimize.api.BaseTrainingListener;
import org.nd4j.linalg.dataset.api.iterator.MultiDataSetIterator;


/**
 * Listener for DL4J models that logs the loss against an evaluation dataset.
 */
class EvaluationLossListener extends BaseTrainingListener implements Serializable {
  private static final long serialVersionUID = 1;
  private static final Logger LOGGER = LogManager.getLogger();

  private final int _frequency;
  private final MultiDataSetIterator _dataSet;
  private final long _evalExampleCount;

  public EvaluationLossListener(MultiDataSetIterator dataSet, long evalExampleCount, int frequency) {
    _frequency = frequency;
    _dataSet = dataSet;
    _evalExampleCount = evalExampleCount;
  }

  @Override
  public void iterationDone(Model model, int iteration, int epoch) {
    if (iteration % _frequency == 0) {
      ComputationGraph graph = (ComputationGraph) model;

      long minibatchCount = 0;
      double scoreSum = 0;
      _dataSet.reset();
      while (_dataSet.hasNext()) {
        scoreSum += graph.score(_dataSet.next());
        minibatchCount++;
      }

      LOGGER.info(
          (scoreSum / minibatchCount) + " loss/example over " + _evalExampleCount + " held-out evaluation examples in "
              + minibatchCount + " minibatches @ iteration " + iteration);
    }
  }
}
