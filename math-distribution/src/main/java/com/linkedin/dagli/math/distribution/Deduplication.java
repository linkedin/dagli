package com.linkedin.dagli.math.distribution;

/**
 * Operations that modify a discrete distribution may result in multiple entries with the same label (event).  Because a
 * discrete distribution effectively maps a label to a probability, such duplicative entries must be resolved.  This
 * enum controls <i>how</i> they should be resolved.
 */
public enum Deduplication {
  /**
   * You assert that there are no duplicates.  Checking for duplicate labels is relatively expensive (typically
   * requiring use of either a hashtable or O(n^2) equality checks) so, if you know for certain that no duplicate labels
   * will result from an operation, this will improve efficiency.  Java asserts will be used to check this; asserts are
   * typically enabled in a JVM during testing but not production/at scale.
   */
  NONE,

  /**
   * If multiple entries (label and probability pairs) share the same label, the highest probability associated with
   * that label will be used.
   *
   * For example, if the distribution is "A" -> 0.3 and "B" -> 0.5, and we map both events' labels to "C", the resulting
   * distribution would be "C" -> 0.5.
   */
  MAX,

  /**
   * If multiple entries (label and probability pairs) share the same label, the label is associated with the <b>sum</b>
   * of all probabilities associated with that label.
   *
   * For example, if the distribution is "A" -> 0.3 and "B" -> 0.5, and we map all labels to "C", the resulting
   * distribution would be "C" -> 0.8.
   */
  MERGE,
}
