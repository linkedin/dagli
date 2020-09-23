package com.linkedin.dagli.math.mdarray;

import com.linkedin.dagli.math.vector.DenseVector;
import java.io.Serializable;


/**
 * A simple representation of a multidimensional array (sometimes colloquially known as a "tensor").
 *
 * {@link MDArray}s are not necessarily immutable, but their shape should be constant.
 *
 * When translating offsets to indices (and vise versa), a row-major layout is assumed (see {@link #asVector()} for
 * details); implementations that store their data in an alternative format (like column-major) must account for this in
 * methods that accept an offset.
 *
 * Dagli's multidimensional arrays primarily act as a shared abstraction over other, much more comprehensive
 * implementations, and do not themselves provide extensive linear algebra operations.
 */
public interface MDArray extends Serializable, AutoCloseable {
  /**
   * Gets the shape (dimensions) of this {@link MDArray}, expressed as a long[].  The shape is fixed for any particular
   * {@link MDArray} instance (i.e. calls to this method will always return the same value).
   *
   * As a special case, a "scalar" MDArray has 0 dimensions and thus this method will return a 0-length array.
   *
   * The caller must not modify the returned array.
   *
   * @return a (read-only) long[] representing the dimensions of the {@link MDArray}
   */
  long[] shape();

  /**
   * Gets the smallest primitive number type capable of losslessly holding the values stored in this array.
   *
   * If no primitive type is capable of losslessly representing the array's values (e.g. they are BigIntegers),
   * {@code double.class} will be returned.
   *
   * For example, for half-precision components, the returned type will be {@code float.class}.  For {0, 1} booleans,
   * the returned type will be {@code byte.class}.
   *
   * @return the smallest primitive number type capable of losslessly holding the values stored in this array
   */
  Class<? extends Number> valueType();

  /**
   * Returns a flattened, vectorized representation of this {@link MDArray}.  No data is copied; changes to the
   * {@link MDArray} will be reflected in the returned vector.
   *
   * MDArrays have a canonical row-major layout of elements that determines how the indices of an element translate
   * to its offset, which applies even if the underlying data storage of this MDArray happens to use a different
   * memory layout (e.g. column-major).
   *
   * Let index(d) be the index of a component within the d'th dimension of the original array, and let size(d) be the
   * size of the d'th dimension of the original array.  Then the index of the vector element corresponding with that
   * component is calculated as:
   * index(0) * (size(1) * size(2)...) + index(1) * (size(2) * size(3)...) + index(2) * (size(3) * size(4)...) + ...
   *
   * A consequence of this is that components whose position in the original array differs only in the last dimension
   * will have vector indices closer than those whose position differs in the second-to-last dimension, which
   * will themselves have vector indices closer than those whose position differs in the third-to-last dimension, etc.
   *
   * @return a {@link DenseVector} representation of this {@link MDArray}
   */
  default DenseVector asVector() {
    return new MDArrayAsVector(this);
  }

  /**
   * Returns an {@link MDArray} representing a subarray of this instance at the specified indices.  Any number of
   * indices (up to the number of dimensions of this instance) may be provided, corresponding to the first
   * {@code indices.length} dimensions.  The returned subarray consists of all those components with matching indices
   * in those dimensions, and the number of dimensions of the returned subarray will be
   * {@code getShape().length - indices.length}.
   *
   * For example, if the original array has shape {4, 3, 2, 1}:
   * subArrayAt() returns the original array
   * subArrayAt(2) returns an array with shape {3, 2, 1}
   * subArrayAt(2, 0) returns an array with shape {2, 1}
   * subArrayAt(2, 0, 1, 0) returns an array with shape { } (a dimensionless scalar "array")
   *
   * The returned subarray acts as a "view" on this array: the subarray is backed by this instance and no data is
   * copied.  Changes to this array will be reflected in the subarray.
   *
   * @param indices the indices of the subarray to be returned
   * @return a subarray "view" of the components of this array
   */
  default MDArray subarrayAt(long... indices) {
    return new MDSubarray(this, indices);
  }

  /**
   * Gets a component at the specified indices.
   *
   * As a special case, the value of a 0-dimension "scalar" array is retrieved by passing a 0-length indices array.
   *
   * For better performance in tight loops, reusing the same index array passed to this method avoids unnecessary
   * long[] creation (and collection).
   *
   * @param indices the indices to fetch
   * @return the value at the specified index
   */
  double getAsDouble(long... indices);

  /**
   * Gets the component at the specified linear offset.  The component corresponding to an offset is determined as it
   * the array was vectorized according to {@link #asVector()}.
   *
   * The value of a 0-dimensional scalar "array" is retrievable at offset 0.
   *
   * @param offset the offset of the component whose value should be retrieved
   * @return the value at the specified index
   */
  default double getAsDouble(long offset) {
    return getAsDouble(MDArrays.offsetToIndices(offset, shape()));
  }

  /**
   * Gets the component at the specified linear offset, possibly without checking that it is in-bounds for the MDArray.
   * Depending on the implementation, this may be slightly faster than the {@link #getAsDouble(long)}.  This method can
   * be used when the offset is already known to be in-bounds.
   *
   * The component corresponding to an offset is determined as it the array was vectorized according to
   * {@link #asVector()}.
   *
   * The value of a 0-dimensional scalar "array" is retrievable at offset 0.
   *
   * @param offset the offset of the component whose value should be retrieved
   * @return the value at the specified index
   */
  default double getAsDoubleUnsafe(long offset) {
    return getAsDouble(MDArrays.offsetToIndices(offset, shape()));
  }

  /**
   * Gets a component at the specified indices.  Non-integer values will be silently truncated.
   *
   * As a special case, the value of a 0-dimension "scalar" array is retrieved by passing a 0-length indices array.
   *
   * For better performance in tight loops, reusing the same index array passed to this method avoids unnecessary
   * long[] creation (and collection).
   *
   * @param indices the indices to fetch
   * @return the value at the specified index
   */
  long getAsLong(long... indices);

  /**
   * Gets the component at the specified linear offset.  Non-integer values will be silently truncated.  The
   * component corresponding to an offset is determined as it the array was vectorized according to {@link #asVector()}.
   *
   * The value of a 0-dimensional scalar "array" is retrievable at offset 0.
   *
   * @param offset the offset of the component whose value should be retrieved
   * @return the value at the specified index
   */
  default long getAsLong(long offset) {
    return getAsLong(MDArrays.offsetToIndices(offset, shape()));
  }

  /**
   * Gets the component at the specified linear offset, possibly without checking that it is in-bounds for the MDArray.
   * Non-integer values will be silently truncated.
   *
   * Depending on the implementation, this may be slightly faster than the {@link #getAsLong(long)}.  This method can
   * be used when the offset is already known to be in-bounds.
   *
   * The component corresponding to an offset is determined as it the array was vectorized according to
   * {@link #asVector()}.
   *
   * The value of a 0-dimensional scalar "array" is retrievable at offset 0.
   *
   * @param offset the offset of the component whose value should be retrieved
   * @return the value at the specified index
   */
  default long getAsLongUnsafe(long offset) {
    return getAsLong(MDArrays.offsetToIndices(offset, shape()));
  }

  /**
   * Releases resources associated with this {@link MDArray}, including those of any underlying data structures wrapped
   * by this instance.  The default implementation is a no-op.
   */
  @Override
  default void close() {
    // close() is a no-op by default
  }
}
