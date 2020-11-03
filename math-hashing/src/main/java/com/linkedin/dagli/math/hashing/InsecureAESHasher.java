package com.linkedin.dagli.math.hashing;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;


/**
 * A hashing function based upon AES-128; it generates hashes by encrypting the to-be-hashed values.  This makes the
 * resulting hashes computationally expensive, but more reliable in quality (in the sense that any obvious patterns
 * comprise a weakness in a well-studied encryption algorithm).
 *
 * For reference, this was tested as a stateless RNG with five weak failures of the Dieharder tests at the -m5 level,
 * looped over the first 16GB of output (starting and index 0).  Please note that small differences in the number of
 * failed tests is not a good indicator of quality: it will vary based on seeding, starting index, and amount of data.
 *
 * This class <strong>should not be used as a cryptographic hash or random number generator.</strong>  It lacks many of
 * the safeguards of secure hash and RNG implementations.
 */
public class InsecureAESHasher implements StatelessRNG {
  private static final long serialVersionUID = 1;
  private static final String AES_MODE = "AES/ECB/NoPadding";

  private final Key _key;
  private final long _initialValue1;
  private final long _initialValue2;

  // Unfortunately, although AES-128/ECB can be readily implemented statelessly, Java does not guarantee that this will
  // be the case; therefore, we must keep one instance for every thread to allow for multithreading:
  private transient ThreadLocal<ThreadLocalState> _threadLocalStates = null;

  // stores thread-local state
  private class ThreadLocalState {
    final Cipher _cipher;
    final byte[] _buffer = new byte[16];

