package com.linkedin.dagli.dl4j;

import java.io.Serializable;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.optimize.api.BaseTrainingListener;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;


public class FFActivationListener extends BaseTrainingListener implements Serializable {
  private static final long serialVersionUID = 1;
  private static final Logger LOGGER = LogManager.getLogger();

  private final int _frequency;
  private final int _sampleSize;

  /**
   * @param frequency the number of minibatches to wait between successive activation logging
   * @param sampleSize the (maximum) number of examples whose activatiosn should be written for each batch
   */
  public FFActivationListener(int frequency, int sampleSize) {
    _frequency = frequency;
    _sampleSize = sampleSize;
  }

  private INDArray sampleRows(INDArray source) {
    INDArrayIndex exampleRows = NDArrayIndex.interval(0, Math.min(_sampleSize, source.shape()[0]));
    return source.get(exampleRows);
  }

  @Override
  public void onForwardPass(Model model, Map<String, INDArray> activations) {
    if (model instanceof ComputationGraph) {
      ComputationGraphConfiguration conf = ((ComputationGraph) model).getConfiguration();
      if (conf.getIterationCount() % _frequency == 0) {
        activations.forEach((name, table) -> LOGGER.info(
            "First " + Math.min(_sampleSize, table.shape()[0]) + " activation rows from " + name + " @ iteration "
                + conf.getIterationCount() + ":\n" + sampleRows(table)));
      }
    }
  }
}
