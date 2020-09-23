package com.linkedin.dagli.math.distribution;

/**
 * Renormalization schemes for when entries are removed, added, or changed in a distribution.
 */
public enum Renormalization {
  /**
   * No (re)normalization is performed.
   */
  NONE,

  /**
   * All probabilities of the distribution will be scaled to maintain the same sum they had before the distribution
   * was modified.  This includes any entries that have just been modified, except those that are now probability 0; if
   * a distribution has two entries with probabilities 0.4 and 0.6, and a new entry with probability 1.0 is added, the
   * normalized probabilities will be 0.2, 0.3, and 0.5, respectively.
   *
   * Because floating point math can be imprecise, the sum of probabilities in the resulting distribution may differ
   * slightly from the original sum.
   *
   * If a modified distribution no longer has any non-zero probability entries, it remains empty and the sum of
   * probabilities will be 0, despite use of this renormalization scheme.
   */
  CONSTANT_SUM,
}
