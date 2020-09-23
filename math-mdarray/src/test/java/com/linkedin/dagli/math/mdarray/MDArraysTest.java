package com.linkedin.dagli.math.mdarray;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Testing for the MDArrays class.
 */
public class MDArraysTest {
  @Test
  public void testConcatenate() {
    Assertions.assertEquals(0, MDArrays.concatenate(new long[0], new long[0]).length);
    Assertions.assertArrayEquals(new long[] { 1 }, MDArrays.concatenate(new long[] { 1 }, new long[0]));
    Assertions.assertArrayEquals(new long[] { 1 }, MDArrays.concatenate(new long[0], new long[] { 1 }));
    Assertions.assertArrayEquals(new long[] { 1, 2 }, MDArrays.concatenate(new long[] { 1 }, new long[] { 2 }));
  }

  @Test
  public void testElementCount() {
    Assertions.assertEquals(6, MDArrays.elementCount(new long[] { 1, 1, 2, 3}));
    Assertions.assertEquals(1, MDArrays.elementCount(new long[] { 1 }));
    Assertions.assertEquals(1, MDArrays.elementCount(new long[] { }));
  }

  @Test
  public void testOffsetToIndices() {
    Assertions.assertArrayEquals(new long[] { }, MDArrays.offsetToIndices(0, new long[] { }));
    Assertions.assertArrayEquals(new long[] { 0 }, MDArrays.offsetToIndices(0, new long[] { 1 }));
    Assertions.assertArrayEquals(new long[] { 0, 0 }, MDArrays.offsetToIndices(0, new long[] { 2, 3 }));
    Assertions.assertArrayEquals(new long[] { 1, 2 }, MDArrays.offsetToIndices(5, new long[] { 2, 3 }));
  }

  @Test
  public void testIndicesToOffset() {
    Assertions.assertEquals(0, MDArrays.indicesToOffset(new long[] { }, new long[] { }));
    Assertions.assertEquals(0, MDArrays.indicesToOffset(new long[] { 0 }, new long[] { 1 }));
    Assertions.assertEquals(0, MDArrays.indicesToOffset(new long[] { 0, 0 }, new long[] { 2, 3 }));
    Assertions.assertEquals(5, MDArrays.indicesToOffset(new long[] { 1, 2 }, new long[] { 2, 3 }));
  }
}
