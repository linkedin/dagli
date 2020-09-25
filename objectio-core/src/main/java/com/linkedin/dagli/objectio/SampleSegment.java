package com.linkedin.dagli.objectio;

import java.io.Serializable;


/**
 * SampleSegment defines a sample from a given set of items, i.e. a subset selected at random.
 *
 * Specifically, every item is mapped, uniformly at random, to a number between 0 (inclusive) and 1 (exclusive).  If
 * that item's number falls within the segment's range, it is included in the same; otherwise it is excluded.
 *
 * The amount of data in a sample will be proportional in expectation to the size of the segment.  So, for example,
 * a [0, 0.2) segment will have approximately 20% of the data.  Depending on how the sampling is performed, this may be
 * exact.
 *
 * Samples also have a "seed" value used to produce the sample.  If two samples are made from the same data set with
 * the same seed, the following will hold:
 * (1) If the two samples are mutually exclusive (e.g. with segments [0, 0.2) and [0.2, 0.4)) the two samples will be
 *     disjoint, e.g. have no items in common.
 * (2) If the two samples cover the full unit line (e.g. with segments [0, 0.3) and [0.3, 1)) the union of the two
 *     samples will contain all the elements in the original set (i.e. the sampling will be exhaustive)
 * (3) If the samples overlap, the portion of items they have in common will be their amount of overlap in expectation.
 *     For example, segments [0, 0.4) and [0.3, 0.5) would have about 10% (of the items of the original set) in common.
 * (4) If the samples have the same segments (e.g. [0.4, 0.5) and [0.4, 0.5)) they will have exactly the same items.
 *
 * SampleSegment can be passed to ObjectReader.sample(...) to obtain a sampled ObjectReader.  This can be very handy
 * for, e.g. splitting data into training and testing sets.
 */
public class SampleSegment implements Serializable {
  private static final long serialVersionUID = 1;

  private final double _start;
  private final double _end;
  private final boolean _complement; // if true, the segment includes every value NOT in the range of [_start, _end).
  private final long _seed;

  /**
   * Creates a new [0, 1) segment with random seed 0.
   */
  public SampleSegment() {
    this(0, 1, 0);
  }

  /**
   * Creates a new segment with a default random seed.
   *
   * @param start a [0, 1] value specifying the start of the segment, inclusive.
   * @param end a [0, 1] value specified the end of the segment, exclusive.  Must be {@code >= start}.
   */
  public SampleSegment(double start, double end) {
    this(start, end, 0);
  }

  /**
   * Creates a new segment.
   *
   * @param start a [0, 1] value specifying the start of the segment, inclusive.
   * @param end a [0, 1] value specified the end of the segment, exclusive.  Must be {@code >= start}.
   * @param seed a random seed that should be used for sampling.
   */
  public SampleSegment(double start, double end, long seed) {
    this(start, end, seed, false);
  }

  /**
   * Creates a new segment.
   *
   * @param start a [0, 1] value specifying the start of the segment, inclusive.
   * @param end a [0, 1] value specified the end of the segment, exclusive.  Must be >= start.
   * @param seed a random seed that should be used for sampling.
   * @param complement if true, this segment corresponds to every value NOT in the range of [start, end).
   */
  private SampleSegment(double start, double end, long seed, boolean complement) {
    _start = start;
    _end = end;
    _complement = complement;
    _seed = seed;
  }

  /**
   * Gets the proportion of items that will be sampled by this segment in expectation.  This corresponds to the length
   * of the range defined by the segment; e.g. a segment [0.3, 0.4) will sample 10% (0.1) of elements in expectation.
   *
   * @return the expected proportion of items that will be within this sample.
   */
  public double getProportion() {
    double size = _end - _start;
    return _complement ? 1.0 - size : size;
  }

  //public double getStart() {
  //  return _start;
  //}

  //public double getEnd() {
  //  return _end;
  //}

  /**
   * Gets the seed used by this segment.
   *
   * @return the seed
   */
  public long getSeed() {
    return _seed;
  }

  /*
  public SampleSegment withStart(double start) {
    return new SampleSegment(start, _end, _seed, _complement);
  }

  public SampleSegment withEnd(double end) {
    return new SampleSegment(_start, end, _seed, _complement);
  }
  */

  /**
   * Gets a copy of this SampleSegment that will use a different seed.
   *
   * @param seed the new seed to use
   * @return a copy of this instance that will use the specified seed
   */
  public SampleSegment withSeed(long seed) {
    return new SampleSegment(_start, _end, seed, _complement);
  }

  /**
   * Checks whether or not this segment "contains" a sampled item with the specified sample value.
   *
   * @param sampleValue the item's [0, 1) sample value; this should be drawn uniformly at random
   * @return true if this item is part of the sample specified by this segment, false otherwise
   */
  public boolean contains(double sampleValue) {
    boolean inRange = sampleValue >= _start && sampleValue < _end;
    return inRange ^ _complement;
  }

  /**
   * Gets the complement ("inverse") of this segment; the new segment will contain all (and only) the sampled items that
   * this one did not.
   *
   * @return a SampleSegment that is the complement of this one
   */
  public SampleSegment complement() {
    return new SampleSegment(_start, _end, _seed, !_complement);
  }
}
