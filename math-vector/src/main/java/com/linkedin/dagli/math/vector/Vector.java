package com.linkedin.dagli.math.vector;

import it.unimi.dsi.fastutil.Size64;
import java.io.Serializable;
import java.util.Arrays;
import java.util.OptionalLong;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * A universal interface for Vectors.
 * Note that, in a vector, there is a value for every possible index.
 * The value for an index that has not been assigned a value is 0.
 */
public interface Vector extends Iterable<VectorElement>, Size64, Serializable {
  /**
   * Gets the smallest primitive type (i.e. {@link Class#isPrimitive()} will return true) of number that is known to be
   * able to "losslessly" store the values in this vector.  This will typically be the same primitive type as used by
   * the vector for internal storage of these values.
   *
   * For example, if the vector internally stores its values as bits (with values 0 or 1), the returned class would be
   * {@code byte.class}.
   *
   * If the smallest possible primitive type is not known (or would be expensive to discover) a "larger" type may be
   * used.  If a vector's values cannot be losslessly stored in a primitive type (for example, an unsigned long or a
   * quad precision floating point type), {@code double.class} should be returned.
   *
   * @return the primitive type of value
   */
  Class<? extends Number> valueType();

  /**
   * Gets the value for a particular index.
   *
   * @param index the index of the element whose value will be returned
   * @return The associated value.  Never throws an "out of range"-type exception: all indices have a value.
   */
  double get(long index);

  /**
   * Counts the number of non-zeros in the vector.  This is also the number of elements returned by the iterator().
   *
   * @return The number of non-zeros in the vector.
   */
  @Override
  default long size64() {
    long[] result = new long[1];
    forEach((index, value) -> result[0]++);
    return result[0];
  }

  /**
   * Finds the highest index of any non-zero element in the vector.
   *
   * If there are no non-zero elements in the vector, an empty {@link OptionalLong} will be returned.
   *
   * @return an {@link OptionalLong} containing the highest index of any non-zero element in the vector, or empty if
   *         the vector has no non-zero elements.
   */
  default OptionalLong maxNonZeroElementIndex() {
    VectorElementIterator iter = reverseIterator();
    return iter.hasNext() ? OptionalLong.of(iter.next().getIndex()) : OptionalLong.empty();
  }

  /**
   * Finds the lowest index of any non-zero element in the vector.
   *
   * If there are no non-zero elements in the vector, an empty {@link OptionalLong} will be returned.
   *
   * @return an {@link OptionalLong} containing the lowest index of any non-zero element in the vector, or empty if
   *         the vector has no non-zero elements.
   */
  default OptionalLong minNonZeroElementIndex() {
    VectorElementIterator iter = iterator();
    return iter.hasNext() ? OptionalLong.of(iter.next().getIndex()) : OptionalLong.empty();
  }

  /**
   * Runs the provided consumer against all the non-zero elements of this vector (in order from lowest to highest
   * index).
   *
   * @param consumer a function accepting a vector element index and value.
   */
  default void forEach(VectorElementConsumer consumer) {
    iterator().forEachRemaining(consumer);
  }

  /**
   * Runs a {@link VectorElementPredicate} function over all elements in this vector (in the same order as
   * {@link #iterator()}, from lowest- to highest-index elements).  Iteration is halted if the predicate returns false.
   *
   * @param predicate a function that consumes vector elements to produce some desirable side-effect and returns a
   *                  value indicating whether iteration should continue (true to continue, false to stop)
   */
  default void forEachUntilFalse(VectorElementPredicate predicate) {
    iterator().forEachRemainingUntilFalse(predicate);
  }

  /**
   * Gets a {@link VectorElementIterator} for this Vector that iterates over (only) non-zero elements in order of lowest
   * index to highest index.
   *
   * Note that {@link VectorElementIterator} has methods for efficiently perusing vector elements without actually
   * instantiating {@link VectorElement} objects.
   *
   * @return a new {@link VectorElementIterator}
   */
  @Override
  VectorElementIterator iterator();

