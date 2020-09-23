package com.linkedin.dagli.text;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class SubstringCountTest {
  @Test
  public void test() {
    List<String> substrings = Arrays.asList("dog", "dogg", "Oggy");
    String input = "hello doggY";

    SubstringCount sc = new SubstringCount().withSubstrings(substrings);
    assertEquals((int) sc.apply(input), 1); // "dogg"

    sc = sc.withOverlappingSubstringsCounted(true);
    assertEquals((int) sc.apply(input), 2); // "dogg" and "dog"

    sc = sc.withSubstrings(substrings, false);
    assertEquals((int) sc.apply(input), 3); // "dogg", "dog", and "Oggy"
  }
}
