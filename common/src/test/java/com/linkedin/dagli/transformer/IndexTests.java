package com.linkedin.dagli.transformer;

import com.linkedin.dagli.object.Index;
import com.linkedin.dagli.object.UnknownItemPolicy;
import com.linkedin.dagli.tester.Tester;
import java.util.Arrays;
import org.junit.jupiter.api.Test;


public class IndexTests {
  @Test
  public void test() {
    Index<?> preparable = new Index<>().withMaxUniqueObjects(2)
        .withMaxUniqueObjectsDuringPreparation(Integer.MAX_VALUE)
        .withUnknownItemPolicy(UnknownItemPolicy.NEW);

    Tester.of(preparable)
        .allParallelInputs(Arrays.asList("F", "E", "B", "D", "C", "A", "A", "A", "B"))
        .allOutputs(Arrays.asList(2, 2, 1, 2, 2, 0, 0, 0, 1))
        .test();

    Tester.of(preparable.withUnknownItemPolicy(UnknownItemPolicy.DISTINCT))
        .allParallelInputs(Arrays.asList("F", "E", "B", "D", "C", "A", "A", "A", "B"))
        .allOutputs(Arrays.asList(2, 2, 1, 2, 2, 0, 0, 0, 1))
        .test();

    Tester.of(preparable.withUnknownItemPolicy(UnknownItemPolicy.MOST_FREQUENT))
        .allParallelInputs(Arrays.asList(1, 2, 2, 3, 3, 3, 4, 4, 4, 4))
        .allOutputs(Arrays.asList(0, 0, 0, 1, 1, 1, 0, 0, 0, 0))
        .test();

    Tester.of(preparable.withUnknownItemPolicy(UnknownItemPolicy.LEAST_FREQUENT))
        .allParallelInputs(Arrays.asList(1, 2, 2, 3, 3, 3, 4, 4, 4, 4))
        .allOutputs(Arrays.asList(1, 1, 1, 1, 1, 1, 0, 0, 0, 0))
        .test();

    Tester.of(preparable)
        .input(1)
        .output(0)
        .test();
  }
}
