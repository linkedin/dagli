package com.linkedin.dagli.dl4j;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.optimize.listeners.TimeIterationListener;


public class ProgressListener extends TimeIterationListener {
  private static final long serialVersionUID = 1;
  private static final Logger LOGGER = LogManager.getLogger();

  private final long _iterationsPerEpoch;
  private final long _totalEpochs;
  private final int _iterationFrequency;

  private double _errorSinceLastReport = 0;
  private long _examplesSinceLastReport = 0;

  private static long minibatchCount(long exampleCount, int minibatchSize) {
    return (exampleCount + minibatchSize - 1) / minibatchSize;
  }

  /**
   * Creates a new listener that will regularly log training status.
   *
   * @param exampleCount the number of examples
   * @param minibatchSize the size of the minibatches
   * @param epochCount the number of epochs
   * @param iterationFrequency the number of minibatches between every progress update
   */
  public ProgressListener(long exampleCount, int minibatchSize, long epochCount, int iterationFrequency) {
    super(Math.toIntExact(Math.min(Integer.MAX_VALUE, minibatchCount(exampleCount, minibatchSize) * epochCount / iterationFrequency)));
    _iterationsPerEpoch = minibatchCount(exampleCount, minibatchSize);
    _iterationFrequency = iterationFrequency;
    _totalEpochs = epochCount;
  }

  @Override
  public void iterationDone(Model model, int iteration, int epoch) {
    _errorSinceLastReport += model.score() * model.batchSize();
    _examplesSinceLastReport += model.batchSize();

    if (iteration % _iterationFrequency == 0) {
      double error = _errorSinceLastReport / _examplesSinceLastReport;
      _errorSinceLastReport = 0;
      _examplesSinceLastReport = 0;

      double completionPercentage = 100 * ((double) iteration) / (_iterationsPerEpoch * _totalEpochs);

      LOGGER.info(error + " loss/example @ " + String.format("%.2f", completionPercentage) + "% complete; iteration "
          + iteration + " (" + ((iteration % _iterationsPerEpoch) + 1) + "/" + _iterationsPerEpoch + " of epoch " + (
          epoch + 1) + "/" + _totalEpochs + ")");
      super.iterationDone(model, iteration, epoch);
    }
  }
}
