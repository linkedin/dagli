package com.linkedin.dagli.math.vector;

/**
 * Base class for a vector that represents the result of a lazily-executed operation on the non-zero values of a single
 * vector.
 *
 * Note that this class never "caches" the result; it is always done "on-the-fly".  This is good if the result is only
 * used once or twice (or never!), but if it is used repeatedly you should "materialize" the vector into a concrete
 * form like a SparseSortedVector.
 */
public abstract class LazyTransformedValueVector extends AbstractVector {
  private static final long serialVersionUID = 1;

  private final Vector _vector;

  protected Vector getWrappedVector() {
    return _vector;
  }

  /**
   * Computes a new value for an element given the index and value in the original vector.
   *
   * @param index the element's index
   * @param value the element's value
   * @return the transformed value
   */
  protected abstract double compute(long index, double value);

  /**
   * Creates a new vector that lazily transforms the values of a wrapped vector
   *
   * @param wrapped the vector whose values will be transformed
   */
  public LazyTransformedValueVector(Vector wrapped) {
    _vector = wrapped;
  }

  @Override
  public double get(long index) {
    double val = _vector.get(index);
    if (val != 0) {
      return compute(index, val);
    }

    return 0;
  }

  @Override
  public VectorElementIterator iterator() {
    return new Iterator(_vector.iterator());
  }

  @Override
  public VectorElementIterator reverseIterator() {
    return new Iterator(_vector.reverseIterator());
  }

  @Override
  public VectorElementIterator unorderedIterator() {
    return new Iterator(_vector.unorderedIterator());
  }

  public class Iterator extends VectorElementTransformedValueIterator {
    /**
     * Creates a new transformed iterator.
     *  @param iterator the underlying iterator to be transformed
     */
    public Iterator(VectorElementIterator iterator) {
      super(iterator, LazyTransformedValueVector.this::compute);
    }
  }
}
