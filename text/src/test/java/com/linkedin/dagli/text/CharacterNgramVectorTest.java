package com.linkedin.dagli.text;

import com.linkedin.dagli.math.hashing.Murmurish;
import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.math.vector.SparseFloatMapVector;
import com.linkedin.dagli.tester.Tester;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class CharacterNgramVectorTest {
  @Test
  public void test() {
    for (CharacterNgramVector.Padding padding : CharacterNgramVector.Padding.values()) {
      Tester.of(new CharacterNgramVector().withMinSize(1).withMaxSize(3).withPadding(padding))
          .input("pizza")
          .test();
    }


    final String pizza = "pizza";
    final CharacterNgramVector ngramVectorizer =
        new CharacterNgramVector().withMinSize(1).withMaxSize(3).withPadding(CharacterNgramVector.Padding.NONE);
    assertEquals(getNgrams(pizza, 1, 3), ngramVectorizer.apply(pizza));

    assertEquals(DenseFloatArrayVector.wrap(), ngramVectorizer.apply(""));
    assertEquals(DenseFloatArrayVector.wrap(), ngramVectorizer.withMinSize(10).withMaxSize(10).apply(pizza));

    final String paddedPizza = "_pizza_";
    final CharacterNgramVector paddedNgramVectorizer =
        new CharacterNgramVector().withMinSize(2).withMaxSize(3).withPadding(CharacterNgramVector.Padding.SINGLE_CHARACTER);
    assertEquals(getNgrams(paddedPizza, 2, 3), paddedNgramVectorizer.apply(pizza));

    final String fullyPaddedPizza = "__pizza__";
    final CharacterNgramVector fullyPaddedNgramVectorizer =
        new CharacterNgramVector().withMinSize(3).withMaxSize(3).withPadding(CharacterNgramVector.Padding.FULL);
    assertEquals(getNgrams(fullyPaddedPizza, 3, 3), fullyPaddedNgramVectorizer.apply(pizza));
  }

  private static SparseFloatMapVector getNgrams(String string, int minSize, int maxSize) {
    SparseFloatMapVector result = new SparseFloatMapVector();

    for (int i = 0; i < string.length(); i++) {
      for (int j = minSize; j <= maxSize && i + j <= string.length(); j++) {
        result.increase(Murmurish.hash(string, i, j, 0), 1);
      }
    }

    return result;
  }
}
