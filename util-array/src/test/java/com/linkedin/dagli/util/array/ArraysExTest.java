package com.linkedin.dagli.util.array;

import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class ArraysExTest {
  @Test
  public void testDeepToString() {
    Assertions.assertEquals("6", ArraysEx.deepToString(6));
    Assertions.assertEquals("[6]", ArraysEx.deepToString(new int[] {6}));
    Assertions.assertEquals("[[6]]", ArraysEx.deepToString(new int[][] {{6}}));
    Assertions.assertEquals("null", ArraysEx.deepToString(null));
  }

  @Test
  public void testDeduplicateSortedArray() {
    final long[] initial = {1, 2, 2, 2, 3, 3, 4};
    final long[] expected = {1, 2, 3, 4, 3, 3, 4};

    Assertions.assertEquals(4, ArraysEx.deduplicateSortedArray(initial));
    Assertions.assertArrayEquals(expected, initial);
  }

  @Test
  public void castTest() {
    float[] floats = ArraysEx.toFloatsLossy(new double[] { 1.5, 2.0, 2.5 });
    Assertions.assertArrayEquals(floats, new float[] { 1.5f, 2.0f, 2.5f });

    long[] ints = ArraysEx.toLongs(new int[] {1, 2, 3});
    Assertions.assertArrayEquals(ints, new long[] {1, 2, 3});

    Assertions.assertTrue(ArraysEx.toFloatsLossy(new double[0]).length == 0);
    Assertions.assertTrue(ArraysEx.toLongs(new int[0]).length == 0);
  }

  @Test
  public void testConcat() {
    Assertions.assertArrayEquals(new Integer[]{1, 2, 3},
        ArraysEx.concat(new Integer[]{1}, new Integer[0], new Integer[]{2, 3}));
  }

  @Test
  public void testSort() {
    double[] sortKey = { 3, 1, 2};
    Object[] sortAux = { 3, 1, 2};
    ArraysEx.sort(sortKey, sortAux);

    Assertions.assertArrayEquals(sortKey, new double[] {1, 2, 3});
    Assertions.assertArrayEquals(sortAux, new Integer[] {1, 2, 3});
  }

  @Test
  public void testReverse() {
    double[] ordered = {1, 2, 3};
    ArraysEx.reverse(ordered);
    Assertions.assertArrayEquals(ordered, new double[] {3, 2, 1});

    double[] ordered2 = {1, 2};
    ArraysEx.reverse(ordered2);
    Assertions.assertArrayEquals(ordered2, new double[] {2, 1});
  }

  @Test
  public void testMonotonic() {
    double[] unordered = {1, 3, 2};
    double[] ordered = {1, 2, 3};

    Assertions.assertFalse(ArraysEx.isMonotonicallyDecreasing(unordered));
    Assertions.assertFalse(ArraysEx.isMonotonicallyIncreasing(unordered));

    Assertions.assertFalse(ArraysEx.isMonotonicallyDecreasing(ordered));
    Assertions.assertTrue(ArraysEx.isMonotonicallyIncreasing(ordered));

    ArraysEx.reverse(ordered);

    Assertions.assertTrue(ArraysEx.isMonotonicallyDecreasing(ordered));
    Assertions.assertFalse(ArraysEx.isMonotonicallyIncreasing(ordered));
  }

  @Test
  public void testLossyNumberToArray() {
    Assertions.assertArrayEquals(ArraysEx.toBytesLossy(Arrays.asList(0.0, 1.5, 300.0, Double.POSITIVE_INFINITY)), new byte[] {0, 1, (byte) 300, -1});
  }
}
