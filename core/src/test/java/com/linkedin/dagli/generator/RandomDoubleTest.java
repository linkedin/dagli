package com.linkedin.dagli.generator;

import com.linkedin.dagli.tester.GeneratorTestBuilder;
import com.linkedin.dagli.tester.Tester;
import org.junit.jupiter.api.Test;


/**
 * Tests for {@link RandomDouble}.
 */
public class RandomDoubleTest {
  @Test
  public void test() {
    GeneratorTestBuilder<Double, RandomDouble> tester = Tester.of(new RandomDouble());
    for (int i = 0; i < 1000; i++) {
      tester = tester.outputTest(r -> r >= 0 && r <= 1);
    }
    tester.test();
  }
}
