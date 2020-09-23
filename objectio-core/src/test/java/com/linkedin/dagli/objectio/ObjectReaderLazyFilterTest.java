package com.linkedin.dagli.objectio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class ObjectReaderLazyFilterTest {
  @Test
  public void basicTest() {
    ObjectReaderCentury century = new ObjectReaderCentury();
    ObjectReader<Integer> evens = century.lazyFilter(i -> i % 2 == 0);

    ObjectIterator<Integer> evensIterator = evens.iterator();
    for (int i = 0; i < 100; i += 2) {
      assertEquals(i, (int) evensIterator.next());
    }

    assertFalse(evensIterator.hasNext());
  }
}
