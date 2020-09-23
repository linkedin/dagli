package com.linkedin.dagli.objectio;

import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class ObjectReaderSampleTest {
  @Test
  public void testSampling() {
    ObjectReaderCentury century = new ObjectReaderCentury();

    for (int i = 0; i < 1000; i++) {
      ObjectReader<Integer> sample1 = century.sample(0, 0.7, i);
      ObjectReader<Integer> sample2 = century.sample(0.7, 1, i);

      Assertions.assertTrue(Collections.disjoint(sample1.toCollection(), sample2.toCollection()));
      Assertions.assertEquals(sample1.size64() + sample2.size64(), 100);

      Assertions.assertEquals(century.sample(new SampleSegment(1, 1)).size64(), 0);
      Assertions.assertEquals(century.sample(new SampleSegment(1, 1).complement()).size64(), 100);
    }

    ObjectReader<Integer> sample3 = century.sample(new SampleSegment(0, 1));
    Integer[] dest = new Integer[200];
    sample3.iterator().next(dest, 0, 10);
    Assertions.assertEquals(0, dest[0]);
    Assertions.assertEquals(9, dest[9]);
    Assertions.assertEquals(100, sample3.iterator().next(dest, 0, 200));

    // check that the close methods don't throw:
    sample3.iterator().close();
    sample3.close();
  }
}
