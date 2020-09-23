package com.linkedin.dagli.math.vector;

import java.util.NoSuchElementException;


/**
 * VectorElementIterator that transforms the values of another VectorElementIterator using a provided transformation
 * function.
 */
public class VectorElementTransformedValueIterator implements VectorElementIterator {

  private final VectorElementTransformer _transformer;
  private final VectorElementIterator _iterator;

  private long _cachedIndex = 0;
  private double _cachedValue = 0;
  private boolean _hasCached = false;
  private boolean _exhausted = false;

  /**
   * Creates a new transformed iterator.
   *
   * @param iterator the underlying iterator to be transformed
   * @param transformer a function that transforms the value of elements
   */
  public VectorElementTransformedValueIterator(VectorElementIterator iterator, VectorElementTransformer transformer) {
    _iterator = iterator;
    _transformer = transformer;
  }

  @Override
  public void forEachRemaining(VectorElementConsumer consumer) {
    if (_hasCached) {
      consumer.consume(_cachedIndex, _cachedValue);
      _hasCached = false;
    }

    _iterator.forEachRemaining(
        (index, value) -> {
          double newValue = _transformer.transform(index, value);
          if (newValue != 0) {
            consumer.consume(index, newValue);
          }
        }
    );
  }

  // Caches the next value.  An untested precondition is that _hasCached is false and the iterator has more elements.
  private void cacheNext() {
    do {
      _iterator.next((index, value) -> {
        double newValue = _transformer.transform(index, value);
        if (newValue != 0) {
          _hasCached = true;
          _cachedIndex = index;
          _cachedValue = newValue;
        }
      });

      if (_hasCached) {
        return;
      }
    } while (_iterator.hasNext());

    _exhausted = true;
  }

  @Override
  public <T> T mapNext(VectorElementFunction<T> mapper) {
    if (!_hasCached) {
      cacheNext();
      if (!_hasCached) {
        throw new NoSuchElementException();
      }
    }

    _hasCached = false;
    return mapper.apply(_cachedIndex, _cachedValue);
  }

  @Override
  public void next(VectorElementConsumer consumer) {
    if (!_hasCached) {
      cacheNext();
      if (!_hasCached) {
        throw new NoSuchElementException();
      }
    }

    _hasCached = false;
    consumer.consume(_cachedIndex, _cachedValue);
  }

  @Override
  public boolean hasNext() {
    if (_hasCached) {
      return true;
    } else if (_exhausted) {
      return false;
    } else if (_iterator.hasNext()) {
      cacheNext();
      return _hasCached;
    } else {
      return false;
    }
  }
}