  /**
   * Gets a {@link VectorElementIterator} for this Vector that iterates over (only) non-zero elements in order of
   * highest index to lowest index.
   *
   * Note that {@link VectorElementIterator} has methods for efficiently perusing vector elements without actually
   * instantiating {@link VectorElement} objects.
   *
   * @return a new {@link VectorElementIterator}
   */
  VectorElementIterator reverseIterator();

  /**
   * Gets a {@link VectorElementIterator} for this Vector that iterates over (only) non-zero elements in an arbitrary
   * order.  This may be more efficient than {@link #iterator()} depending on how the Vector stores its data.
   *
   * Note that {@link VectorElementIterator} has methods for efficiently perusing vector elements without actually
   * instantiating {@link VectorElement} objects.
   *
   * @return a new {@link VectorElementIterator}
   */
  default VectorElementIterator unorderedIterator() {
    return iterator();
  }

  @Override
  default Spliterator<VectorElement> spliterator() {
    return Spliterators.spliterator(iterator(), size64(),
        Spliterator.NONNULL + Spliterator.DISTINCT + Spliterator.ORDERED);
  }

  /**
   * @return a {@link Stream} of {@link VectorElement}s of this vector whose elements will be ordered from lowest to
   *         highest index.
   */
  default Stream<VectorElement> stream() {
    return StreamSupport.stream(this.spliterator(), false);
  }

  /**
   * Gets the vector as a (new) array of doubles containing a dense representation of the vector: each offset in the
   * resulting array will correspond to the same index in the vector (so the value at offset 3 in the array will be
   * the same as the value at index 3 in the vector).  An exception will be thrown if the vector cannot fit in an
   * array, which will be the case if the highest non-zero element's index is as high or or higher than the maximum
   * supported array length (which is usually slightly less than {@link Integer#MAX_VALUE}, but JVM-dependent), or if
   * the vector has non-zero elements with negative indices.
   *
   * @return an array of doubles corresponding to the vector elements
   */
  default double[] toDoubleArray() {
    if (minNonZeroElementIndex().orElse(0) < 0) {
      throw new IndexOutOfBoundsException("Vector has negative element index");
    }

    double[] result = new double[Math.toIntExact(maxNonZeroElementIndex().orElse(-1) + 1)];
    copyTo(result);

    return result;
  }

  /**
   * Gets the vector as a (new) array of floats containing a dense representation of the vector: each offset in the
   * resulting array will correspond to the same index in the vector (so the value at offset 3 in the array will be
   * the same as the value at index 3 in the vector).  An exception will be thrown if the vector cannot fit in an
   * array, which will be the case if the highest non-zero element's index is as high or or higher than the maximum
   * supported array length (which is usually slightly less than {@link Integer#MAX_VALUE}, but JVM-dependent), or if
   * the vector has non-zero elements with negative indices.
   *
   * Element values that do not fit into floats will be silently truncated via a narrowing double-to-float conversion.
   *
   * @return an array of floats corresponding to the vector elements
   */
  default float[] toFloatArray() {
    if (minNonZeroElementIndex().orElse(0) < 0) {
      throw new IndexOutOfBoundsException("Vector has negative element index");
    }

    float[] result = new float[Math.toIntExact(maxNonZeroElementIndex().orElse(-1) + 1)];
    copyTo(result);

    return result;
  }

  /**
   * Copies the values of this vector's elements to the specified array.  Only values at indices
   * <code>0...array.length-1</code> will be copied (including zeros), and they will be copied into contiguous positions
   * in the array.  No exception will be thrown if the vector contains elements that are not copied.
   *
   * For example, a vector with entries <code>[-3: 2.5, 1: 2.1, 2: 3.14, 3: 42.42]</code> would be copied into an array
   * of length three as: <code>[0, 2.1, 3.14]</code>.
   *
   * Element values that do not fit into floats will be silently truncated via a narrowing double-to-float conversion.
   *
   * @param dest the destination array to which values will be copied
   */
  default void copyTo(float[] dest) {
    copyTo(dest, 0, dest.length);
  }