    public ThreadLocalState() {
      copyLongsToBytes(_initialValue1, _initialValue2, _buffer);
      try {
        _cipher = Cipher.getInstance(AES_MODE);
        _cipher.init(Cipher.ENCRYPT_MODE, _key);
      } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
        throw new RuntimeException(e); // should never happen: AES mode specified is guaranteed available by Java spec
      }
    }
  }

  private ThreadLocalState getThreadLocalState() {
    // The following is thread-safe despite the lack of explicit synchronization, with two possibilities:
    // (1) We detect that _threadLocalStates is null, and set its reference to a new value
    // (2) We detect that _threadLocalStates is null, set its reference, but another thread clobbers it with a different
    //     new ThreadLocal instance.
    // We are indifferent to either option: both result in _threadLocalStates being non-null and we don't really care
    // which instance _threadLocalStates points to.  Note that setting a reference is an atomic operation.
    if (_threadLocalStates == null) {
      _threadLocalStates = ThreadLocal.withInitial(ThreadLocalState::new);
    }
    // At this point, _threadLocalStates points to...some ThreadLocal instance.  The instance we get from our thread's
    // copy of the _threadLocalStates reference might be immediately garbage collected after this method ends because
    // it was clobbered by another thread and we have the old value, but it doesn't matter.
    return _threadLocalStates.get();
  }

  private static Key createKey(long firstBytes, long lastBytes) {
    byte[] data = new byte[16];
    copyLongsToBytes(firstBytes, lastBytes, data);
    return new SecretKeySpec(data, "AES");
  }

  private static void copyLongToBytes(long firstBytes, byte[] bytes) {
    bytes[0] = (byte) firstBytes;
    bytes[1] = (byte) (firstBytes >> 8);
    bytes[2] = (byte) (firstBytes >> 16);
    bytes[3] = (byte) (firstBytes >> 24);
    bytes[4] = (byte) (firstBytes >> 32);
    bytes[5] = (byte) (firstBytes >> 40);
    bytes[6] = (byte) (firstBytes >> 48);
    bytes[7] = (byte) (firstBytes >> 56);
  }

  private static void copyLongsToBytes(long firstBytes, long lastBytes, byte[] bytes) {
    bytes[0] = (byte) firstBytes;
    bytes[1] = (byte) (firstBytes >> 8);
    bytes[2] = (byte) (firstBytes >> 16);
    bytes[3] = (byte) (firstBytes >> 24);
    bytes[4] = (byte) (firstBytes >> 32);
    bytes[5] = (byte) (firstBytes >> 40);
    bytes[6] = (byte) (firstBytes >> 48);
    bytes[7] = (byte) (firstBytes >> 56);
    bytes[8] = (byte) lastBytes;
    bytes[9] = (byte) (lastBytes >> 8);
    bytes[10] = (byte) (lastBytes >> 16);
    bytes[11] = (byte) (lastBytes >> 24);
    bytes[12] = (byte) (lastBytes >> 32);
    bytes[13] = (byte) (lastBytes >> 40);
    bytes[14] = (byte) (lastBytes >> 48);
    bytes[15] = (byte) (lastBytes >> 56);
  }

  /**
   * Creates a new AES hasher with the default seed values.
   *
   * Although class should not be used as a cryptographic hashing function or random number generator in general, it is
   * <strong>absolutely and especially not</strong> suitable for cryptographic purposes if this constructor is used.
   */
  public InsecureAESHasher() {
    // the default values provided here are arbitrary and random
    this(0x3fb2c1a36fbdf7dbL, 0xaf339b1da650f8beL, 0xb814b228b628678eL, 0x74f3b9894f8209a9L);
  }

  /**
   * Creates a new AES hasher with a single seed values.  The other initial and values will be generated using this
   * seed.
   *
   * Although class should not be used as a cryptographic hashing function or random number generator in general, it is
   * <strong>absolutely and especially not</strong> suitable for cryptographic purposes if this constructor is used.
   *
   * @param seed the seed value
   */
  public InsecureAESHasher(long seed) {
    InsecureAESHasher hasher = new InsecureAESHasher(seed, seed + 1, seed + 2, seed + 3);
    long key1 = hasher.hash(1);
    long key2 = hasher.hash(key1);
    long iv1 = hasher.hash(key2);
    long iv2 = hasher.hash(iv1);

    _key = createKey(key1, key2);
    _initialValue1 = iv1;
    _initialValue2 = iv2;
  }

  /**
   * Creates a new AES hasher.  As AES-128 is a 128-bit block cipher with a 128-bit key, four 64-bit values are provided
   * to seed the hasher.  Although AES-128 is a reasonably strong symmetric encryption algorithm, this class should
   * nonetheless not be relied upon as a cryptographic hashing function.
   *
   * @param initialValue1 the lower 64 bits of the offset of the value that will be encrypted to generate hash values
   * @param initialValue2 the upper 64 bits of the offset of the value that will be encrypted to generate hash values
   * @param key1 the lower 64 bits of the encryption key
   * @param key2 the upper 64 bits of the encrypted key
   */
  public InsecureAESHasher(long initialValue1, long initialValue2, long key1, long key2) {
    _key = createKey(key1, key2);
    _initialValue1 = initialValue1;
    _initialValue2 = initialValue2;
  }

  private static long longFromFirstBytes(byte[] data) {
    return (data[0] & 0xffL)
        | (data[1] & 0xffL) << 8
        | (data[2] & 0xffL) << 16
        | (data[3] & 0xffL) << 24
        | (data[4] & 0xffL) << 32
        | (data[5] & 0xffL) << 40
        | (data[6] & 0xffL) << 48
        | (((long) data[7]) << 56);
  }

  private static long longFromLastBytes(byte[] data) {
    return (data[8] & 0xffL)
        | (data[9] & 0xffL) << 8
        | (data[10] & 0xffL) << 16
        | (data[11] & 0xffL) << 24
        | (data[12] & 0xffL) << 32
        | (data[13] & 0xffL) << 40
        | (data[14] & 0xffL) << 48
        | (((long) data[15]) << 56);
  }

  /**
   * Hashes a 64-bit long value.
   *
   * @param val the value to hash
   * @return the lower 8 bytes of the 16 byte block produced by encrypting the input value
   */
  @Override
  public long hash(long val) {
    return hash(val, 0);
  }

  /**
   * Hashes a 64-bit long value and places the resulting 128-bit hash in a provided array of longs.
   *
   * @param val the value to hash
   * @param output an array of longs of length at least 2 whose first two values will be set to the resulting hash
   */
  public void hash128(long val, long[] output) {
    ThreadLocalState state = getThreadLocalState();
    copyLongToBytes(val + _initialValue1, state._buffer);

    try {
      byte[] result = state._cipher.doFinal(state._buffer);
      output[0] = longFromFirstBytes(result);
      output[1] = longFromLastBytes(result);
    } catch (BadPaddingException | IllegalBlockSizeException e) {
      throw new RuntimeException(e); // should never happen
    }
  }

  /**
   * Hashes two 64-bit values.
   *
   * @param val1 the first value to hash
   * @param val2 the second value to hash
   * @return the lower 8 bytes of the 16 byte block produced by encrypting the input value
   */
  public long hash(long val1, long val2) {
    ThreadLocalState state = getThreadLocalState();
    copyLongsToBytes(val1 + _initialValue1, val2 + _initialValue2, state._buffer);

    try {
      return longFromFirstBytes(state._cipher.doFinal(state._buffer));
    } catch (BadPaddingException | IllegalBlockSizeException e) {
      throw new RuntimeException(e); // should never happen
    }
  }

  /**
   * Hashes two 64-bit values and places the resulting 128-bit hash in a provided array of longs.
   *
   * @param val1 the first value to hash
   * @param val2 the second value to hash
   * @param output an array of longs of length at least 2 whose first two values will be set to the resulting hash
   */
  public void hash128(long val1, long val2, long[] output) {
    ThreadLocalState state = getThreadLocalState();
    copyLongsToBytes(val1 + _initialValue1, val2 + _initialValue2, state._buffer);

    try {
      byte[] result = state._cipher.doFinal(state._buffer);
      output[0] = longFromFirstBytes(result);
      output[1] = longFromLastBytes(result);
    } catch (BadPaddingException | IllegalBlockSizeException e) {
      throw new RuntimeException(e); // should never happen
    }
  }

  @Override
  public InsecureAESHasher withSeed(long seed) {
    return new InsecureAESHasher(seed);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InsecureAESHasher that = (InsecureAESHasher) o;
    return _initialValue1 == that._initialValue1 && _initialValue2 == that._initialValue2 && Objects.equals(_key,
        that._key);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_key, _initialValue1, _initialValue2);
  }
}
