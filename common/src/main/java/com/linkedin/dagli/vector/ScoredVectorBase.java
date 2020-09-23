package com.linkedin.dagli.vector;

import com.linkedin.dagli.annotation.struct.Struct;
import com.linkedin.dagli.math.vector.Vector;


/**
 * Simple structure containing a vector's index in some list of candidates, its score (lower or higher may be "better",
 * depending on the algorithm producing the structure), and the vector itself.
 */
@Struct("ScoredVector")
abstract class ScoredVectorBase implements java.io.Serializable {
  private static final long serialVersionUID = 1;

  /**
   * The context-dependent index of the vector in some list of candidates.  The first entry in the list is index 0.
   */
  int _index;

  /**
   * The score of the vector.  The semantics of this depend on the algorithm producing it; either higher or lower could
   * be "better".
   */
  double _score;

  /**
   * The vector being ranked.
   */
  Vector _vector;
}
