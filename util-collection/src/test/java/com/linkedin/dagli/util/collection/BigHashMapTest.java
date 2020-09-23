package com.linkedin.dagli.util.collection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Tests the {@link BigHashMap}.
 */
public class BigHashMapTest {
  @Test
  public void test() {
    ArrayList<HashMap<Integer, Double>> sourceMaps = new ArrayList<HashMap<Integer, Double>>();
    sourceMaps.add(new HashMap<>());

    HashMap<Integer, Double> singletonMap = new HashMap<>();
    singletonMap.put(1, 5.0);
    sourceMaps.add(singletonMap);

    HashMap<Integer, Double> smallMap = new HashMap<>();
    for (int i = 0; i < 5; i++) {
      smallMap.put(i, (double) i);
    }
    sourceMaps.add(smallMap);

    HashMap<Integer, Double> largeMap = new HashMap<>();
    for (int i = 0; i < 1000; i++) {
      largeMap.put(i, (double) i);
    }
    sourceMaps.add(largeMap);

    ArrayList<BigHashMap<Integer, Double>> targetMaps = new ArrayList<>();
    targetMaps.add(new BigHashMap<>(Integer.class, BigHashMapTest::hash, 1));
    targetMaps.add(new BigHashMap<>(Integer.class, BigHashMapTest::hash, 1, 0.1));
    targetMaps.add(new BigHashMap<>(Integer.class, BigHashMapTest::hash, 1, 10));
    targetMaps.add(new BigHashMap<>(Integer.class, BigHashMapTest::hash, 7));
    targetMaps.add(new BigHashMap<>(Integer.class, BigHashMapTest::hash, 100, 0.1));
    targetMaps.add(new BigHashMap<>(Integer.class, BigHashMapTest::hash, 100, 10));

    for (HashMap<Integer, Double> sourceMap : sourceMaps) {
      for (BigHashMap<Integer, Double> targetMap : targetMaps) {
        test(sourceMap, targetMap);
      }
    }
  }

  private static long hash(Integer integer) {
    return integer;
  }

  private void test(Map<Integer, Double> entries, BigHashMap<Integer, Double> bigHashMap) {
    bigHashMap = serde(bigHashMap);
    Assertions.assertTrue(bigHashMap.isEmpty());
    bigHashMap.clear();
    bigHashMap = serde(bigHashMap);
    Assertions.assertTrue(bigHashMap.isEmpty());

    bigHashMap.putAll(entries);
    Assertions.assertEquals(entries.size(), bigHashMap.size());
    Assertions.assertEquals(entries.size(), bigHashMap.size64());
    Assertions.assertEquals(entries, bigHashMap);
    Assertions.assertEquals(bigHashMap, entries);

    bigHashMap = serde(bigHashMap);
    Assertions.assertEquals(entries.size(), bigHashMap.size());
    Assertions.assertEquals(entries.size(), bigHashMap.size64());
    Assertions.assertEquals(entries, bigHashMap);
    Assertions.assertEquals(bigHashMap, entries);

    bigHashMap.computeIfAbsent(0, k -> null); // should be a no-op
    Assertions.assertEquals(entries, bigHashMap);

    Assertions.assertEquals(1.0, bigHashMap.computeIfAbsent(Integer.MAX_VALUE, k -> 1.0));
    Assertions.assertEquals(1.0, bigHashMap.computeIfAbsent(Integer.MAX_VALUE, k -> 10.0));

    Assertions.assertEquals(entries.size(), bigHashMap.size() - 1);
    Assertions.assertEquals(entries.size(), bigHashMap.size64() - 1);
    Assertions.assertNotEquals(entries, bigHashMap);
    Assertions.assertNotEquals(bigHashMap, entries);

    Assertions.assertEquals(1.0, bigHashMap.getOrDefault(Integer.MAX_VALUE, 2.0));
    Assertions.assertEquals(1.0, bigHashMap.remove(Integer.MAX_VALUE));
    Assertions.assertEquals(2.0, bigHashMap.getOrDefault(Integer.MAX_VALUE, 2.0));

    Assertions.assertEquals(entries.size(), bigHashMap.size());
    Assertions.assertEquals(entries.size(), bigHashMap.size64());
    Assertions.assertEquals(entries, bigHashMap);
    Assertions.assertEquals(bigHashMap, entries);
    Assertions.assertEquals(entries.hashCode(), bigHashMap.hashCode());

    Assertions.assertEquals(entries.entrySet(), bigHashMap.entrySet());
    Assertions.assertEquals(entries.keySet(), bigHashMap.keySet());
    Assertions.assertEquals(bigHashMap.entrySet(), entries.entrySet());
    Assertions.assertEquals(bigHashMap.keySet(), entries.keySet());
    Assertions.assertEquals(new HashSet<>(entries.values()), new HashSet<>(bigHashMap.values()));

    bigHashMap = serde(bigHashMap);
    Assertions.assertEquals(entries.entrySet(), bigHashMap.entrySet());
    Assertions.assertEquals(entries.keySet(), bigHashMap.keySet());
    Assertions.assertEquals(bigHashMap.entrySet(), entries.entrySet());
    Assertions.assertEquals(bigHashMap.keySet(), entries.keySet());
    Assertions.assertEquals(new HashSet<>(entries.values()), new HashSet<>(bigHashMap.values()));

    bigHashMap.clear();
    Assertions.assertTrue(bigHashMap.isEmpty());
  }

  @SuppressWarnings("unchecked")
  private BigHashMap<Integer, Double> serde(BigHashMap<Integer, Double> map) {
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(bos);
      oos.writeObject(map);
      oos.close();

      ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
      ObjectInputStream ois = new ObjectInputStream(bis);
      return (BigHashMap<Integer, Double>) ois.readObject();
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
