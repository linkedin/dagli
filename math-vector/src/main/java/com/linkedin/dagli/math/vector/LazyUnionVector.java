package com.linkedin.dagli.math.vector;

import java.util.NoSuchElementException;


/**
 * Base class for a vector that represents the result of a lazily-executed operation on the two operand vectors, where
 * the operation is over the union of their non-zero elements (all others are assumed to be 0); an example is element-
 * wise summation of two vectors.
 *
 * Note that this class never "caches" the result; it is always done "on-the-fly".  This is good if the result is only
 * used once or twice (or never!), but if it is used repeatedly you should "materialize" the vector into a concrete
 * form like a SparseFloatArrayVector.
 */
public abstract class LazyUnionVector extends AbstractVector {
  private static final long serialVersionUID = 1;

  private final Vector _first;
  private final Vector _second;

  protected Vector getFirstVector() {
    return _first;
  }

  protected Vector getSecondVector() {
    return _second;
  }

  /**
   * Computes the product of two non-zero elements (if one of the operands is 0, the result is assumed to be 0).
   *
   * @param elementValue1 the first element's value
   * @param elementValue2 the second element's value
   * @return the result of applying some operation to the two elements
   */
  protected abstract double compute(double elementValue1, double elementValue2);

  /**
   * Creates a new vector that lazily performs an operation on two wrapped vector operands.
   * @param first the first wrapped operand
   * @param second the second wrapped operand
   */
  public LazyUnionVector(Vector first, Vector second) {
    _first = first;
    _second = second;
  }

  @Override
  public double get(long index) {
    double val1 = _first.get(index);
    double val2 = _second.get(index);
    if (val1 != 0 || val2 != 0) {
      return compute(val1, val2);
    }
    return 0;
  }

  @Override
  public VectorElementIterator iterator() {
    return new Iterator(false);
  }

  @Override
  public VectorElementIterator reverseIterator() {
    return new Iterator(true);
  }

  private class Iterator implements VectorElementIterator {
    private final VectorElementIterator _firstIterator;
    private final VectorElementIterator _secondIterator;

    private final boolean _reverse;

    private long _currentFirstIndex;
    private long _currentSecondIndex;

    private double _currentFirstValue = 0;
    private double _currentSecondValue = 0;

    private double _nextResult;
    private long _nextIndex;

    private boolean _firstExhausted = false;
    private boolean _secondExhausted = false;


    public Iterator(boolean reverse) {
      _reverse = reverse;

      _firstIterator = reverse ? _first.reverseIterator() : _first.iterator();
      _secondIterator = reverse ? _second.reverseIterator() : _second.iterator();

      _currentFirstIndex = reverse ? Long.MAX_VALUE : Long.MIN_VALUE;
      _currentSecondIndex = _currentFirstIndex;
      _nextIndex = _currentFirstIndex;

      advance();
    }

    private void advanceFirst() {
      if (_firstIterator.hasNext()) {
        _firstIterator.next((index, value) -> {
          _currentFirstIndex = index;
          _currentFirstValue = value;
        });
      } else {
        _firstExhausted = true;
        _currentFirstIndex = _reverse ? Long.MIN_VALUE : Long.MAX_VALUE;
        _currentFirstValue = 0;
      }
    }

    private void advanceSecond() {
      if (_secondIterator.hasNext()) {
        _secondIterator.next((index, value) -> {
          _currentSecondIndex = index;
          _currentSecondValue = value;
        });
      } else {
        _secondExhausted = true;
        _currentSecondIndex = _reverse ? Long.MIN_VALUE : Long.MAX_VALUE;
        _currentSecondValue = 0;
      }
    }

    private boolean before(long firstIndex, long secondIndex) {
      return _reverse ? firstIndex > secondIndex : firstIndex < secondIndex;
    }

    private void advance() {
      do {
        if (_currentFirstIndex == _nextIndex) {
          advanceFirst();
        }
        if (_currentSecondIndex == _nextIndex) {
          advanceSecond();
        }

        if (_firstExhausted && _secondExhausted) {
          return;
        }

        if (_currentFirstIndex == _currentSecondIndex) {
          _nextIndex = _currentFirstIndex;
          _nextResult = compute(_currentFirstValue, _currentSecondValue);
        } else if (before(_currentFirstIndex, _currentSecondIndex)) {
          _nextIndex = _currentFirstIndex;
          _nextResult = compute(_currentFirstValue, 0);
        } else {
          _nextIndex = _currentSecondIndex;
          _nextResult = compute(0, _currentSecondValue);
        }
      } while (_nextResult == 0);
    }

    @Override
    public boolean hasNext() {
      return !(_firstExhausted && _secondExhausted);
    }

    @Override
    public <T> T mapNext(VectorElementFunction<T> mapper) {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      T res = mapper.apply(_nextIndex, _nextResult);
      advance();
      return res;
    }

    @Override
    public void next(VectorElementConsumer consumer) {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      consumer.consume(_nextIndex, _nextResult);
      advance();
    }
  }
}
