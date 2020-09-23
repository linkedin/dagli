package com.linkedin.dagli.objectio;

import java.util.Collection;


/**
 * Wraps a {@link Collection} as an {@link ObjectWriter}.  Changes to the {@link CollectionWriter} will affect
 * the wrapped collection, and vice-versa.
 * @param <T>
 */
public class CollectionWriter<T> implements ObjectWriter<T> {
  private Collection<T> _collection;

  /**
   * Create a new wrapper around the given collection.
   *
   * @param collection the collection to be wrapped
   */
  public CollectionWriter(Collection<T> collection) {
    _collection = collection;
  }

  /**
   * @return the underlying collection
   */
  public Collection<T> getCollection() {
    return _collection;
  }

  @Override
  public void write(T appended) {
    _collection.add(appended);
  }

  @Override
  public ObjectReader<T> createReader() {
    return new IterableReader<>(_collection, _collection.size());
  }

  @Override
  public long size64() {
    return _collection.size();
  }

  /**
   * Closing has no effect on the underlying collection.
   */
  @Override
  public void close() { }
}
