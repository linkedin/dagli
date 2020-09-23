package com.linkedin.dagli.objectio;

import com.linkedin.dagli.objectio.testing.Tester;
import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class MultiplexingTest {
  @Test
  public void genericTest() {
    CollectionWriter<Object> writer1 = new CollectionWriter<>(new ArrayList<>());
    CollectionWriter<Object> writer2 = new CollectionWriter<>(new ArrayList<>());

    MultiplexedWriter<Object> multiwriter = new MultiplexedWriter<>(writer1, writer2);
    Tester.testWriter(multiwriter);
  }

  @Test
  public void testMultiplexing() {
    CollectionWriter<Integer> writer1 = new CollectionWriter<>(new ArrayList<>());
    CollectionWriter<Integer> writer2 = new CollectionWriter<>(new ArrayList<>());

    MultiplexedWriter<Integer> multiwriter = new MultiplexedWriter<>(writer1, writer2);
    for (int i = 0; i < 10; i++) {
      multiwriter.write(i);
    }
    Assertions.assertEquals(10, multiwriter.size64());
    Assertions.assertEquals(5, writer1.size64());

    multiwriter.close();
    try (ObjectReader<Integer> reader = multiwriter.createReader()) {
      try (ObjectIterator<Integer> iterator = reader.iterator()) {
        for (int i = 0; i < 10; i++) {
          Assertions.assertEquals(i, iterator.next());
        }
      }
    }
  }
}
