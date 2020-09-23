package com.linkedin.dagli.math.mdarray;

import com.linkedin.dagli.math.vector.AbstractVector;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.math.vector.VectorElementConsumer;
import com.linkedin.dagli.math.vector.VectorElementFunction;
import com.linkedin.dagli.math.vector.VectorElementIterator;
import java.util.NoSuchElementException;


/**
 * {@link DenseVector} backed by an {@link MDArray}.
 */
class MDArrayAsVector extends AbstractVector implements DenseVector {
  private static final long serialVersionUID = 1;

  private final MDArray _mdArray;
  private final long _capacity; // cached to avoid cost of repeated computation

  MDArrayAsVector(MDArray mdArray) {
    _mdArray = mdArray;
    _capacity = MDArrays.elementCount(_mdArray.shape());
  }

  @Override
  public long capacity() {
    return _capacity;
  }

  @Override
  public Class<? extends Number> valueType() {
    return _mdArray.valueType();
  }

  @Override
  public double get(long index) {
    if (index < 0 || index >= _capacity) {
      return 0;
    }

    return _mdArray.getAsDouble(index);
  }

  @Override
  public VectorElementIterator iterator() {
    return new Iterator();
  }

  @Override
  public VectorElementIterator reverseIterator() {
    return new ReverseIterator();
  }

  private class Iterator implements VectorElementIterator {
    private long _nextOffset = 0;
    private double _currentValue = 0;

    @Override
    public <T> T mapNext(VectorElementFunction<T> mapper) {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      double retVal = _currentValue;
      _currentValue = 0;
      return mapper.apply(_nextOffset - 1, retVal);
    }

    @Override
    public void next(VectorElementConsumer consumer) {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      double retVal = _currentValue;
      _currentValue = 0;
      consumer.consume(_nextOffset - 1, retVal);
    }

    @Override
    public boolean hasNext() {
      while (_currentValue == 0 && _nextOffset < _capacity) {
        _currentValue = _mdArray.getAsDouble(_nextOffset++);
      }
      return _currentValue != 0;
    }
  }

  private class ReverseIterator implements VectorElementIterator {
    private long _currentOffset = _capacity;
    private double _currentValue = 0;

    @Override
    public <T> T mapNext(VectorElementFunction<T> mapper) {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      double retVal = _currentValue;
      _currentValue = 0;
      return mapper.apply(_currentOffset, retVal);
    }

    @Override
    public void next(VectorElementConsumer consumer) {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      double retVal = _currentValue;
      _currentValue = 0;
      consumer.consume(_currentOffset, retVal);
    }

    @Override
    public boolean hasNext() {
      while (_currentValue == 0 && _currentOffset > 0) {
        _currentValue = _mdArray.getAsDouble(--_currentOffset);
      }
      return _currentValue != 0;
    }
  }
}
