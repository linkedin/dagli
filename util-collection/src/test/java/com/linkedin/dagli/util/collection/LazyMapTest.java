package com.linkedin.dagli.util.collection;

import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class LazyMapTest {
  @Test
  public void basicTests() {
    LazyMap<Integer, String> lazyMap = new LazyMap<>(String::valueOf);
    lazyMap.putIfAbsent(0);
    lazyMap.put(1, "A");
    Assertions.assertFalse(lazyMap.putIfAbsent(1)); // should have no effect
    lazyMap.putIfAbsent(2);

    HashMap<Integer, String> expected = new HashMap<>();
    expected.put(0, "0");
    expected.put(1, "A");
    expected.put(2, "2");
    Assertions.assertEquals(expected, lazyMap);

    Assertions.assertEquals("A", lazyMap.put(1, "AA"));
    lazyMap.putIfAbsent(3);
    Assertions.assertEquals("3", lazyMap.put(3, "C"));
    lazyMap.remove(1);
    Assertions.assertFalse(lazyMap.containsKey(1));
    lazyMap.putIfAbsent(1);

    Assertions.assertTrue(lazyMap.containsKey(0));
    Assertions.assertTrue(lazyMap.containsKey(1));
    Assertions.assertFalse(lazyMap.containsKey(100));

    expected.put(1, "1");
    expected.put(3, "C");
    Assertions.assertEquals(expected, lazyMap);
  }
}
