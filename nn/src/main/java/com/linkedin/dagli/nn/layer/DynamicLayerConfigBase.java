package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.struct.Struct;
import com.linkedin.dagli.math.mdarray.MDArrays;


/**
 * Some properties of a {@link NNLayer} have dependencies on the encapsulating DAG (e.g. hyperparameters provided by
 * constant-result inputs to the neural network) and cannot be determined until the neural network is initialized during
 * DAG execution.
 */
@Struct("DynamicLayerConfig")
class DynamicLayerConfigBase {
  /**
   * The shape of the tensor outputted by this layer for each example (it does not include an "example" dimension).
   * For layers producing scalars, this will be { 1 }.  For vectors and matrices, the first dimension is the number of
   * columns, the second is the number of rows, e.g. { 5 } is a row vector with 5 elements.
   */
  protected long[] _outputShape;

  /**
   * @return the number elements in the tensor outputted by this layer; this is a function of its output shape
   */
  public long getOutputElementCount() {
    assert _outputShape.length > 0; // "scalars" with empty shape arrays are not valid in DynamicLayerConfig
    return MDArrays.elementCount(_outputShape);
  }
}
