package com.linkedin.dagli.text.phone;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.linkedin.dagli.tester.Tester;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class ContainsPhoneNumberTest {
  @Test
  public void test() {
    ContainsPhoneNumber containsPhoneNumber = new ContainsPhoneNumber().withLeniency(PhoneNumberUtil.Leniency.POSSIBLE);

    Tester.of(containsPhoneNumber).output(true).input("My number is 442-253-2131, okay?").test();

    assertTrue(containsPhoneNumber.apply("My number is (442)253-2131, okay?"));
    assertTrue(containsPhoneNumber.apply("My number is 253-2131, okay?"));
    assertTrue(containsPhoneNumber.apply("My number is 2532131, okay?"));
    assertFalse(containsPhoneNumber.apply("I'm 234 years old"));
    assertFalse(containsPhoneNumber.apply("The cost is $500,000/year"));
    assertFalse(containsPhoneNumber.apply("The cost is $500000 per year"));
    assertFalse(containsPhoneNumber.apply("I have 50000 of them sitting in stock"));
  }
}
