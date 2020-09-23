package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.struct.Struct;


/**
 * Dynamic configuration for layers that provide integers or sequence of integers.
 */
@Struct("IntegerInputLayerConfig")
class IntegerInputLayerConfigBase extends DynamicLayerConfig {
  /**
   * The maximum integer value seen across all examples.  Since integers are primarily used to identify one of a set of
   * things, {@code maxIntegerValue + 1} gives the vocabulary size, needed for, e.g. embedding layers.
   */
  protected long _maxIntegerValue;
}
