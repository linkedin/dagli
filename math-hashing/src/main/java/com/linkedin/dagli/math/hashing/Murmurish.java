package com.linkedin.dagli.math.hashing;

/**
 * Hash methods that are derived from MurmurHash but are faster and do not produce the same hash for a given input.
 * Do not use these hashes if you need consistency with MurmurHash hashes calculated elsewhere.
 */
public abstract class Murmurish {
  private Murmurish() { }

  /**
   * Gets a long from a 4-character block in a CharSequence in little endian byte order.
   *
   * @param buf the strings from which the block will be taken
   * @param offset the offset at which the block will start
   * @long the block itself
   */
  private static long getLongLittleEndian(CharSequence buf, int offset) {
    return ((long) buf.charAt(offset + 3) << 48) | ((long) buf.charAt(offset + 2) << 32) | (
        (long) buf.charAt(offset + 1) << 16) | ((long) buf.charAt(offset));
  }

  /**
   * This is an adaption of MurmurHash3 128 bit that obtains a 64-bit hash of a provided CharSequence.
   * The hash values are not compatible with other MurmurHash implementations, which should be be considered if,
   * e.g. you need cross-language compatibility.
   *
   * @param seq the string to hash
   * @param seed a seed value to initialize the hash
   * @return a 64-bit hash value
   */
  public static long hash(CharSequence seq, long seed) {
    return hash(seq, 0, seq.length(), seed);
  }

  /**
   * This is an adaption of MurmurHash3 128 bit that obtains a 64-bit hash of a provided CharSequence.
   * The hash values are not compatible with other MurmurHash implementations, which should be be considered if,
   * e.g. you need cross-language compatibility.
   *
   * @param seq the string to hash
   * @param offset character offset where hashing begins
   * @param len number of characters to be hashed
   * @param seed a seed value to initialize the hash
   * @return a 64-bit hash value
   */
  public static long hash(CharSequence seq, int offset, int len, long seed) {
    long h1 = seed & 0x00000000FFFFFFFFL;
    long h2 = seed >>> 32;

    final long c1 = 0x87c37b91114253d5L;
    final long c2 = 0x4cf5ad432745937fL;

    int roundedEnd = offset + (len & 0xFFFFFFF8);  // round down to 8 char block
    for (int i = offset; i < roundedEnd; i += 8) {
      long k1 = getLongLittleEndian(seq, i);
      long k2 = getLongLittleEndian(seq, i + 4);
      k1 *= c1;
      k1 = Long.rotateLeft(k1, 31);
      k1 *= c2;
      h1 ^= k1;
      h1 = Long.rotateLeft(h1, 27);
      h1 += h2;
      h1 = h1 * 5 + 0x52dce729;
      k2 *= c2;
      k2 = Long.rotateLeft(k2, 33);
      k2 *= c1;
      h2 ^= k2;
      h2 = Long.rotateLeft(h2, 31);
      h2 += h1;
      h2 = h2 * 5 + 0x38495ab5;
    }

    long k1 = 0;
    long k2 = 0;

    // CHECKSTYLE:OFF
    switch (len & 7) {
      case 7:
        k2 = ((long) seq.charAt(roundedEnd + 6)) << 32;
      case 6:
        k2 |= ((long) seq.charAt(roundedEnd + 5)) << 16;
      case 5:
        k2 |= ((long) seq.charAt(roundedEnd + 4));
        k2 *= c2;
        k2 = Long.rotateLeft(k2, 33);
        k2 *= c1;
        h2 ^= k2;
      case 4:
        k1 |= ((long) seq.charAt(roundedEnd + 3)) << 48;
      case 3:
        k1 |= ((long) seq.charAt(roundedEnd + 2)) << 32;
      case 2:
        k1 |= ((long) seq.charAt(roundedEnd + 1)) << 16;
      case 1:
        k1 |= ((long) seq.charAt(roundedEnd));
        k1 *= c1;
        k1 = Long.rotateLeft(k1, 31);
        k1 *= c2;
        h1 ^= k1;
    }
    // CHECKSTYLE:ON

    //----------
    // finalization

    h1 ^= len;
    h2 ^= len;

    h1 += h2;
    h2 += h1;

    h1 = MurmurHash3.fmix64(h1);
    h2 = MurmurHash3.fmix64(h2);

    h1 += h2;
    h2 += h1;

    return h2;
  }

  /**
   * This is an adaption of MurmurHash3 128-bit that returns a 64-bit hash (half of the 128-bit hash) for a provided
   * long value.  The algorithm is very similar to "true" MurmurHash3 128-bit, with the primary difference being that
   * this implementation accepts a 64-bit seed.  Hash values will not match canonical MurmurHash3 implementations.
   *
   * Compare to {@link MurmurHash3#fmix64(long)}, which is a less thorough--but cheaper--hashing function for 64-bit
   * data.
   *
   * @param data the long value to hash
   * @param seed a seed value to initialize the hash
   * @return a 64-bit hash value
   */
  public static long hash(long data, long seed) {
    long h1 = seed & 0x00000000FFFFFFFFL;
    long h2 = seed >>> 32;

    final long c1 = 0x87c37b91114253d5L;
    final long c2 = 0x4cf5ad432745937fL;

    data *= c1;
    data = Long.rotateLeft(data, 31);
    data *= c2;
    h1 ^= data;

    h1 += h2;
    h2 += h1;

    h1 = MurmurHash3.fmix64(h1);
    h2 = MurmurHash3.fmix64(h2);

    h1 += h2;
    h2 += h1;

    return h2;
  }
}
