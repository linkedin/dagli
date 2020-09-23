package com.linkedin.dagli.math.hashing;

import java.io.Serializable;


/**
 * A "stateless" random number generator accepts an 64-bit index (which may be negative) to a random value in the
 * sequence and returns the corresponding random value in amortized constant time.  Equivalently, stateless RNGs
 * are integer hashing functions that, given sequential (or otherwise similar) inputs, return hash values with no overly
 * obvious correlations.
 *
 * All implementations of this interface are immutable and thread-safe.
 *
 * Other, stateful random number generators, like Java's {@link java.util.Random}, maintain an internal state that is
 * used to determine the next pseudorandom value.  However, this both makes the RNG mutable and requires the random
 * sequence of values to be retrieved sequentially (i.e. the n'th random value cannot generally be retrieved without
 * first retrieving all n-1 preceding values).
 *
 * The stateless random number generators defined in this package are tested against the Dieharder tests (see
 * https://en.wikipedia.org/wiki/Diehard_tests) with seeds of 0 and values generated with incrementing indices starting
 * at 0.  While passing these tests does not prove that an RNG is "good", they at least show that it is not obviously
 * bad.  For reference, Dieharder's AES implementation (with -m5 set, increasing the data used per test by 5X) has weak
 * failures for four tests.
 *
 * To achieve distinct sequences of random values, the seeds used when creating a stateless random number generator
 * should be random.  Using similar seeds with a high number of shared bits (such as seed values 0, 1, and 2) may yield
 * very obviously correlated sequences in some cases.
 *
 * The primarily disadvantage of stateless RNGs are that the cost to generate each value may be higher than other,
 * stateful RNGs of equivalent "randomness", although this is not a practical concern unless the number of numbers being
 * generated is very large.
 */
public interface StatelessRNG extends Serializable {
  /**
   * Hashes an integer, or, equivalently, returns the pseudorandom value corresponding to a given "index".  This
   * operation must be amortized constant time.
   *
   * @param index the index of the pseudorandom value to be retrieved or, equivalently, the integer to be hashed.
   * @return a pseudorandom value
   */
  long hash(long index);

  /**
   * Hashes an integer to a uniformly distributed [0, 1) pseudorandom value.  Note that the default implementation of
   * this method uses only the lower 52 bits of the hash.
   *
   * @param index the index of the pseudorandom value to be retrieved or, equivalently, the integer to be hashed.
   * @return a pseudorandom [0, 1) double value
   */
  default double hashToDouble(long index) {
    final long doubleExponent = 1023L << 52;

    // Create a double value of 1.[our hash bits as boolean fraction bits] and subtract 1 to get a [0, 1) value:
    return Double.longBitsToDouble(doubleExponent | (hash(index) >>> 12)) - 1;
  }

  /**
   * Returns a copy of this instance that will use the specified seed.  For implementations that use more than one
   * seed value, all seed values of the returned instance will be derived from the seed provided to this method.
   *
   * @param seed the seed value to use
   * @return a copy of this instance that will use the specified seed value
   */
  StatelessRNG withSeed(long seed);
}
