package com.linkedin.dagli.objectio;

import java.util.NoSuchElementException;


/**
 * The empty iterator singleton.
 */
enum EmptyIterator implements ObjectIterator {
  INSTANCE;

  @Override
  public boolean hasNext() {
    return false;
  }

  @Override
  public Object next() {
    throw new NoSuchElementException();
  }

  @Override
  public void close() { }
}