  /**
   * Copies the values of this vector's elements to the specified array.  Only values at indices
   * <code>0...length-1</code> will be copied (including zeros), and they will be copied into contiguous positions in
   * the array.  No exception will be thrown if the vector contains elements that are not copied.
   *
   * For example, a vector with entries <code>[-3: 2.5, 1: 2.1, 2: 3.14, 3: 42.42]</code> would be copied into an array
   * of length three as: <code>[0, 2.1, 3.14]</code>.
   *
   * Element values that do not fit into floats will be silently truncated via a narrowing double-to-float conversion.
   *
   * @param dest the destination array to which values will be copied
   * @param start the offset in <code>dest</code> where copied data will be placed
   * @param length the number of elements to be copied (including zeros); specifically, elements at indices
   *               <code>0...length-1</code> will be copied to <code>dest</code>
   */
  default void copyTo(float[] dest, int start, int length) {
    // zero the targeted portion of the array
    Arrays.fill(dest, start, start + length, 0);

    unorderedIterator().forEachRemaining((idx, val) -> {
      if (idx >= 0 && idx < length) {
        dest[start + (int) idx] = (float) val;
      }
    });
  }

  /**
   * Copies the values of this vector's elements to the specified array.  Only values at indices
   * <code>0...array.length-1</code> will be copied (including zeros), and they will be copied into contiguous positions
   * in the array.  No exception will be thrown if the vector contains elements that are not copied.
   *
   * For example, a vector with entries <code>[-3: 2.5, 1: 2.1, 2: 3.14, 3: 42.42]</code> would be copied into an array
   * of length three as: <code>[0, 2.1, 3.14]</code>.
   *
   * @param dest the destination array to which values will be copied
   */
  default void copyTo(double[] dest) {
    copyTo(dest, 0, dest.length);
  }

  /**
   * Copies the values of this vector's elements to the specified array.  Only values at indices
   * <code>0...length-1</code> will be copied (including zeros), and they will be copied into contiguous positions in
   * the array.  No exception will be thrown if the vector contains elements that are not copied.
   *
   * For example, a vector with entries <code>[-3: 2.5, 1: 2.1, 2: 3.14, 3: 42.42]</code> would be copied into an array
   * of length three as: <code>[0, 2.1, 3.14]</code>.
   *
   * @param dest the destination array to which values will be copied
   * @param start the offset in <code>dest</code> where copied data will be placed
   * @param length the number of elements to be copied (including zeros); specifically, elements at indices
   *               <code>0...length-1</code> will be copied to <code>dest</code>
   */
  default void copyTo(double[] dest, int start, int length) {
    // zero the targeted portion of the array
    Arrays.fill(dest, start, start + length, 0);

    unorderedIterator().forEachRemaining((idx, val) -> {
      if (idx >= 0 && idx < length) {
        dest[start + (int) idx] = val;
      }
    });
  }

  /**
   * Calculates the norm or "norm" (a true norm is returned iff {@code p >= 1}).
   * See: https://en.wikipedia.org/wiki/Norm_(mathematics)
   *
   * At p == 0 the definition of the "norm" is the number of non-zero entries.
   * At p == Infinity the norm is the maximum absolute value in the vector.
   *
   * @param p the p-value of the norm ({@code >= 0}.  At p == 1 this is Manhattan distance, at p == 2 Euclidean
   *          distance, etc.
   * @return the norm
   */
  default double norm(double p) {
    double[] acc = new double[1]; // accumulator that can be captured by a lambda and convey back the result

    // special and optimized cases
    if (p == 0) { // Hamming distance from 0 vector
      return size64();
    } else if (p == 1) { // Manhattan
      unorderedIterator().forEachRemaining((index, value) -> acc[0] += Math.abs(value));
      return acc[0];
    } else if (p == 2) { // Euclidean
      unorderedIterator().forEachRemaining((index, value) -> acc[0] += value * value);
      return Math.sqrt(acc[0]);
    } else if (Double.isInfinite(p) && p > 0) { // effectively the max absolute value in the vector
      unorderedIterator().forEachRemaining((index, value) -> acc[0] = Math.max(Math.abs(value), acc[0]));
      return acc[0];
    }

    if (p < 0) {
      throw new IllegalArgumentException("p must be greater or equal to 0");
    }

    unorderedIterator().forEachRemaining((index, value) -> acc[0] += Math.pow(Math.abs(value), p));
    return Math.pow(acc[0], 1 / p);
  }

