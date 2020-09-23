package com.linkedin.dagli.math.hashing;

/**
 * Simple, computationally inexpensive hashing function that xorshift64*s an input value and then xorshift64*s the
 * result.  The result is unique for every input value (i.e. the hash is a one-to-one map.)
 *
 * The intuition behind this hashing function is that xorshifting the input twice helps mitigate the easily-discernible
 * correlations in the output values that are seen when passing related inputs (like incrementing indices) to
 * xorshift64* only once (as evaluated by the Dieharder test suite.)
 *
 * For reference, this was tested as a stateless RNG with four weak failures of the Dieharder tests at the -m5 level,
 * looped over the first 16GB of output (starting and index 0).  Please note that small differences in the number of
 * failed tests is not a good indicator of quality: it will vary based on seeding, starting index, and amount of data.
 */
public class DoubleXorShift implements StatelessRNG {
  private static final long serialVersionUID = 1;
  // these are arbitrary random values (i.e. they are not deliberately chosen or otherwise magical):
  private static final long SEED_SALT = 0x8095206b6caeb4cbL; // mitigates danger of non-random seeds
  private static final long DEFAULT_SEED = 0x1459977fd4d0ad7eL;
  private static final long INPUT_SALT = 0x9007a03320d52142L; // better mixing of inputs that are often mostly 0s

  private final long _seed;

  /**
   * Creates a new instance with the default seed.
   */
  public DoubleXorShift() {
    this(DEFAULT_SEED);
  }

  /**
   * Creates a new instance with the specified seed.
   *
   * @param seed the seed to be used; a seed value; a (pseudo)random value is recommended rather than 0, 1, 2, etc.
   */
  public DoubleXorShift(long seed) {
    _seed = seed;
  }

  @Override
  public long hash(long index) {
    return hash(index, _seed);
  }

  /**
   * xorshift64*s an input value and then xorshift64*s the result.
   *
   * The result is unique for every input (given a constant seed); this function is a one-to-one map.
   *
   * @param index the value to be shared (equivalently, the index of the pseudorandom value to be retrieved)
   * @param seed a seed value; a (pseudo)random value is recommended rather than 0, 1, 2, etc.
   * @return a pseudorandom value
   */
  public static long hash(long index, long seed) {
    return xorshift64s(xorshift64s(index + INPUT_SALT) + seed + SEED_SALT);
  }

  /**
   * xorshift64*s an input value and then xorshift64*s the result using a default seed value.
   *
   * @param index the value to be shared (equivalently, the index of the pseudorandom value to be retrieved)
   * @return a pseudorandom value
   */
  public static long hashWithDefaultSeed(long index) {
    return hash(index, DEFAULT_SEED);
  }

  /**
   * Calculates a xorshift64* "hash" (the input x is taken as the "hidden state" of a xorshift64* generator; note that
   * xorship64* values from an incrementing sequence of hidden states will be easily discernible as non-random.  It is,
   * however, a useful building block.
   *
   * The result is unique for every input; this function is a one-to-one map.
   *
   * @param x the "hidden state" used to generate a xorshift64* value
   * @return the generated value
   */
  static long xorshift64s(long x) {
    x ^= x >>> 12;
    x ^= x << 25;
    x ^= x >>> 27;
    return x * 0x2545F4914F6CDD1DL; // multiplying by an odd number will always yield a unique result mod 2^64
  }

  @Override
  public StatelessRNG withSeed(long seed) {
    return new DoubleXorShift(seed);
  }
}
