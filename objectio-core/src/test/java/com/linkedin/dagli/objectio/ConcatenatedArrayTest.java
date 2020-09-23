package com.linkedin.dagli.objectio;

import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class ConcatenatedArrayTest {
  @Test
  public void test() {
    ObjectReader<Integer> a = ObjectReader.wrap(Arrays.asList(1, 2, 3));
    ObjectReader<Integer> b = ObjectReader.wrap(Arrays.asList(0, 0, 0));
    ObjectReader<Integer> c = ObjectReader.wrap(Arrays.asList(3, 0, -3));

    ConcatenatedReader<Integer> bica = new ConcatenatedReader<>(Integer[]::new, a, b, c);
    try (ConcatenatedReader.Iterator<Integer> iterator = bica.iterator()) {
      Assertions.assertArrayEquals(new Integer[] {1, 0, 3}, iterator.next());
      Assertions.assertArrayEquals(new Integer[] {2, 0, 0}, iterator.next());
      Assertions.assertArrayEquals(new Integer[] {3, 0, -3}, iterator.next());
      Assertions.assertFalse(iterator.hasNext());
    }
  }
}
