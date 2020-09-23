package com.linkedin.dagli.transformer;

import com.linkedin.dagli.dag.LocalDAGExecutor;
import com.linkedin.dagli.object.Rank;
import com.linkedin.dagli.tester.Tester;
import java.util.Arrays;
import java.util.Comparator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class RankTest {
  @Test
  public void testDuplicates1() {
    Tester.of(new Rank().withLimit(3))
        .allParallelInputs(Arrays.asList(2, 1, 3, 1))
        .allOutputs(Arrays.asList(2, 0, 3, 0))
        .preparedTransformerTester(prepped -> assertEquals((int) prepped.apply(4), 3))
        .test();
  }

  @Test
  public void testDuplicates2() {
    Rank rank = new Rank().withLimit(3).withIgnoreDuplicates(true);
    Rank.Prepared prepped = rank.internalAPI().prepare(new LocalDAGExecutor().withMaxThreads(1),
        Arrays.asList(2, 1, 1, 1, 1, 3, 1, 4)).getPreparedTransformerForNewData();

    assertEquals((int) prepped.apply(0), 0);
    assertEquals((int) prepped.apply(1), 0);
    assertEquals((int) prepped.apply(2), 1);
    assertEquals((int) prepped.apply(3), 2);
    assertEquals((int) prepped.apply(4), 3);
    assertEquals((int) prepped.apply(4), 3);
  }

  @Test
  public void test() {
    Rank rank = new Rank();
    Tester.of(rank).input(5).output(0).test();

    rank = rank.withLimit(2).withComparator(Comparator.reverseOrder());
    Rank.Prepared prepped = rank.internalAPI()
        .prepare(new LocalDAGExecutor().withMaxThreads(1), Arrays.asList(5, 1, 3, 2))
        .getPreparedTransformerForNewData();

    assertEquals((int) prepped.apply(6), 0);
    assertEquals((int) prepped.apply(5), 0);
    assertEquals((int) prepped.apply(4), 1);
    assertEquals((int) prepped.apply(3), 1);
    assertEquals((int) prepped.apply(2), 2);
    assertEquals((int) prepped.apply(0), 2);
  }
}