  /**
   * Computes a dot product multiplying this vector with another.
   *
   * Derived classes are encouraged to override this method with a more efficient implementation and/or provide
   * specialized overloads for particular types.
   *
   * @param other the other vector
   * @return the dot product of this vector with another vector
   */
  default double dotProduct(Vector other) {
    double[] result = new double[1];
    this.lazyMultiply(other).unorderedIterator().forEachRemaining((index, value) -> result[0] += value);
    return result[0];
  }

  /**
   * Element-wise multiplies this vector by another vector (Hadamard product).  Neither vector is modified.  This method
   * is lazy and has a trivial cost.
   *
   * <strong>Changes to either operand vector may be reflected in the vector returned by this method.</strong>
   *
   * By default this operation is "lazy", meaning the resulting vector's elements are computed on-the-fly, so it's best
   * to "materialize" the result by converting it to a concrete vector type like SparseSortedVector if you intend to
   * use it extensively.  This also insulates you from future changes to the operand vectors that would otherwise affect
   * the result.
   *
   * Derived classes are free to substitute a non-lazy vector if it can be computed cheaply (e.g. a very small
   * DenseFloatVector).  The implementation must, like the default "lazy" one, still have a trivial cost.
   *
   * @param other the other vector
   * @return a lazy vector that is the product of this and another vector
   */
  default Vector lazyMultiply(Vector other) {
    return new LazyProductVector(this, other);
  }

  /**
   * Multiplies every element by the given scalar.  This method is lazy and has a trivial cost.
   *
   * <strong>Changes to this vector may be reflected in the vector returned by this method.</strong>
   *
   * By default this operation is "lazy", meaning the resulting vector's elements are computed on-the-fly, so it's best
   * to "materialize" the result by converting it to a concrete vector type like SparseSortedVector if you intend to
   * use it extensively.  This also insulates you from future changes to the operand vectors that would otherwise affect
   * the result.
   *
   * Derived classes are free to substitute a non-lazy vector if it can be computed cheaply (e.g. a very small
   * DenseFloatVector).  The implementation must, like the default "lazy" one, still have a trivial cost.
   *
   * @param multiplier the scalar multiplicand
   * @return a lazy vector where every value is multiplied by the given multiplier
   */
  default Vector lazyMultiply(double multiplier) {
    if (Double.isInfinite(multiplier) || Double.isNaN(multiplier)) {
      throw new ArithmeticException("Attempted to multiply vector elements by infinity or NaN, which would implicitly "
          + "create an infinite number of NaN elements");
    }
    return new LazyScalarProductVector(this, multiplier);
  }

  /**
   * Divides every element by the given scalar.  This method is lazy and has a trivial cost.
   *
   * <strong>Changes to either operand vector may be reflected in the vector returned by this method.</strong>
   *
   * By default this operation is "lazy", meaning the resulting vector's elements are computed on-the-fly, so it's best
   * to "materialize" the result by converting it to a concrete vector type like SparseSortedVector if you intend to
   * use it extensively.  This also insulates you from future changes to the operand vectors that would otherwise affect
   * the result.
   *
   * Derived classes are free to substitute a non-lazy vector if it can be computed cheaply (e.g. a very small
   * DenseFloatVector).  The implementation must, like the default "lazy" one, still have a trivial cost.
   *
   * @param divisor the scalar divisor
   * @return a lazy vector where every value is divided by the given divisor
   */
  default Vector lazyDivide(double divisor) {
    if (divisor == 0) {
      throw new ArithmeticException("Attempt to divide vector elements by 0, which would implicitly create an infinite"
          + "number of NaN elements");
    }
    return new LazyScalarQuotientVector(this, divisor);
  }

