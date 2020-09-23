package com.linkedin.dagli.objectio;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Spliterator;
import java.util.stream.Stream;


/**
 * A collection that wraps an {@link ObjectReader} (and also implements the {@link ObjectReader} interface).
 * The wrapped reader must have no more than Integer.MAX_VALUE elements.
 *
 * @param <T> the type of element in the collection
 */
class ReaderAsCollection<T> implements Collection<T>, ObjectReader<T> {
  private ObjectReader<T> _objectReader;

  /**
   * Creates a new instance.
   *
   * @param objectReader the reader to be wrapped as a collection; must have no more than Integer.MAX_VALUE elements.
   */
  public ReaderAsCollection(ObjectReader<T> objectReader) {
    if (objectReader.size64() > Integer.MAX_VALUE) {
      throw new IndexOutOfBoundsException();
    }

    _objectReader = objectReader;
  }

  /**
   * Gets the size of this collection.
   *
   * @return the size of this collection
   * @throws IndexOutOfBoundsException if the underlying reader has somehow grown to more than Integer.MAX_VALUE
   *         elements
   */
  @Override
  @SuppressWarnings("deprecation") // need to define deprecated size() method to implement Collection interface
  public int size() {
    long size64 = size64();
    if (size64 > Integer.MAX_VALUE) {
      throw new IndexOutOfBoundsException("Reader contains more than Integer.MAX_VALUE elements");
    }
    return (int) size64;
  }

  @Override
  public boolean isEmpty() {
    return size64() == 0;
  }

  @Override
  public boolean contains(Object o) {
    try (ObjectIterator<T> iterator = _objectReader.iterator()) {
      int size = size();
      for (int i = 0; i < size; i++) {
        if (iterator.next().equals(o)) {
          return true;
        }
      }
      return false;
    }
  }

  @Override
  public long size64() {
    return _objectReader.size64();
  }

  @Override
  public ObjectIterator<T> iterator() {
    return _objectReader.iterator();
  }

  @Override
  public void close() {
    _objectReader.close();
  }

  @SuppressWarnings("unchecked") // safe per nextAvailable(...) spec
  private Object[] fill(Object[] arr) {
    try (ObjectIterator<T> iter = _objectReader.iterator()) {
      int size = size();
      int toCopy = size;
      while (toCopy > 0) {
        toCopy -= iter.nextAvailable((T[]) arr, size - toCopy, toCopy);
      }

      return arr;
    }
  }

  @Override
  public Object[] toArray() {
    return fill(new Object[size()]);
  }

  @Override
  public <U> U[] toArray(U[] a) {
    if (a.length >= size()) {
      return (U[]) fill(a);
    } else {
      return (U[]) fill(Arrays.copyOf(a, size()));
    }
  }

  @Override
  public boolean add(T t) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    HashSet<?> set = new HashSet<>(c);
    try (ObjectIterator<T> iter = iterator()) {
      while (iter.hasNext()) {
        set.remove(iter.next());
        if (set.isEmpty()) {
          return true;
        }
      }
      return false;
    }
  }

  @Override
  public boolean addAll(Collection<? extends T> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Stream<T> stream() {
    return _objectReader.stream();
  }

  @Override
  public Spliterator<T> spliterator() {
    return _objectReader.spliterator();
  }
}