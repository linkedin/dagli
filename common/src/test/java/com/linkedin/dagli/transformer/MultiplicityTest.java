package com.linkedin.dagli.transformer;

import com.linkedin.dagli.tester.Tester;
import com.linkedin.dagli.object.Multiplicity;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class MultiplicityTest {
  @Test
  public void test() {
    Tester.of(new Multiplicity())
        .allParallelInputs(Arrays.asList("a", "a", "a", "b", "b", "c"))
        .allOutputs(Arrays.asList(3L, 3L, 3L, 2L, 2L, 1L))
        .preparedTransformerTester(prepared -> Assertions.assertEquals(prepared.apply("d"), 0))
        .test();
  }
}
