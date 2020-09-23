package com.linkedin.dagli.objectio;

import com.linkedin.dagli.util.closeable.Closeables;
import java.util.Arrays;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class ObjectReadersTest {
  @Test
  public void equalsTest() {
    ObjectReader<Integer> constantReader = new ConstantReader<>(4, 3);
    ObjectReader<Integer> iterableReader1 = new IterableReader<>(Arrays.asList(4, 4, 4));

    ObjectReader<Integer> iterableReader2 = new IterableReader<>(Arrays.asList(4, 4));
    ObjectReader<Integer> iterableReader3 = new IterableReader<>(Arrays.asList(4, 4, 4, 4));
    ObjectReader<Integer> iterableReader4 = new IterableReader<>(Arrays.asList(4, 4, 4, 3));

    Assertions.assertTrue(ObjectReader.equals(constantReader, constantReader));
    Assertions.assertTrue(ObjectReader.equals(constantReader, iterableReader1));
    Assertions.assertTrue(ObjectReader.equals(null, null));

    Assertions.assertFalse(ObjectReader.equals(constantReader, iterableReader2));
    Assertions.assertFalse(ObjectReader.equals(constantReader, iterableReader3));
    Assertions.assertFalse(ObjectReader.equals(constantReader, iterableReader4));
    Assertions.assertFalse(ObjectReader.equals(iterableReader3, iterableReader4));
    Assertions.assertFalse(ObjectReader.equals(constantReader, null));
    Assertions.assertFalse(ObjectReader.equals(null, constantReader));
  }

  @Test
  public void splitTest() {
    ObjectReader<Integer[]> constantReader = new ConstantReader<>(new Integer[] { 1, 2, 3, 4}, 10);
    ObjectReader<Integer>[] splitReaders = ObjectReader.split(4, constantReader);
    Assertions.assertEquals(10, splitReaders[1].size64());
    Assertions.assertEquals(2, splitReaders[1].iterator().next());
  }

  @Test
  public void testEmpty() {
    Closeables.tryClose(ObjectReader.empty());
    Assertions.assertEquals(0, ObjectReader.wrap(ObjectReader.empty()).size64());
    Assertions.assertFalse(ObjectReader.empty().iterator().hasNext());
    Assertions.assertThrows(NoSuchElementException.class, () -> ObjectReader.empty().iterator().next());
  }

  private static class BadConstantReader<T> extends ConstantReader<T> {

    public BadConstantReader(T obj, long count) {
      super(obj, count);
    }

    @Override
    public void close() {
      throw new IllegalArgumentException();
    }
  }

  @Test
  public void testCloseException() {
    Assertions.assertThrows(RuntimeException.class, () -> Closeables.tryClose(new BadConstantReader(4, 10)));
  }


  @Test
  public void testConcatenate() {
    ConcatenatedReader<Integer> concatenated =
        ObjectReader.concatenate(Integer[]::new, new ConstantReader<>(1, 4), new ConstantReader<>(2, 4));

    Integer[] firstRecord = concatenated.iterator().next();
    Assertions.assertEquals(1, firstRecord[0]);
    Assertions.assertEquals(2, firstRecord[1]);

    ObjectReader<Integer>[] split = ObjectReader.split(2, concatenated);
    Assertions.assertArrayEquals(split, concatenated.getObjectReaders());
  }

}
