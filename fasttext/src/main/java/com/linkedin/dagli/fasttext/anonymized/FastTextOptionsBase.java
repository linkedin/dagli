package com.linkedin.dagli.fasttext.anonymized;

import com.linkedin.dagli.annotation.struct.Struct;
import java.io.Serializable;

/**
 * Struct for passing FastText options.  TODO: move options out of archaic Args class and into this one
 */
@Struct("FastTextOptions")
abstract class FastTextOptionsBase implements Serializable {
  private static final long serialVersionUID = 1;

  /**
   * Legacy arguments.  Eventually these will be directly included in this class.
   */
  Args _args;

  /**
   * Whether or not the model will be multilabel (as opposed to multinomial).
   */
  boolean _multilabel;

  /**
   * The number of examples being provided.  This is useful because it avoids an extra scan of the input data to
   * determine this value.
   */
  long _exampleCount;

  /**
   * Controls whether all threads should start actively training simultaneously; if false, threads start training
   * as soon as they reach their predetermined start point in the input data file, which will result in some examples
   * being more frequently considered than others (depending on their position in that file).
   */
  boolean _synchronizedStart;
}
