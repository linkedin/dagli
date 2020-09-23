package com.linkedin.dagli.text;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class AccentlessTest {
  @Test
  public void test() {
    Accentless accentless = new Accentless();
    Assertions.assertEquals("Como estas", accentless.apply("Cómo estás"));
    Assertions.assertEquals("👍🏼", accentless.apply("👍🏼"));
    Assertions.assertEquals("Motley Crue", accentless.apply("Mötley Crüe"));

    // make sure different ways of expressing "à" transformer to "a"
    // (thanks to @mscholle for this example)
    Assertions.assertEquals("a", accentless.apply("\u0061\u0300"));
    Assertions.assertEquals("a", accentless.apply("\u00E0"));

    // test ó, too
    Assertions.assertEquals("o", accentless.apply("\u00F3"));
    Assertions.assertEquals("o", accentless.apply("o\u0300"));
  }
}
