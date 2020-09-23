package com.linkedin.dagli.nn.layer;

/**
 * RNN layers can process a sequence in both the forward and backward direction with their results combined in the way
 * specified by this enum.  Both the forward and backward directions will have their own independent sets of parameters.
 */
public enum Bidirectionality {
  /**
   * The RNN layer is processed in the forward direction only.
   */
  FORWARD_ONLY,

  /**
   * The output of the layer is the concatenated outputs of the forward and backward passes, such that the output vector
   * at each timestep is twice as long as the layer's specified unit count.
   */
  CONCATENATED,

  /**
   * The output of the layer is calculated by summing the outputs of the forward and backward passes together.
   */
  SUMMED,

  /**
   * The output of the layer is calculated by element-wise multiplication of the outputs of the forward and backward
   * passes (Hadamard product).
   */
  MULITIPLIED,

  /**
   * The output of the layer is calculated as the arithmetic mean of the outputs of the forward and backward passes.
   */
  MEAN
}
