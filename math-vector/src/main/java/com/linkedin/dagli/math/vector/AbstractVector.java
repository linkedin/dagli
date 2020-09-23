package com.linkedin.dagli.math.vector;

/**
 * Base class for Vector that provides standard implementations for common methods.
 */
public abstract class AbstractVector implements Vector {
  private static final long serialVersionUID = 1;

  @Override
  public String toString() {
    return Vectors.toString(this);
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Vector)) {
      return false;
    }

    return Vectors.equals(this, (Vector) other);
  }

  @Override
  public int hashCode() {
    return Vectors.hashCode(this);
  }
}
