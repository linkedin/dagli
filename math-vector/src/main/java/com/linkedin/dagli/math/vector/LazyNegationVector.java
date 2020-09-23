package com.linkedin.dagli.math.vector;

import com.linkedin.dagli.math.number.PrimitiveNumberTypes;


/**
 * A vector that represents the result of lazily-negating each element (e.g. an element with value X now has value -X).
 *
 * Note that this class never "caches" the result; it is always done "on-the-fly".  This is good if the result is only
 * used once or twice (or never!), but if it is used repeatedly you should "materialize" the vector into a concrete
 * form like a SparseSortedVector.
 */
class LazyNegationVector extends AbstractVector {
  private static final long serialVersionUID = 1;

  private final Vector _negated;

  /**
   * Default constructor for the benefit of Kryo serialization.  Results in an invalid instance (Kryo will fill in the
   * fields with deserialized values after instantiation).
   */
  private LazyNegationVector() {
    this(null);
  }

  @Override
  public Class<? extends Number> valueType() {
    return PrimitiveNumberTypes.negatedType(_negated.valueType());
  }

  /**
   * Creates a new vector that lazily negates the element values of a wrapped vector
   * @param toNegate the wrapped vector to negate
   */
  public LazyNegationVector(Vector toNegate) {
    _negated = toNegate;
  }

  @Override
  public double get(long index) {
    return -_negated.get(index);
  }

  @Override
  public VectorElementIterator iterator() {
    return new Iterator(_negated.iterator());
  }

  @Override
  public VectorElementIterator reverseIterator() {
    return new Iterator(_negated.reverseIterator());
  }

  @Override
  public VectorElementIterator unorderedIterator() {
    return new Iterator(_negated.unorderedIterator());
  }

  private static class Iterator implements VectorElementIterator {
    private final VectorElementIterator _negatedIterator;

    Iterator(VectorElementIterator wrapped) {
      _negatedIterator = wrapped;
    }

    @Override
    public <T> T mapNext(VectorElementFunction<T> mapper) {
      return _negatedIterator.mapNext((index, value) -> mapper.apply(index, -value));
    }

    @Override
    public void next(VectorElementConsumer consumer) {
      _negatedIterator.next((index, value) -> consumer.consume(index, -value));
    }

    @Override
    public boolean hasNext() {
      return _negatedIterator.hasNext();
    }
  }
}
