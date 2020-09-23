package com.linkedin.dagli.objectio.tuple;

import com.linkedin.dagli.objectio.ObjectIterator;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.tuple.Tuple;
import com.linkedin.dagli.tuple.Tuple1;
import com.linkedin.dagli.tuple.Tuple10;
import com.linkedin.dagli.tuple.Tuple2;
import com.linkedin.dagli.tuple.Tuple3;
import com.linkedin.dagli.tuple.Tuple4;
import com.linkedin.dagli.tuple.Tuple5;
import com.linkedin.dagli.tuple.Tuple6;
import com.linkedin.dagli.tuple.Tuple7;
import com.linkedin.dagli.tuple.Tuple8;
import com.linkedin.dagli.tuple.Tuple9;
import com.linkedin.dagli.util.array.ArraysEx;
import java.util.NoSuchElementException;


/**
 * Reads values from multiple parallel object readers as {@link Tuple}s.  "Parallel" here means that the readers have
 * the same number of objects and the ith object read by each reader will comprise part of the ith tuple.
 *
 * @param <T> the type of the tuple (e.g. {@link Tuple1}, {@link Tuple2}, etc.)  The tuple's arity must correspond with
 *            the number of readers wrapped by this {@link TupleReader}.
 */
public class TupleReader<T extends Tuple> implements ObjectReader<T> {
  private final ObjectReader<?>[] _objectReaders;

  /**
   * Creates a new instance that will wrap the provided object readers, packaging their read values into tuples.
   *
   * @param objectReaders the objectReaders to wrap.
   */
  public TupleReader(ObjectReader<?>... objectReaders) {
    if (objectReaders.length == 0) {
      throw new IllegalArgumentException("Must provide at least one ObjectReader");
    }
    _objectReaders = objectReaders;
  }

  public int getTupleSize() {
    return _objectReaders.length;
  }

  /**
   * Creates a tuple from the ith array in an array of Object arrays.  This is needed to generate the tuples from the
   * items read by the constituent ObjectReaders.
   *
   * @param values an array of arrays
   * @param index the index of the array to convert to a tuple
   * @return a tuple of the elements in the objs[index] array
   */
  private static Tuple toTuple(Object[][] values, int index) {
    switch (values.length) {
      case 1:
        return Tuple1.of(values[0][index]);
      case 2:
        return Tuple2.of(values[0][index], values[1][index]);
      case 3:
        return Tuple3.of(values[0][index], values[1][index], values[2][index]);
      case 4:
        return Tuple4.of(values[0][index], values[1][index], values[2][index], values[3][index]);
      case 5:
        return Tuple5.of(values[0][index], values[1][index], values[2][index], values[3][index], values[4][index]);
      case 6:
        return Tuple6.of(values[0][index], values[1][index], values[2][index], values[3][index], values[4][index],
            values[5][index]);
      case 7:
        return Tuple7.of(values[0][index], values[1][index], values[2][index], values[3][index], values[4][index],
            values[5][index], values[6][index]);
      case 8:
        return Tuple8.of(values[0][index], values[1][index], values[2][index], values[3][index], values[4][index],
            values[5][index], values[6][index], values[7][index]);
      case 9:
        return Tuple9.of(values[0][index], values[1][index], values[2][index], values[3][index], values[4][index],
            values[5][index], values[6][index], values[7][index], values[8][index]);
      case 10:
        return Tuple10.of(values[0][index], values[1][index], values[2][index], values[3][index], values[4][index],
            values[5][index], values[6][index], values[7][index], values[8][index], values[9][index]);
      default:
        throw new UnsupportedOperationException("Unsupported arity of the values array: " + values.length);
    }
  }

  @Override
  public long size64() {
    return _objectReaders[0].size64();
  }

  /**
   * Implementation of an iterator that iterates over an array of wrapped iterators and packages the iterated values as
   * tuples.
   *
   * @param <T> the type of the tuple (e.g. {@link Tuple1}, {@link Tuple2}, etc.)  The tuple's arity must correspond with
   *            the number of iterators wrapped by this instance.
   */
  private static class Iterator<T extends Tuple> implements ObjectIterator<T> {
    private final ObjectIterator<Object>[] _iterators;
    private final T[] _minibuffer = (T[]) new Tuple[1];

    public Iterator(ObjectIterator<Object>[] iterators) {
      _iterators = iterators;
    }

    @Override
    public boolean hasNext() {
      return _iterators[0].hasNext();
    }

    @Override
    public T next() {
      if (next(_minibuffer) == 0) {
        throw new NoSuchElementException();
      }

      T res = _minibuffer[0];
      _minibuffer[0] = null;
      return res;
    }

    @Override
    public int next(Object[] destination, int offset, int count) {
      Object[][] buffers = new Object[_iterators.length][count];
      int maxRead = 0;

      for (int i = 0; i < _iterators.length; i++) {
        int readSoFar = 0;
        int read;
        while ((read = _iterators[i].next(buffers[i], readSoFar, count - readSoFar)) > 0) {
          readSoFar += read;
        }
        maxRead = Math.max(maxRead, readSoFar);
      }

      for (int i = 0; i < maxRead; i++) {
        destination[offset + i] = toTuple(buffers, i);
      }

      return maxRead;
    }

    @Override
    public void close() {
      for (int i = 0; i < _iterators.length; i++) {
        _iterators[i].close();
        _iterators[i] = null;
      }
    }
  }

  @Override
  public ObjectIterator<T> iterator() {
    ObjectIterator[] res = ArraysEx.mapArray(_objectReaders, ObjectIterator[]::new, ObjectReader::iterator);
    return new Iterator<T>(res);
  }

  @Override
  public void close() {
    java.util.Arrays.fill(_objectReaders, null);
  }
}
