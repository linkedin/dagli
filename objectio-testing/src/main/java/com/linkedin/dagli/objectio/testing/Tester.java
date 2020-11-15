package com.linkedin.dagli.objectio.testing;

import com.linkedin.dagli.objectio.ObjectIterator;
import com.linkedin.dagli.objectio.ObjectIteratorNext;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.objectio.ObjectWriter;
import com.linkedin.dagli.util.collection.Iterables;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Provides generic testing routines for ObjectIO readers and writers.
 */
public abstract class Tester {
  private static final int TEST_LIST_SIZE = 100;

  private Tester() { }

  /**
   * A simple type to be read/written during testing.
   */
  private static class Value implements Serializable {
    private final int _value;

    @Override
    public int hashCode() {
      return Integer.hashCode(_value);
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof Value) {
        return (((Value) other)._value == _value);
      }

      return false;
    }

    /**
     * Trivial constructor needed by Kryo for deserialization.
     */
    private Value() {
      this(0);
    }

    /**
     * Create a new instance that stores the provided value.
     *
     * @param val a value to store
     */
    Value(int val) {
      _value = val;
    }
  }

  /**
   * Create a generic list of values for testing
   * @return a generic list of values for testing
   */
  private static List<Object> createTestList() {
    List<Object> result = new ArrayList<>(TEST_LIST_SIZE);
    for (int i = 0; i < TEST_LIST_SIZE; i++) {
      result.add(i % 2 == 0 ? i : new Value(i));
    }

    return result;
  }

  /**
   * Runs basic tests on an {@link ObjectWriter} to ensure that values can be written and read back via the associated
   * reader.
   *
   * @param writer the writer to test (which must support writing arbitrary Serializable objects)
   */
  public static void testWriter(ObjectWriter<Object> writer) {
    testWriter(writer, createTestList());
  }
  /**
   * Runs basic tests on an {@link ObjectWriter} to ensure that values can be written and read back via the associated
   * reader.
   *
   * @param writer the writer to test
   * @param toWrite the list of items to be written and read back as part of the test
   */
  public static <T> void testWriter(ObjectWriter<T> writer, List<T> toWrite) {
    int size = toWrite.size();
    int halfSize = size / 2;
    @SuppressWarnings("unchecked")
    T[] toWriteArray = (T[]) toWrite.toArray(); // masquerade this Object[] as a T[]

    assertEquals(writer.size64(), 0);

    writer.write(toWriteArray, 0, halfSize);

    assertEquals(writer.size64(), halfSize);

    writer.write(toWriteArray, halfSize, size - halfSize);
    assertEquals(writer.size64(), size);
    writer.close();
    ObjectReader<T> reader = writer.createReader();

    assertEquals(size, reader.size64());
    testIterators(reader, toWrite);
  }

  /**
   * Checks that the contents of a {@link ObjectIterator} match those provided.
   *
   * @param reader the reader providing the iterators to test
   * @param contents the contents the iterator should contain
   */
  public static <T> void testIterators(ObjectReader<T> reader, List<T> contents) {
    try (ObjectIterator<T> iterator = reader.iterator()) {
      testIterator(iterator, ObjectIterator::next, contents);
    }
    try (ObjectIterator<T> iterator = reader.iterator()) {
      testIterator(iterator, ObjectIterator::nextAvailable, contents);
    }
    try (ObjectIterator<T> iterator = reader.iterator()) {
      testIterator(iterator, Tester::oneAtATime, contents);
    }
  }

  private static <T> int oneAtATime(ObjectIterator<T> iterator, Object[] buffer, int offset, int count) {
    for (int i = 0; i < count; i++) {
      if (!iterator.hasNext()) {
        return i;
      }
      buffer[i + offset] = iterator.next();
    }
    return count;
  }

  /**
   * Checks that the contents of a {@link ObjectIterator} match those provided.
   *
   * @param it the iterator to test
   * @param contents the contents the iterator should contain
   * @param <T> the type of element in the iterator being tested
   */
  @SuppressWarnings("unchecked") // ObjectIterators allow use of Object[] masquerading as T[]
  public static <T> void testIterator(ObjectIterator<T> it, ObjectIteratorNext<T> nextMethod, List<T> contents) {
    T[] buff = (T[]) new Object[10];
    Iterator<T> cIt = contents.iterator();
    int read;
    while ((read = nextMethod.next(it, buff, 0, 10)) > 0) {
      for (int i = 0; i < read; i++) {
        assertEquals(cIt.next(), buff[i]);
      }

      // interject with try-reads to make testing more comprehensive
      int tryRead = it.tryNextAvailable(buff, 0, 1); // may return -1, 0, or 1
      if (tryRead == 1) { // if we managed to actually read something...
        assertEquals(cIt.next(), buff[0]);
      }
    }

    assertEquals(0, it.next(buff, 0, buff.length));
    assertEquals(0, it.nextAvailable(buff, 0, buff.length));
    assertTrue(it.tryNextAvailable(buff, 0, buff.length) <= 0);
    assertFalse(it.hasNext());
  }

  /**
   * Checks that the contents of an {@link ObjectReader} match those provided.
   *
   * @param reader the reader to test
   * @param contents the contents the reader should contain
   * @param <T> the type of element in the reader being tested
   */
  public static <T> void testReader(ObjectReader<T> reader, List<T> contents) {
    assertEquals(reader.size64(), contents.size());
    testIterators(reader, contents);

    assertEquals(contents, reader.stream().collect(Collectors.toList()));
    assertEquals(contents, Iterables.concatenate(reader.batchStream(2).toArray(Collection[]::new)));
  }
}
