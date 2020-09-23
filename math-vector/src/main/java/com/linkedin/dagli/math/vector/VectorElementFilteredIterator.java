package com.linkedin.dagli.math.vector;

import java.util.NoSuchElementException;


/**
 * VectorElementIterator that filters another VectorElementIterator using a provided filtering function.
 */
public class VectorElementFilteredIterator implements VectorElementIterator {

  private final VectorElementPredicate _predicate;
  private final VectorElementIterator _iterator;

  private long _cachedIndex = 0;
  private double _cachedValue = 0;
  private boolean _hasCached = false;
  private boolean _exhausted = false;

  /**
   * Creates a new filtered iterator.
   *
   * @param iterator the underlying iterator to be filtered
   * @param predicate a function that will return true if the element should be iterated over, or false to ignore it.
   */
  public VectorElementFilteredIterator(VectorElementIterator iterator, VectorElementPredicate predicate) {
    _iterator = iterator;
    _predicate = predicate;
  }

  @Override
  public void forEachRemaining(VectorElementConsumer consumer) {
    if (_hasCached) {
      consumer.consume(_cachedIndex, _cachedValue);
      _hasCached = false;
    }

    _iterator.forEachRemaining(
        (index, value) -> {
          if (_predicate.test(index, value)) {
            consumer.consume(index, value);
          }
        }
    );
  }

  // Caches the next value.  An untested precondition is that _hasCached is false and the iterator has more elements.
  private void cacheNext() {
    do {
      _iterator.next((index, value) -> {
        if (_predicate.test(index, value)) {
          _hasCached = true;
          _cachedIndex = index;
          _cachedValue = value;
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
