package com.linkedin.dagli.object;

import com.linkedin.dagli.tester.Tester;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;


public class OrderStatisticTest {
  @Test
  public void test() {
    List<String> items = Arrays.asList("A", "B", "C");
    long[] cumulativeCounts = new long[] { 1, 3, 7 };

    Tester.of(new OrderStatistic<>().withKthLargest(0))
        .input(items, cumulativeCounts, 1)
        .output("C")
        .input(items, cumulativeCounts, 5)
        .output("B")
        .input(items, cumulativeCounts, 7)
        .output("A")
        .test();

    Tester.of(new OrderStatistic<>().withKthSmallest(0))
        .input(items, cumulativeCounts, 7)
        .output("C")
        .input(items, cumulativeCounts, 4)
        .output("C")
        .input(items, cumulativeCounts, 2)
        .output("B")
        .input(items, cumulativeCounts, 1)
        .output("A")
        .test();

    Tester.of(new OrderStatistic<>().withPercentile(0))
        .input(items, cumulativeCounts, 1.0)
        .output("C")
        .input(items, cumulativeCounts, 0.5)
        .output("C")
        .input(items, cumulativeCounts, 0.22)
        .output("B")
        .input(items, cumulativeCounts, 0.1)
        .output("A")
        .input(items, cumulativeCounts, 0)
        .output("A")
        .test();
  }
}
