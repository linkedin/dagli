package com.linkedin.dagli.objectio;

import java.util.NoSuchElementException;


public class ObjectReaderCentury implements ObjectReader<Integer> {
  @Override
  public long size64() {
    return 100;
  }

  public static class Iterator implements ObjectIterator<Integer> {
    private int _current = 0;
    @Override
    public boolean hasNext() {
      return _current < 100;
    }

    public Integer next() {
      if (_current >= 100) {
        throw new NoSuchElementException();
      }

      return _current++;
    }

    @Override
    public int tryNextAvailable(Object[] destination, int offset, int count) {
      int limit = Math.min(100 - _current, count);

      for (int i = offset; i < offset + limit; i++) {
        destination[i] = _current++;
      }

      return limit;
    }

    @Override
    public void close() {

    }
  }

  @Override
  public ObjectIterator<Integer> iterator() {
    return new Iterator();
  }

  @Override
  public void close() {

  }
}
