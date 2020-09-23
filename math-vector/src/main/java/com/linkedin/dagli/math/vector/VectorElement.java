package com.linkedin.dagli.math.vector;

import com.linkedin.dagli.math.hashing.MurmurHash3;
import java.io.Serializable;


/**
 * Element (index-value pair) within a {@link Vector}.  This interface has the widest types (long, double) possible to
 * permit the greatest flexibility of vector implementations, but note that may instances may internally use other,
 * narrower data types for efficiency and may have alternative means for retrieving these directly.
 *
 * Elements are naturally ordered (via {@link #compareTo(VectorElement)}) by their value first, and then their index if
 * the indices match (which should never be the case within a single vector!)
 */
public final class VectorElement implements Comparable<VectorElement>, Serializable {
  private final long _index;
  private final double _value;

  /**
   * Gets the index of the vector element.  This may be negative.
   *
   * @return the index, possibly negative, of the vector element.
   */
  public long getIndex() {
    return _index;
  }

  /**
   * Gets the value of the vector element.
   *
   * @return the value of the vector element.
   */
  public double getValue() {
    return _value;
  }

  /**
   * Creates a new {@link VectorElement}.
   *
   * @param index index of the value within the vector.
   * @param value a double-precision float of the value at this index.
   */
  public VectorElement(long index, double value) {
    _index = index;
    _value = value;
  }

  /**
   * Private no-args constructor specifically for the benefit of Kryo
   */
  private VectorElement() {
    this(0, 0);
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof VectorElement)) {
      return false;
    }

    VectorElement elem = (VectorElement) other;
    return this._index == elem._index && this._value == elem._value;
  }

  @Override
  public int hashCode() {
    return (int) hashCode(_index, _value);
  }

  /**
   * Produces a hash of a vector element expressed as an index and value.
   *
   * @param index the index of the element
   * @param value the value of the element
   * @return the hash of the vector element
   */
  public static long hashCode(long index, double value) {
    long hash = MurmurHash3.fmix64(Double.doubleToLongBits(value));
    return MurmurHash3.fmix64(index ^ hash);
  }

  @Override
  public int compareTo(VectorElement o) {
    if (this._value != o._value) {
      return Double.compare(this._value, o._value);
    } else {
      return Long.compare(this._index, o._index);
    }
  }

  @Override
  public String toString() {
    return this._index + ": " + this._value;
  }
}
