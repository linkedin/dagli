package com.linkedin.dagli.objectio;

import com.linkedin.dagli.util.array.ArraysEx;
import java.util.function.IntFunction;


/**
 * Given an array of ObjectReaders of equal size, creates a new ObjectReader whose items are arrays of the items of
 * its constituent ObjectReaders.  No copies of the underlying ObjectReaders are created.
 *
 * So, for example, if we combine three ObjectReaders:
 * A = [1, 2, 3]
 * B = [0, 0, 0]
 * C = [-1, -2, -3]
 *
 * The concatenated array ObjectReader would be:
 * [[1, 0, -1], [2, 0, -2], [3, 0, -3]]
 */
public class ConcatenatedReader<T> implements ObjectReader<T[]> {
  private final ObjectReader<T>[] _objectReaders;
  private final IntFunction<T[]> _arrayGenerator;

  /**
   * Retrieves the constituent batch iterables being concatenated.
   *
   * @return a new array containing the batch iterables comprising this concatenation
   */
  public ObjectReader<T>[] getObjectReaders() {
    return _objectReaders.clone();
  }

  /**
   * Creates a new reader instance.
   *
   * @param arrayGenerator a method that accepts an integer size and returns an array of that size of type T (the type
   *                       of element read by this reader).  Specifically, you can (and should) pass the constructor
   *                       for the array, e.g. if T == String then the array generator would be String[]::new.
   *
   * @param objectReaders one or more parallel readers (each of the same size) whose items will be combined into arrays.
   */
  @SafeVarargs
  @SuppressWarnings("unchecked")
  public ConcatenatedReader(IntFunction<T[]> arrayGenerator, ObjectReader<? extends T>... objectReaders) {
    _objectReaders = (ObjectReader<T>[]) objectReaders;
    _arrayGenerator = arrayGenerator;
    assert allSameSize(objectReaders);
  }

  // check that all passed readers are the same size (or that <= 1 readers are provided
  @SafeVarargs
  private static <T> boolean allSameSize(ObjectReader<? extends T>... objectReaders) {
    if (objectReaders.length > 1) {
      final long size = objectReaders[0].size64();
      for (int i = 1; i < objectReaders.length; i++) {
        if (objectReaders[i].size64() != size) {
          return false;
        }
      }
    }

    return true;
  }

  @Override
  public long size64() {
    return _objectReaders[0].size64();
  }

  @Override
  public Iterator<T> iterator() {
    ObjectIterator<? extends T>[] ois = new ObjectIterator[_objectReaders.length];
    for (int i = 0; i < ois.length; i++) {
      ois[i] = _objectReaders[i].iterator();
    }
    return new Iterator<>(_arrayGenerator, ois);
  }

  @Override
  public void close() {
    for (ObjectReader<? extends T> or : _objectReaders) {
      or.close();
    }
  }

  /**
   * Given an array of ObjectIterators of equal size, creates a new ObjectIterator whose items are arrays of the items of
   * its constituent ObjectIterators.  No copies of the underlying ObjectIterators are created.
   *
   * So, for example, if we combine three ObjectIterators with the following sets of items:
   * A = [1, 2, 3]
   * B = [0, 0, 0]
   * C = [-1, -2, -3]
   *
   * The concatenated array ObjectIterator would be:
   * [[1, 0, -1], [2, 0, -2], [3, 0, -3]]
   */
  public static class Iterator<T> implements ObjectIterator<T[]> {
    private final ObjectIterator<T>[] _objectIterators;
    private final IntFunction<T[]> _arrayGenerator;
    private Object[] _buffer = ArraysEx.EMPTY_OBJECT_ARRAY;

    /**
     * Creates a new reader instance.
     *
     * @param arrayGenerator a method that accepts an integer size and returns an array of that size of type T (the type
     *                       of element read by this reader).  Specifically, you can (and should) pass the constructor
     *                       for the array, e.g. if T == String then the array generator would be String[]::new.
     *
     * @param objectIterators one or more parallel iterators (each of the same size) whose items will be combined into
     *                        arrays.
     */
    @SafeVarargs
    @SuppressWarnings("unchecked") // safe because the iterator is read-only
    public Iterator(IntFunction<T[]> arrayGenerator, ObjectIterator<? extends T>... objectIterators) {
      _arrayGenerator = arrayGenerator;
      _objectIterators = (ObjectIterator<T>[]) objectIterators;
    }

    @Override
    public boolean hasNext() {
      return _objectIterators[0].hasNext();
    }

    @Override
    public T[] next() {
      T[] arr = _arrayGenerator.apply(_objectIterators.length);
      for (int i = 0; i < arr.length; i++) {
        arr[i] = _objectIterators[i].next();
      }
      return arr;
    }

    private void copyBufferToDestination(Object[] destination, int offset, int count, int iteratorIndex) {
      for (int i = 0; i < count; i++) {
        ((Object[]) destination[offset + i])[iteratorIndex] = _buffer[i];
      }
    }

    @Override
    @SuppressWarnings("unchecked") // _buffer never escapes this class
    public int next(Object[] destination, int offset, int count) {
      if (_buffer.length < count) {
        _buffer = (T[]) new Object[count];
      }
      int resultCount = _objectIterators[0].next(_buffer, 0, count);

      for (int i = 0; i < resultCount; i++) {
        destination[offset + i] = _arrayGenerator.apply(_objectIterators.length);
      }
      copyBufferToDestination(destination, offset, resultCount, 0);

      for (int i = 1; i < _objectIterators.length; i++) {
        _objectIterators[i].next(_buffer, 0, resultCount);
        copyBufferToDestination(destination, offset, resultCount, i);
      }

      return resultCount;
    }

    @Override
    public void close() {
      for (ObjectIterator<? extends T> iter : _objectIterators) {
        iter.close();
      }
    }

    @Override
    public long skip(long toSkip) {
      for (int i = 1; i < _objectIterators.length; i++) {
        _objectIterators[i].skip(toSkip);
      }
      return _objectIterators[0].skip(toSkip);
    }
  }
}
