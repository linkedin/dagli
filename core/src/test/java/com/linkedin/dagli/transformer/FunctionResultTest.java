package com.linkedin.dagli.transformer;

import com.linkedin.dagli.function.FunctionResult2;
import com.linkedin.dagli.tester.Tester;
import org.junit.jupiter.api.Test;

public class FunctionResultTest {
  @Test
  public void test() {
    FunctionResult2<Integer, Integer, Integer> transformer = new FunctionResult2<>(Math::max);
    Tester.of(transformer).input(-4, 20).output(20).input(3, 7).output(7).test();
  }
}