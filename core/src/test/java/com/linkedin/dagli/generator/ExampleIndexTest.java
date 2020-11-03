package com.linkedin.dagli.generator;

import com.linkedin.dagli.tester.Tester;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;


/**
 * Tests for {@link ExampleIndex}.
 */
public class ExampleIndexTest {
  @Test
  public void test() {
    Tester.of(new ExampleIndex()).allOutputs(LongStream.range(0, 10).boxed().collect(Collectors.toList())).test();
  }
}