  /**
   * Adds this vector with another vector.  Neither vector is modified.  This method is lazy and has a trivial cost.
   *
   * <strong>Changes to either operand vector may be reflected in the vector returned by this method.</strong>
   *
   * By default this operation is "lazy", meaning the resulting vector's elements are computed on-the-fly, so it's best
   * to "materialize" the result by converting it to a concrete vector type like SparseSortedVector if you intend to
   * use it extensively.  This also insulates you from future changes to the operand vectors that would otherwise affect
   * the result.
   *
   * Derived classes are free to substitute a non-lazy vector if it can be computed cheaply (e.g. a very small
   * DenseFloatVector).  The implementation must, like the default "lazy" one, still have a trivial cost.  If one vector is
   * a 0 vector, this method may simply return the non-zero operand.
   *
   * @param other the other vector
   * @return a lazily-computed vector that is the sum of this and another vector
   */
  default Vector lazyAdd(Vector other) {
    return new LazySumVector(this, other);
  }

  /**
   * Subtracts another vector from this one.  Neither vector is modified.  This method is lazy and has a trivial cost.
   *
   * <strong>Changes to either operand vector may be reflected in the vector returned by this method.</strong>
   *
   * By default this operation is "lazy", meaning the resulting vector's elements are computed on-the-fly, so it's best
   * to "materialize" the result by converting it to a concrete vector type like SparseSortedVector if you intend to
   * use it extensively.  This also insulates you from future changes to the operand vectors that would otherwise affect
   * the result.
   *
   * Derived classes are free to substitute a non-lazy vector if it can be computed cheaply (e.g. a very small
   * DenseFloatVector).  The implementation must, like the default "lazy" one, still have a trivial cost.
   *
   * @param other the other vector
   * @return a lazy vector that is the difference of this and another vector
   */
  default Vector lazySubtract(Vector other) {
    return new LazyDifferenceVector(this, other);
  }

  /**
   * Lazily negates each element of this vector (e.g. an element with value X now has value -X).  This method has
   * trivial cost.
   *
   * <strong>Changes to this vector may be reflected in the vector returned by this method.</strong>
   *
   * By default this operation is "lazy", meaning the resulting vector's elements are computed on-the-fly, so it's best
   * to "materialize" the result by converting it to a concrete vector type like SparseSortedVector if you intend to
   * use it extensively.  This also insulates you from future changes to the operand vectors that would otherwise affect
   * the result.
   *
   * Derived classes are free to substitute a non-lazy vector if it can be computed cheaply (e.g. a very small
   * DenseFloatVector).  The implementation must, like the default "lazy" one, still have a trivial cost.
   *
   * @return a vector whose elements have the negated values of this vector
   */
  default Vector lazyNegation() {
    return new LazyNegationVector(this);
  }

  /**
   * Lazily "clips" each value of this vector to a specified range (which must include 0).  Any value outside this range
   * will be changed to the minimum (if less than the clipping range minimum) or the maximum (if greater than the range
   * maximum).
   *
   * NaN (not-a-number) values will not be clipped and will remain NaN in the clipped vector.
   *
   * <strong>Changes to this vector may be reflected in the vector returned by this method.</strong>
   *
   * By default this operation is "lazy", meaning the resulting vector's elements are computed on-the-fly, so it's best
   * to "materialize" the result by converting it to a concrete vector type like SparseSortedVector if you intend to
   * use it extensively.
   *
   * Derived classes are free to substitute a non-lazy vector if it can be computed cheaply (e.g. a very small
   * DenseFloatVector).  The implementation must, like the default "lazy" one, still have a trivial cost.
   *
   * @param min the minimum value of the clipping range; must be {@code <= 0}
   * @param max the maximum value of the clipping range; must be {@code >= 0}
   * @return a vector whose elements have the clipped values of this vector
   */
  default Vector lazyClip(double min, double max) {
    return new LazyClippedVector(this, min, max);
  }

  /**
   * Returns an immutable 0-vector (all elements are 0).  The vector returned will always be the same instance.
   *
   * @return a 0-vector
   */
  static DenseVector empty() {
    return EmptyVector.INSTANCE;
  }
}
