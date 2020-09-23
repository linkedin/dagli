package com.linkedin.dagli.math.distribution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LabelProbabilityTest {
  private LabelProbability<String> lp1 = new LabelProbability<>(new String("LABEL"), 0.5);
  private LabelProbability<String> lp2 = new LabelProbability<>(new String("LABEL"), 0.5);
  private LabelProbability<String> lp3 = new LabelProbability<>(new String("LABEL"), 0.6);
  private LabelProbability<Integer> lp4 = new LabelProbability<>(1, 0.6);
  private LabelProbability<String> lp5 = new LabelProbability<>(new String("OTHER LABEL"), 0.6);
  private LabelProbability<String> lp6 = new LabelProbability<>(null, 0.6);
  private LabelProbability<String> lp7 = new LabelProbability<>(null, 0.6);
  private LabelProbability<String> lp8 = new LabelProbability<>(null, 0.7);

  @Test
  public void testEqualsAndHashCode() {
    check(lp1, lp2, lp3);

    assertNotEquals(lp3, lp4);
    assertNotEquals(lp3.hashCode(), lp4.hashCode());

    assertNotEquals(lp1, lp6);
    assertNotEquals(lp1.hashCode(), lp6.hashCode());

    check(lp6, lp7, lp8);
  }

  // tests that equals1.equals(equals2) and that their hashes and strings are the same, while also checking that they
  // are different than the "unequal" parameter on these dimensions.
  private static <T> void check(LabelProbability<T> equals1, LabelProbability<T> equals2, LabelProbability<T> unequal) {
    assertEquals(equals1, equals2);
    assertEquals(equals1.hashCode(), equals2.hashCode());
    assertEquals(equals1.toString(), equals2.toString());

    assertNotEquals(equals1, unequal);
    assertNotEquals(equals1.hashCode(), unequal.hashCode());
    assertNotEquals(equals1.toString(), unequal.toString());
  }

  @Test
  public void testComparability() {
    assertEquals(LabelProbability.PROBABILITY_ORDER.compare(lp1, lp2), 0);
    assertTrue(LabelProbability.PROBABILITY_ORDER.compare(lp1, lp3) < 0);
    assertTrue(LabelProbability.PROBABILITY_ORDER.compare(lp3, lp1) > 0);

    assertEquals(LabelProbability.PROBABILITY_ORDER.compare(lp3, lp5), 0); // same probability

    assertTrue(LabelProbability.PROBABILITY_ORDER.compare(lp1, lp6) < 0);
    assertEquals(LabelProbability.PROBABILITY_ORDER.compare(lp3, lp6), 0);
  }
}
