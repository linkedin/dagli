package com.linkedin.dagli.generator;

import com.linkedin.dagli.tester.Tester;
import org.junit.jupiter.api.Test;


/**
 * Tests for {@link Constant}s.
 */
public class ConstantTest {
  @Test
  public void test() {
    Tester.of(new Constant<>(null)).output(null).test();
    Tester.of(new Constant<>(6)).output(6).resultSupertype(Integer.class).test();
  }
}
