package com.linkedin.dagli.evaluation;

import com.linkedin.dagli.annotation.struct.Struct;
import java.io.Serializable;


/**
 * Represents a total weight (double) paired with a total count (long) of some set of things.
 *
 * Instances of this class may be mutated before they are returned, but clients can consider them to be immutable.
 */
@Struct("WeightAndCount")
class WeightAndCountBase implements Serializable {
  private static final long serialVersionUID = 1;

  /**
   * A total weight of the things described by this instance.
   */
  double _weight;

  /**
   * The total count of things described by this instance.
   */
  long _count;

  /**
   * Add another instance to this one.
   *
   * @param other the instance to add to this one; it will not be modified
   */
  void addToThis(WeightAndCount other) {
    _weight += other._weight;
    _count += other._count;
  }
}
