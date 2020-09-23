package com.linkedin.dagli.nn.result;

import com.linkedin.dagli.annotation.Versioned;
import com.linkedin.dagli.math.mdarray.MDArray;


/**
 * A straightforward, generic implementation of {@link NNResult} that stores its outputs as {@link MDArray}s.
 */
@Versioned
public class GenericNNResult extends NNResult {
  private static final long serialVersionUID = 1;

  private final MDArray[] _outputs;

  // private constructor for Kryo
  private GenericNNResult() {
    _outputs = null;
  }

  /**
   * Creates a new instance with the specified output arrays.
   *
   * @param outputArrays the (ordered) outputs of the neural network
   */
  public GenericNNResult(MDArray... outputArrays) {
    _outputs = outputArrays;
  }

  @Override
  protected MDArray getAsMDArray(int outputIndex) {
    return _outputs[outputIndex];
  }
}
