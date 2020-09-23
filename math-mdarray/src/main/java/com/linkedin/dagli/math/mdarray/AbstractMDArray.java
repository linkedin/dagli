package com.linkedin.dagli.math.mdarray;

import com.linkedin.dagli.annotation.Versioned;
import java.util.stream.Collectors;
import java.util.stream.LongStream;


/**
 * Base class for {@link MDArray}s.  It is recommended that all {@link MDArray} implementations extend this class.
 */
@Versioned
public abstract class AbstractMDArray implements MDArray {
  private static final long serialVersionUID = 1;

  protected final long[] _shape;

  /**
   * Creates a new instance with the specified shape.  Note that the instance keeps a reference to the provided
   * shape array, which should not be modified subsequently.
   *
   * @param shape the shape of this instance
   */
  public AbstractMDArray(long[] shape) {
    for (long dim : shape) {
      if (dim < 1) {
        throw new IllegalArgumentException("Dimensions of an MDArray may not be negative.");
      }
    }

    _shape = shape;
  }

  @Override
  public String toString() {
    if (_shape.length == 0) { // scalar
      return Double.toString(getAsDouble(0));
    } else if (_shape.length == 1) { // vector
      return asVector().toString();
    } else {
      return "[" + LongStream.range(0, _shape[0])
          .mapToObj(this::subarrayAt)
          .map(MDArray::toString)
          .collect(Collectors.joining(", ")) + "]";
    }
  }

  @Override
  public final long[] shape() {
    return _shape;
  }
}
