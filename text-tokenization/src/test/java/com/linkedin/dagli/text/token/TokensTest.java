package com.linkedin.dagli.text.token;

import com.linkedin.dagli.tester.Tester;
import java.util.Arrays;
import java.util.Locale;
import org.junit.jupiter.api.Test;


public class TokensTest {
  @Test
  public void test() {
    Tester.of(new Tokens())
        .input(Locale.US, "That is a large pizza")
        .output(Arrays.asList("That", "is", "a", "large", "pizza"))
        .input(Locale.UK, "That is a large pizza")
        .output(Arrays.asList("That", "is", "a", "large", "pizza"))
        .input(Locale.forLanguageTag("es"), "Como estas mi hermano")
        .output(Arrays.asList("Como", "estas", "mi", "hermano"))
        .test();
  }
}
