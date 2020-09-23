package com.linkedin.dagli.text;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class NormalizedUnicodeTest {
  @Test
  public void test() {
    Assertions.assertEquals("ff", new NormalizedUnicode().apply("ﬀ"));
  }
}
