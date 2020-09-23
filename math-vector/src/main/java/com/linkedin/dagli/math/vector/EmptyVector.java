package com.linkedin.dagli.math.vector;

import java.io.ObjectStreamException;
import java.util.NoSuchElementException;
import java.util.function.Consumer;


/**
 * An immutable vector with no elements.
 */
class EmptyVector extends AbstractVector implements DenseVector {
  private static final long serialVersionUID = 1;

  private static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
  static final EmptyVector INSTANCE = new EmptyVector();

  // we use a custom readResolve() method to ensure that only a single instance exists, even when previously-serialized
  // copies are being read
  private Object readResolve() throws ObjectStreamException {
    return INSTANCE;
  }

  @Override
  public Class<? extends Number> valueType() {
    return byte.class; // smallest type available
  }

  @Override
  public double get(long index) {
    return 0;
  }

  @Override
  public long size64() {
    return 0;
  }

  @Override
  public void forEach(VectorElementConsumer consumer) {
    // noop
  }

  @Override
  public VectorElementIterator iterator() {
    return new VectorElementIterator() {
      @Override
      public <T> T mapNext(VectorElementFunction<T> mapper) {
        throw new NoSuchElementException();
      }

      @Override
      public boolean hasNext() {
        return false;
      }
    };
  }

  @Override
  public VectorElementIterator reverseIterator() {
    return iterator();
  }

  @Override
  public void forEach(Consumer<? super VectorElement> action) {
    // noop
  }

  @Override
  public double[] toDoubleArray() {
    return EMPTY_DOUBLE_ARRAY;
  }

  @Override
  public double norm(double p) {
    return 0;
  }

  @Override
  public double dotProduct(Vector other) {
    return 0;
  }

  @Override
  public Vector lazyMultiply(Vector other) {
    return this;
  }

  @Override
  public Vector lazyMultiply(double multiplier) {
    if (Double.isInfinite(multiplier) || Double.isNaN(multiplier)) {
      throw new ArithmeticException("Attempted to multiply vector elements by infinity or NaN, which would implicitly "
          + "create an infinite number of NaN elements");
    }
    return this; // 0 * multiplier == 0
  }

  @Override
  public Vector lazyDivide(double divisor) {
    if (divisor == 0) {
      // 0/0 is not allowed
      throw new ArithmeticException("Attempt to divide vector elements by 0, which would implicitly create an infinite"
          + "number of NaN elements");
    }
    return this; // 0 / nonzero = 0
  }

  @Override
  public Vector lazyAdd(Vector other) {
    return other; // 0 + other == other
  }

  @Override
  public Vector lazySubtract(Vector other) {
    return new LazyNegationVector(other); // 0 - other == -other
  }

  @Override
  public Vector lazyNegation() {
    return this; // -0 == 0
  }

  @Override
  public long capacity() {
    return 0;
  }
}
