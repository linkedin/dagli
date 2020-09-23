package com.linkedin.dagli.tuple;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;


/**
 * Base class for tuples providing common methods.
 */
abstract class AbstractTuple implements Tuple {
  private static final long serialVersionUID = 1;

  static final Comparator<Comparable<Object>> ELEMENT_COMPARATOR = Comparator.nullsFirst(Comparator
      .naturalOrder());

  @Override
  public int hashCode() {
    int hash = 0x32a9e22d;
    for (int i = 0; i < this.size(); i++) {
      hash = fmix32(this.get(i).hashCode() + hash);
    }
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Tuple)) {
      return false;
    }
    Tuple o = (Tuple) obj;

    if (this.size() != o.size()) {
      return false;
    }

    for (int i = 0; i < this.size(); i++) {
      if (!Objects.equals(this.get(i), o.get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString() {
    return Arrays.toString(toArray());
  }

  static int fmix32(int h) {
    h ^= h >>> 16;
    h *= 0x85ebca6b;
    h ^= h >>> 13;
    h *= 0xc2b2ae35;
    h ^= h >>> 16;
    return h;
  }
}
