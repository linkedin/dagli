package com.linkedin.dagli.text;

import com.linkedin.dagli.tester.Tester;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class RegexTest {
  @Test
  public void test() {
    Pattern pattern = Pattern.compile("ca+t");

    MatchesRegex matches = new MatchesRegex().withPattern(pattern);
    Tester.of(matches).output(true).input("caaaat").test();
    assertEquals(matches.apply("dooog"), false);

    ReplacedRegex replaced = new ReplacedRegex().withPattern(pattern);
    Tester.of(replaced).output("dogfish").input("catfish", "dog").test();
    assertEquals(replaced.apply("blah", "asdf"), "blah");
  }
}
