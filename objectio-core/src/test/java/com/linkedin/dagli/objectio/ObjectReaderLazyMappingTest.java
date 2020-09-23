package com.linkedin.dagli.objectio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class ObjectReaderLazyMappingTest {
  @Test
  public void basicTest() {
    ObjectReaderCentury century = new ObjectReaderCentury();
    ObjectReader<String> centuryStrings = century.lazyMap(i -> Integer.toString(i));

    ObjectIterator<String> centuryStringsIterator = centuryStrings.iterator();
    for (int i = 0; i < 100; i++) {
      assertEquals(Integer.toString(i), centuryStringsIterator.next());
    }

    assertFalse(centuryStringsIterator.hasNext());
  }
}
