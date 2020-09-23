package com.linkedin.dagli.objectio;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class ObjectReaderLazyFlatMappingTest {
  @Test
  public void basicTest() {
    ObjectReaderCentury century = new ObjectReaderCentury();
    ObjectReader<Integer> flatMapped =
        century.lazyFlatMap(i -> IntStream.range(0, i % 3).boxed().collect(Collectors.toList()));

    ObjectIterator<Integer> flatIterator = flatMapped.iterator();
    int count = 0;
    for (int i = 0; i < 100; i++) {
      for (int j = 0; j < (i % 3); j++) {
        assertEquals(j, (int) flatIterator.next());
        count++;
      }
    }
    assertFalse(flatIterator.hasNext());
    assertEquals(count, flatMapped.size64());
  }
}
