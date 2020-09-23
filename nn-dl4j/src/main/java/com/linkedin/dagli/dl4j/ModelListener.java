package com.linkedin.dagli.dl4j;

import java.io.Serializable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.optimize.api.BaseTrainingListener;


/**
 * Listener for DL4J models that logs the model architecture and parameters.
 */
public class ModelListener extends BaseTrainingListener implements Serializable {
  private static final long serialVersionUID = 1;
  private static final Logger LOGGER = LogManager.getLogger();

  private final int _frequency;

  public ModelListener(int frequency) {
    _frequency = frequency;
  }

  @Override
  public void iterationDone(Model model, int iteration, int epoch) {
    if (iteration % _frequency == 0) {
      model.paramTable().forEach(
        (name, table) -> LOGGER.info("Parameter table " + name + " @ iteration " + iteration + ":\n" + table));
    }
  }
}
