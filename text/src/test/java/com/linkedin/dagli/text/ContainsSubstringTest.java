package com.linkedin.dagli.text;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class ContainsSubstringTest {
  @Test
  public void test() {
    List<String> strings = Arrays.asList("cat", "DOG", "Sheep", "cow");
    ContainsSubstring cs = new ContainsSubstring().withSubstrings(strings);

    String sentence1 = "The cat, chased by the sheepdog, had a cow, man";
    String sentence2 = "The sheepdog liked the taste of cats.";
    String sentence3 = "sheep count people to go to sleep.";

    assertFalse(cs.apply(""));

    assertTrue(cs.apply(sentence1));
    assertTrue(cs.apply(sentence2));
    assertFalse(cs.apply(sentence3));

    cs = cs.withSubstrings(strings, false);
    assertTrue(cs.apply(sentence1));
    assertTrue(cs.apply(sentence2));
    assertTrue(cs.apply(sentence3));

    cs = cs.withWholePhrasesOnly(true);
    assertTrue(cs.apply(sentence1));
    assertFalse(cs.apply(sentence2));
    assertTrue(cs.apply(sentence3));
  }
}
