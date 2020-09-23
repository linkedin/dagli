package com.linkedin.dagli.transformer;

import com.linkedin.dagli.list.NgramVector;
import com.linkedin.dagli.tester.Tester;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class NGramVectorTest {
  private static List<String> list = Arrays.asList("Mary", "had", "a", "little", "lamb");

  @Test
  public void test() {
    // default is unigrams; there are 5 of them
    Tester.of(new NgramVector())
        .input(list)
        .outputTest(vec -> vec.size64() == 5)
        .test();

    // try trigrams with default padding (1 object start/end); should be five trigrams
    Tester.of(new NgramVector().withMinSize(3).withMaxSize(3))
        .input(list)
        .outputTest(vec -> vec.size64() == 5)
        .test();

    // try unigrams, bigrams, and trigrams with default padding (1 object start/end)
    Tester.of(new NgramVector().withMaxSize(3))
        .input(list)
        .outputTest(vec -> vec.size64() == 5 + 6 + 5)
        .test();

    // try unigrams, bigrams, and trigrams with no padding
    Tester.of(new NgramVector().withPadding(NgramVector.Padding.NONE).withMaxSize(3))
        .input(list)
        .outputTest(vec -> vec.size64() == 5 + 4 + 3)
        .test();

    // try bigrams and trigrams with no padding
    Tester.of(new NgramVector().withPadding(NgramVector.Padding.NONE).withMinSize(2).withMaxSize(3))
        .input(list)
        .outputTest(vec -> vec.size64() == 4 + 3)
        .test();
  }

  @Test
  public void testFullPadding() {
    // default is unigrams
    assertEquals(5, new NgramVector().withPadding(NgramVector.Padding.FULL).apply(list).size64());

    // try trigrams with default padding
    assertEquals(7, new NgramVector().withPadding(NgramVector.Padding.FULL)
        .withMinSize(3)
        .withMaxSize(3)
        .apply(list)
        .size64());

    // try unigrams, bigrams, and trigrams
    assertEquals(5 + 6 + 7,
        new NgramVector().withPadding(NgramVector.Padding.FULL).withMaxSize(3).apply(list).size64());

    // try bigrams and trigrams
    assertEquals(6 + 7, new NgramVector().withPadding(NgramVector.Padding.FULL)
        .withMinSize(2)
        .withMaxSize(3)
        .apply(list)
        .size64());
  }
}
