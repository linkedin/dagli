package com.linkedin.dagli.objectio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class ObjectReaderLazyShuffleTest {
  @Test
  public void basicTest() {
    ObjectReaderCentury century = new ObjectReaderCentury();
    ObjectReader<Integer> shuffled = century.lazyShuffle(3);

    boolean[] seen = new boolean[100];
    try (ObjectIterator<Integer> iterator = shuffled.iterator()) {
      while (iterator.hasNext()) {
        int next = iterator.next();
        assertFalse(seen[next]);
        seen[next] = true;
      }
    }

    for (int i = 0; i < 100; i++) {
      assertTrue(seen[i]);
    }
  }
}
