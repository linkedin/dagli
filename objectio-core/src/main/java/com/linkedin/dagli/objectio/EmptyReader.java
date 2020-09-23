package com.linkedin.dagli.objectio;

/**
 * The empty reader singleton.
 */
enum EmptyReader implements ObjectReader {
  INSTANCE;

  @Override
  public long size64() {
    return 0;
  }

  @Override
  public ObjectIterator iterator() {
    return ObjectIterator.empty();
  }

  @Override
  public void close() { }
}
