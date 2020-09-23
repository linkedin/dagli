package com.linkedin.dagli.math.hashing;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Arrays;
import java.util.List;
import java.util.function.LongUnaryOperator;
import java.util.function.ToLongFunction;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;


public class HashingTest {

  private static final int LONGS_TO_TEST = 10000000;
  private static final int STRINGS_TO_TEST = 10000000;

  private static Stream<Arguments> longHashFunctionArgs() {
    List<LongUnaryOperator> hashers =
        Arrays.asList(new InsecureAESHasher()::hash, new DoubleXorShift()::hash, val -> Murmurish.hash(val, 0),
            val -> Murmurish.hash(val, 1337), MurmurHash3::fmix64);
    List<LongUnaryOperator> valueGenerators = Arrays.asList(
        i -> i * (Long.MAX_VALUE / LONGS_TO_TEST),
        i -> i << 8,
        i -> i << 32,
        LongUnaryOperator.identity()
    );

    return hashers.stream().flatMap(hasher -> valueGenerators.stream().map(vg -> Arguments.of(hasher, vg)));
  }

  /**
   * Primitive test for collisions and bit parity of hash values from a given hasher.  Bad hash functions can still pass
   * this test, but it's a useful sanity check.
   *
   * @param hasher the hash function to test
   */
  @ParameterizedTest
  @MethodSource("longHashFunctionArgs")
  public void testLongHashFunction(LongUnaryOperator hasher, LongUnaryOperator valueToHashFromIndex) {
    long[][] bitCounts = new long[64][2];
    long largerCount = 0;
    long smallerCount = 0;
    LongOpenHashSet seen = new LongOpenHashSet(LONGS_TO_TEST);
    long lastHash = 0;
    for (long i = 0; i < LONGS_TO_TEST; i++) {
      long hash = hasher.applyAsLong(valueToHashFromIndex.applyAsLong(i));
      if (hash > lastHash) {
        largerCount++;
      } else {
        smallerCount++;
      }
      lastHash = hash;
      seen.add(hash);
      for (int b = 0; b < 64; b++) {
        bitCounts[b][(hash & (1L << b)) != 0 ? 1 : 0]++;
      }
    }

    final double allowableErrorMultiplier = 1.005;

    Assertions.assertTrue(largerCount < smallerCount * allowableErrorMultiplier);
    Assertions.assertTrue(smallerCount < largerCount * allowableErrorMultiplier);

    for (int b = 0; b < 64; b++) {
      Assertions.assertTrue(bitCounts[b][0] < bitCounts[b][1] * allowableErrorMultiplier);
      Assertions.assertTrue(bitCounts[b][1] < bitCounts[b][0] * allowableErrorMultiplier);
    }

    Assertions.assertTrue((LONGS_TO_TEST - seen.size()) <= 1);
  }

  @Test
  public void testStringHashFunctions() {
    testStringWithAccentsHashFunction(str -> Murmurish.hash(str, 0));
    testStringHashFunction(str -> Murmurish.hash(str, 0));
    testStringWithDifferentCasingHashFunction(str -> Murmurish.hash(str, 0));
  }

  private void testStringHashFunction(ToLongFunction<String> hasher) {
    long[][] bitCounts = new long[64][2];
    LongOpenHashSet seen = new LongOpenHashSet(STRINGS_TO_TEST);
    for (long i = 0; i < STRINGS_TO_TEST; i++) {
      long hash = hasher.applyAsLong(Long.toString(i * (Long.MAX_VALUE / STRINGS_TO_TEST)));
      seen.add(hash);
      for (int b = 0; b < 64; b++) {
        bitCounts[b][(hash & (1L << b)) != 0 ? 1 : 0]++;
      }
    }

    for (int b = 0; b < 64; b++) {
      Assertions.assertTrue(bitCounts[b][0] < bitCounts[b][1] * 1.05);
      Assertions.assertTrue(bitCounts[b][1] < bitCounts[b][0] * 1.05);
    }

    Assertions.assertTrue((STRINGS_TO_TEST - seen.size()) <= 1);
  }

  private void testStringWithAccentsHashFunction(ToLongFunction<String> hasher) {
    Assertions.assertNotEquals(hasher.applyAsLong("helló"), hasher.applyAsLong("hello"));
  }

  private void testStringWithDifferentCasingHashFunction(ToLongFunction<String> hasher) {
    Assertions.assertNotEquals(hasher.applyAsLong("Hello"), hasher.applyAsLong("hello"));
  }

  @Test
  public void testMurmurHash3Short() {
    final byte[] shortBytes = new byte[] { 1, 2, 3, 4};

    long[] longPair = new long[2];
    MurmurHash3.murmurhash3_x64_128(shortBytes, 0, shortBytes.length, 0, longPair);

    Assertions.assertEquals(720734999560851427L, longPair[0]);
    Assertions.assertEquals(1043635621, MurmurHash3.murmurhash3_x86_32(shortBytes, 0, shortBytes.length, 0));
    Assertions.assertEquals(-785539136, MurmurHash3.murmurhash3_x86_32("oof", 0, 3, 0));
  }

  @Test
  public void testMurmurHash3Long() {
    final byte[] longBytes = new byte[100];
    longBytes[99] = 99;

    long[] longPair = new long[2];
    MurmurHash3.murmurhash3_x64_128(longBytes, 0, longBytes.length, 0, longPair);

    Assertions.assertEquals(8656573494302028032L, longPair[0]);
    Assertions.assertEquals(1880509316, MurmurHash3.murmurhash3_x86_32(longBytes, 0, longBytes.length, 0));
    Assertions.assertEquals(-1039762711, MurmurHash3.murmurhash3_x86_32(new String(longBytes), 0, 40, 0));
  }

  @Test
  public void testMurmurHash3Variable() {
    // now we just check that MH3 doesn't throw for various sizes of data
    final byte[] longBytes = new byte[100];
    final long[] longPair = new long[2];
    final String string = new String(longBytes);

    for (int i = 0; i < 100; i++) {
      MurmurHash3.murmurhash3_x64_128(longBytes, 0, i, 0, longPair);
      MurmurHash3.murmurhash3_x86_32(longBytes, 0, i, 0);
      MurmurHash3.murmurhash3_x86_32(string, 0, i, 0);
    }
  }
}
