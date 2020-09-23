package com.linkedin.dagli.text;

import com.linkedin.dagli.tester.Tester;
import org.junit.jupiter.api.Test;


public class ContainsEmailTest {
  @Test
  public void test() {
    ContainsEmailAddress containsEmail = new ContainsEmailAddress();
    Tester.of(containsEmail)
        .output(true)
        .input("My email is john_smith@asd.someserver.com")
        .output(false)
        .input("This is a non-email message @ a short-ish length")
        .test();
  }
}
