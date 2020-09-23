package com.linkedin.dagli.object;

import com.linkedin.dagli.tester.Tester;
import java.util.Arrays;
import org.junit.jupiter.api.Test;


public class BucketIndexTest {
  @Test
  public void test() {
    Tester.of(new BucketIndex().withBucketCount(3))
        .allParallelInputs(Arrays.asList(2, 2, 2, 1, 1, 3, 3, 3, 3))
        .allOutputs(Arrays.asList(1, 1, 1, 0, 0, 2, 2, 2, 2))
        .test();

    Tester.of(new BucketIndex().withBucketCount(2))
        .allParallelInputs(Arrays.asList(2, 2, 2, 1, 1, 3, 3, 3, 3))
        .allOutputs(Arrays.asList(0, 0, 0, 0, 0, 1, 1, 1, 1))
        .test();

    Tester.of(new BucketIndex().withBucketCount(1))
        .allParallelInputs(Arrays.asList(2, 2, 2, 1, 1, 3, 3, 3, 3))
        .allOutputs(Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0))
        .test();
  }
}
