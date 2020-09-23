package com.linkedin.dagli.objectio;

import java.util.Arrays;
import java.util.NoSuchElementException;


/**
 * A reader whose values are all the same, constant object.
 *
 * @param <T> the type of the constant value
 */
public class ConstantReader<T> implements ObjectReader<T> {

  private final long _count;
  private T _obj; // non-final so it can be cleared by close()

  /**
   * Creates a new reader that will contain the specified value in the specified quantity
   *
   * @param obj the object that will be read by this reader
   * @param count the number of times this object is (logically) present in this reader.  This will be the value
   *              returned by {@link ConstantReader#size64()}, etc.
   */
  public ConstantReader(T obj, long count) {
    _count = count;
    _obj = obj;
  }

  @Override
  public long size64() {
    return _count;
  }

  @Override
  public ObjectIterator<T> iterator() {
    return new Iterator<>(_obj, _count);
  }

  @Override
  public void close() {
    _obj = null;
  }

  /**
   * An iterator that returns a constant object a fixed number of times.
   *
   * @param <T> the type of object that will be returned
   */
  public static class Iterator<T> implements ObjectIterator<T> {
    private long _current = 0;
    private final long _count;
    private T _obj;

    /**
     * Creates a new instance.
     *
     * @param obj the constant object value that the iterator will return
     * @param count the number of times this iterator (logically) contains this object
     */
    public Iterator(T obj, long count) {
      _obj = obj;
      _count = count;
    }

    @Override
    public long skip(long toSkip) {
      toSkip = Math.min(toSkip, _count - _current);
      this._current += toSkip;
      return toSkip;
    }

    @Override
    public boolean hasNext() {
      return _current < _count;
    }

    @Override
    public T next() {
      if (_current >= _count) {
        throw new NoSuchElementException();
      }

      _current++;
      return _obj;
    }

    @Override
    public int tryNextAvailable(Object[] destination, int offset, int count) {
      return next(destination, offset, count);
    }

    @Override
    public int nextAvailable(Object[] destination, int offset, int count) {
      return next(destination, offset, count);
    }

    @Override
    public int next(Object[] destination, int offset, int count) {
      count = (int) Math.min(count, _count - _current);
      _current += count;

      Arrays.fill(destination, offset, offset + count, _obj);
      return count;
    }

    @Override
    public void close() {
      _obj = null;
    }
  }
}
