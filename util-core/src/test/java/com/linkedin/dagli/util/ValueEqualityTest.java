package com.linkedin.dagli.util;

import com.linkedin.dagli.annotation.equality.DeepArrayValueEquality;
import com.linkedin.dagli.util.equality.ValueEqualityChecker;
import java.io.Serializable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class ValueEqualityTest {
  private static class A<U extends Serializable, S extends U, T extends CharSequence> {
    @DeepArrayValueEquality
    T[] _val1;

    @DeepArrayValueEquality
    S _val2;

    @DeepArrayValueEquality
    Object _val3;

    int[] _val4;

    public A(T[] val1, S val2, Object val3, int[] val4) {
      _val1 = val1;
      _val2 = val2;
      _val3 = val3;
      _val4 = val4;
    }
  }

  private static final ValueEqualityChecker<A> EQUALITY = new ValueEqualityChecker<>(A.class);

  private static void checkEquals(A val1, A val2) {
    Assertions.assertEquals(EQUALITY.hashCode(val1), EQUALITY.hashCode(val2));
    Assertions.assertTrue(EQUALITY.equals(val1, val2));
    Assertions.assertTrue(EQUALITY.equals(val2, val1));
  }

  private static void checkNotEquals(A val1, A val2) {
    Assertions.assertFalse(EQUALITY.equals(val1, val2));
    Assertions.assertFalse(EQUALITY.equals(val2, val1));

    // it is not absolutely certain to be a bug if this test fails; however, it is very likely to be since the false
    // positive rate should be ~1 in 4 billion
    Assertions.assertNotEquals(EQUALITY.hashCode(val1), EQUALITY.hashCode(val2));
  }

  @Test
  public void test() {
    int[] intArray1a = new int[] { 1 };
    int[] intArray1b = new int[] { 1 };

    int[][] deepIntArray1a = new int[][] {{1}, {2}};
    int[][] deepIntArray1b = new int[][] {{1}, {2}};
    int[][] deepIntArray2 = new int[][] {{2}, {1}};

    String[] stringArray1a = new String[] { "A" };
    String[] stringArray1b = new String[] { "A" };
    String[] stringArray2 = new String[] { "B" };

    checkEquals(new A<>(null, deepIntArray1a, null, null), new A<>(null, deepIntArray1b, null, null));
    checkNotEquals(new A<>(null, deepIntArray1a, null, null), new A<>(null, deepIntArray2, null, null));

    checkEquals(new A<>(stringArray1a, null, null, null), new A<>(stringArray1b, null, null, null));
    checkNotEquals(new A<>(stringArray1a, null, null, null), new A<>(stringArray2, null, null, null));

    checkEquals(new A<>(null, null, new String("A"), null), new A<>(null, null, new String("A"), null));
    checkNotEquals(new A<>(null, null, "A", null), new A<>(null, null, "B", null));

    checkNotEquals(new A<>(null, null, null, intArray1a), new A<>(null, null, intArray1b, null));
  }
}
