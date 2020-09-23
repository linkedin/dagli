package com.linkedin.dagli.objectio.biglist;

import com.linkedin.dagli.objectio.ObjectIterator;
import com.linkedin.dagli.objectio.ObjectReader;
import it.unimi.dsi.fastutil.BigArrays;
import it.unimi.dsi.fastutil.BigList;
import it.unimi.dsi.fastutil.objects.ObjectBigList;
import java.util.stream.Stream;


/**
 * Reads items from a wrapped {@link BigList}.  Changes to the list are reflected in the reader.
 *
 * The implementation assumes that reading the {@link BigList} is a non-blocking operation.
 *
 * @param <T> the type of object stored in the list
 */
public class BigListReader<T> implements ObjectReader<T> {
  private final BigList<T> _list;

  /**
   * Creates a new instance that wraps the provided list.
   * @param existing the existing list to wrap
   */
  public BigListReader(BigList<T> existing) {
    _list = existing;
  }

  /**
   * An iterator that iterates the items in a wrapped {@link BigList}.
   *
   * @param <T> the type of element to iterate upon
   */
  public static class Iterator<T> implements ObjectIterator<T> {
    private long _position = 0;
    private final BigList<T> _list;

    /**
     * Creates a new instance.
     *
     * @param list the BigList whose elements are to be iterated
     */
    public Iterator(BigList<T> list) {
      _list = list;
    }

    @Override
    public long skip(long toSkip) {
      toSkip = Math.min(toSkip, _list.size64() - this._position);
      this._position += toSkip;
      return toSkip;
    }

    @Override
    public boolean hasNext() {
      return this._position < _list.size64();
    }

    @Override
    public T next() {
      return _list.get(_position++);
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
      count = (int) Math.min(_list.size64() - _position, count);

      if (destination.length <= BigArrays.SEGMENT_SIZE && _list instanceof ObjectBigList) {
        ((ObjectBigList) _list).getElements(_position, BigArrays.wrap(destination), offset, count);
        _position += count;
      } else {
        for (int i = 0; i < count; i++) {
          destination[offset + i] = _list.get(_position++);
        }
      }
      return count;
    }

    @Override
    public void close() { }
  }

  @Override
  public long size64() {
    return _list.size64();
  }

  @Override
  public ObjectIterator<T> iterator() {
    return new Iterator<>(_list);
  }

  @Override
  public void close() { }

  @Override
  public Stream<T> stream() {
    return _list.stream();
  }
}
