package com.linkedin.dagli.objectio.biglist;

import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.objectio.ObjectWriter;
import it.unimi.dsi.fastutil.BigArrays;
import it.unimi.dsi.fastutil.BigList;
import it.unimi.dsi.fastutil.objects.ObjectBigArrayBigList;
import it.unimi.dsi.fastutil.objects.ObjectBigList;


/**
 * Writes items to a wrapped BigList.  Changes to one are reflected in the other.
 *
 * @param <T> the type of object stored
 */
public class BigListWriter<T> implements ObjectWriter<T> {
  private final BigList<T> _list;

  /**
   * Creates a new instance that wraps a new {@link ObjectBigArrayBigList} with the specified capacity.
   *
   * @param initialCapacity the initial capacity for the new, wrapped {@link ObjectBigArrayBigList}
   */
  public BigListWriter(long initialCapacity) {
    this(new ObjectBigArrayBigList<>(initialCapacity));
  }

  /**
   * Creates a new instance that wraps a new {@link ObjectBigArrayBigList} with the default capacity.
   */
  public BigListWriter() {
    this(new ObjectBigArrayBigList<>());
  }


  /**
   * Creates a new instance that wraps the provided list.
   * @param existing the existing list to wrap
   */
  public BigListWriter(BigList<T> existing) {
    _list = existing;
  }

  @Override
  public void write(T[] appended, int offset, int count) {
    if (_list instanceof ObjectBigList) {
      ObjectBigList<T> arrayBigList = (ObjectBigList<T>) _list;
      arrayBigList.addElements(_list.size64(), BigArrays.wrap(appended), offset, count);
    } else {
      for (int i = 0; i < count; i++) {
        _list.add(appended[offset + i]);
      }
    }
  }

  @Override
  public ObjectReader<T> createReader() {
    return new BigListReader<>(_list);
  }

  @Override
  public long size64() {
    return _list.size64();
  }

  @Override
  public void write(T appended) {
    _list.add(appended);
  }

  @Override
  public void close() { }
}
