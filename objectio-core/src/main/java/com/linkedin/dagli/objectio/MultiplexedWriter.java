package com.linkedin.dagli.objectio;

/**
 * ObjectWriter that wraps multiple other ObjectWriters and appends elements by spreading them over the underlying
 * ObjectWriters.  So if we have three underlying ObjectWriters A, B, and C, and are appending the integers
 * 1...7 in order, at the end of these appends each ObjectWriter will have written the following:
 * A: 1, 4, 7
 * B: 2, 5
 * C: 3, 6
 *
 * This may be useful if your appendables are multithreaded and can cache the appended item for processing and then
 * immediately return to the caller.  Some processing (e.g. Kryo serialization) can normally only be handled by a single
 * thread; using this class, then, might allow you to write to multiple ObjectWriters, each operating asynchronously,
 * increasing the degree of multithreading.
 *
 * @param <T> the type of element contained in this ObjectWriter
 */
public class MultiplexedWriter<T> implements ObjectWriter<T> {
  private final ObjectWriter<T>[] _objectWriters;
  private int _nextObjectWriter = 0;

  /**
   * Creates a new instance that will put appended items into the provided ObjectWriters.
   *
   * Any existing elements written by the writers will be viewed as having been added by the multiplexer.  The writer
   * with the fewest elements will be the first one to be added to, and if more than one writer has the fewest
   * elements the one first in the ordering "wins".  The purpose of this is to allow a
   * {@link MultiplexedWriter} to be created, used to add elements to the writers, destroyed, and then
   * re-created with the same writers and continue where it left off.
   *
   * @param objectWriters the writers to which items will be appended
   */
  @SafeVarargs
  public MultiplexedWriter(ObjectWriter<T>... objectWriters) {
    _objectWriters = objectWriters.clone();
    long minItems = Long.MAX_VALUE;
    for (int i = 0; i < _objectWriters.length; i++) {
      if (_objectWriters[i].size64() < minItems) {
        _nextObjectWriter = i;
        minItems = _objectWriters[i].size64();
      }
    }
  }

  @Override
  public void write(T appended) {
    _objectWriters[_nextObjectWriter].write(appended);
    _nextObjectWriter = (_nextObjectWriter + 1) % _objectWriters.length;
  }

  @Override
  public void close() {
    for (ObjectWriter<T> objectWriter : _objectWriters) {
      objectWriter.close();
    }
  }

  @Override
  public ObjectReader<T> createReader() {
    ObjectReader<T>[] readers = new ObjectReader[_objectWriters.length];
    for (int i = 0; i < _objectWriters.length; i++) {
      readers[i] = _objectWriters[i].createReader();
    }

    return new MultiplexedReader<>(readers);
  }

  @Override
  public long size64() {
    long size = 0;
    for (ObjectWriter<T> objectWriter : _objectWriters) {
      size += objectWriter.size64();
    }
    return size;
  }
}
