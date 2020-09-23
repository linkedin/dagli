package com.linkedin.dagli.objectio;

import it.unimi.dsi.fastutil.Size64;


public interface ObjectWriter<T> extends AutoCloseable, Size64 {
  /**
   * Creates a reader that can read the data written by this writer, once the writer is closed.  If called before the
   * writer is closed and the writer does not support retrieving a "partial" reader, an {@link IllegalStateException}
   * should be thrown.  If a reader is returned, it must be able to read all the elements written thus far; whether or
   * not the reader can read elements written subsequently is implementation-dependent.
   *
   * @return an {@link ObjectReader} that can read all the elements written thus far
   * @throws IllegalStateException if the writer has not been closed and does not support creating "partial" readers
   */
  ObjectReader<T> createReader();

  /**
   * Gets the number of elements that have been written to this ObjectWriter.  For ObjectWriters that are appending to
   * data not written by this instance, this should include the number of pre-existing items, too.
   *
   * In other words, the returned size should match the size64() of the {@link ObjectReader} that would be returned by
   * {@link ObjectWriter#createReader()}.
   *
   * @return the number of elements written
   */
  @Override
  long size64();

  /**
   * Appends items to this {@link ObjectWriter}
   *
   * @param appended the source array from which items will be copied
   */
  default void writeAll(T[] appended) {
    write(appended, 0, appended.length);
  }

  /**
   * Appends items to this {@link ObjectWriter}
   *
   * @param appended the source array from which items will be copied
   */
  default void writeAll(Iterable<T> appended) {
    appended.forEach(this::write);
  }

  /**
   * Appends a single item to this {@link ObjectWriter}
   *
   * @param appended the item to be appended
   */
  void write(T appended);

  /**
   * Appends elements from a {@link ObjectIterator}.
   *
   * @param iterator the iterator from which elements will be retrieved
   * @param maxToAppend the maximum number of elements to retrieve and append.
   *
   * @return the actual number of elements appended, which may be less than maxToAppend if the iterator is exhausted
   */
  default long write(ObjectIterator<T> iterator, long maxToAppend) {
    T[] buff = (T[]) new Object[(int) Math.min(maxToAppend, Math.max(128, Math.min(4096, maxToAppend / 16)))];

    int read;
    long total = 0;
    while ((read = iterator.next(buff, 0, (int) Math.min(buff.length, maxToAppend - total))) > 0) {
      write(buff, 0, read);
      total += read;
    }

    return total;
  }

  /**
   * Appends items to this {@link ObjectWriter}
   * @param appended the source array from which items will be copied
   * @param offset where copying begins in the source array
   * @param count the number of items to copy
   */
  default void write(T[] appended, int offset, int count) {
    for (int i = 0; i < count; i++) {
      write(appended[offset + i]);
    }
  }

  /**
   * Called when no more items will be appended.  Further operations on this ObjectWriter, except for calls to
   * createReader, are undefined.
   */
  @Override
  void close();

  /**
   * Casts an instance to an effective "supertype" interface.  The semantics of {@link ObjectWriter} guarantee that
   * the returned type is valid for the instance.
   *
   * Note that although this and other {@code cast(...)} methods are safe, this safety extends only to the interfaces
   * for which they are implemented.  The covariance and contravariance relationships existing for these interfaces do
   * not necessarily hold for their implementing classes.
   *
   * @param writer the instance to cast
   * @param <R> the type of item written by the returned writer
   * @return the passed writer, typed to a new "supertype" interface of the original
   */
  @SuppressWarnings("unchecked")
  static <R> ObjectWriter<R> cast(ObjectWriter<? super R> writer) {
    return (ObjectWriter<R>) writer;
  }
}
