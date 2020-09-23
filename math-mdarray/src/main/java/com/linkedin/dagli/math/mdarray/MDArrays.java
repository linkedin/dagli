package com.linkedin.dagli.math.mdarray;

import com.linkedin.dagli.util.invariant.Arguments;
import java.util.Arrays;


/**
 * Static utility methods for {@link MDArray}s.
 */
public abstract class MDArrays {
  private MDArrays() { }

  /**
   * Gets the number of values in an {@link MDArray} with the specified shape.
   *
   * @param shape the shape of the MDArray
   * @return the number of values the MDArray stores (including 0s)
   */
  public static long elementCount(long[] shape) {
    long product = 1;
    for (long l : shape) {
      product *= l;
    }
    return product;
  }

  /**
   * Calculates the indices of an element in an {@link MDArray} given its offset.  An {@link IllegalArgumentException}
   * will be thrown if it falls outside the bounds of the array.
   *
   * @param offset the canonical offset of the element
   * @param shape the shape of the {@link MDArray}
   * @return the indices of the element at the given offset
   */
  public static long[] offsetToIndices(long offset, long[] shape) {
    // the coordinates in the MDArray for a given offset can be calculated by successively dividing the offset by the
    // dimensions (starting with the last dimension) and getting the remainder (which is the index at that dimension)
    long residual = offset;
    long[] indices = new long[shape.length];

    for (int i = indices.length - 1; i >= 0; i--) {
      long newResidual = residual / shape[i]; // newResidual should be 0 when i == 0 unless offset is out-of-bounds
      indices[i] = residual % shape[i]; // get remainder; could test if "residual - (shape[i] * newResidual)" is faster
      residual = newResidual;
    }

    if (residual != 0) {
      throw new IllegalArgumentException("The offset " + offset + " is outside the bounds of the MDArray");
    }

    return indices;
  }

  /**
   * Calculates the canonical offset of an element located at a given array of indices.
   *
   * @param indices the indices of the element whose offset should be calculated
   * @param shape the shape of the {@link MDArray} containing the element
   * @return the element's 0-based offset
   */
  public static long indicesToOffset(long[] indices, long[] shape) {
    long subarraySize = 1;
    long result = 0;
    for (int i = shape.length - 1; i >= 0; i--) {
      result += indices[i] * subarraySize;
      subarraySize *= shape[i];
    }

    return result;
  }

  /**
   * Checks if the provided indices fall within a shape.
   *
   * @param indices the indices to check
   * @param shape the shape
   * @return true if the indices are a valid location within an {@link MDArray} with the given shape
   */
  public static boolean validIndices(long[] indices, long[] shape) {
    if (indices.length != shape.length) {
      return false;
    }

    for (int i = 0; i < indices.length; i++) {
      if (indices[i] < 0 || indices[i] >= shape[i]) {
        return false;
      }
    }

    return true;
  }

  /**
   * Checks that the provided indices are valid in an array of a given shape, throwing an
   * {@link IllegalArgumentException} if they are not.
   *
   * @param indices the indices to check
   * @param shape the shape of the array
   */
  public static void checkValidIndices(long[] indices, long[] shape) {
    Arguments.check(validIndices(indices, shape),
        () -> "The indices " + Arrays.toString(indices) + " are not valid for an MDArray with shape "
            + Arrays.toString(shape));
  }

  /**
   * Concatenates two arrays of longs; this is primarily useful for manipulating indices and shapes.
   *
   * @param prefix the indices that will comprise the first part of the resulting array
   * @param suffix the indices that will comprise the second part of the resulting array
   * @return an array of the elements of {@code prefix} followed by {@code suffix}
   */
  public static long[] concatenate(long[] prefix, long[] suffix) {
    long[] res = Arrays.copyOf(prefix, prefix.length + suffix.length);
    System.arraycopy(suffix, 0, res, prefix.length, suffix.length);
    return res;
  }
}
