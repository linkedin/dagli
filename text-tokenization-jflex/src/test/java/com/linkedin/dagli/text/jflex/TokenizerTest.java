package com.linkedin.dagli.text.jflex;

import java.util.Locale;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class TokenizerTest {
  @Test
  public void test() {
    test(Locale.ENGLISH);
    test(Locale.GERMAN); // German tokenizer should do fine on our English example
    test(Locale.JAPAN); // will fall back to generic rules; actually Japanese tokenization will be quite poor!
  }

  private static void test(Locale locale) {
    JFlexTokenizer tokenizer = new JFlexTokenizer(locale);
    java.util.List<String> tokens = tokenizer.tokenize("The quick brown fox.");
    Assertions.assertEquals(tokens.get(0), "The");
    Assertions.assertEquals(tokens.get(1), "quick");
    Assertions.assertEquals(tokens.get(2), "brown");
    Assertions.assertEquals(tokens.get(3), "fox");
    Assertions.assertEquals(tokens.get(4), ".");
  }
}
