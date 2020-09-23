package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.struct.Struct;


@Struct("AttentionLayerConfig")
public class AttentionLayerConfigBase extends DynamicLayerConfig {
  /**
   * The number of queries (if the vectors of the input sequence are used as the queries, this will be the length of
   * the input sequence).
   *
   * This is also the length of the outputted sequence.
   */
  protected long _queryCount;

  /**
   * The number of heads.
   */
  protected long _headCount;

  /**
   * The size of each head.
   */
  protected long _headSize;

  /**
   * The size of each vector in the outputted sequence.
   */
  protected long _outputSize;

  /**
   * Whether or not to project the queries, keys, values and outputs via learned projection matrices.  If projection
   * is not use the legal values for the other parameters will be heavily constrained.
   */
  protected boolean _isProjected;
}
