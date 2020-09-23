package com.linkedin.dagli.text;

import java.util.HashMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class ReplacedSubstringTest {
  @Test
  public void test() {
    HashMap<String, String> replacements = new HashMap<>();
    replacements.put("cat", "dog");
    replacements.put("wildcat", "bear");
    replacements.put("catty", "lion");
    replacements.put("ThAT", "Bat");

    String line = "That Cate is like a wildcat, but not Wildcatty";

    ReplacedSubstrings rs = new ReplacedSubstrings().withReplacements(replacements);

    assertEquals(rs.apply(line), "That Cate is like a bear, but not Wildlion");

    rs = rs.withReplacements(replacements, false);
    assertEquals(rs.apply(line), "Bat doge is like a bear, but not bearty");

    rs = rs.withWholePhrasesOnly(true);
    assertEquals(rs.apply(line), "Bat Cate is like a bear, but not Wildcatty");
  }
}
