package com.linkedin.dagli.util.cryptography;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;


/**
 * The default cryptography provider for Dagli, backed by javax.crypto.
 *
 * Encryption is performed with AES 128-bit in CTR mode.  By default, the key used is randomly created for this JVM
 * instance and thus encrypted data can only be read back in the current session.  Note that multiple instantiations of
 * this provider in the same JVM will use the same session key.  However, you may also supply your own key, the chief
 * utility for this being the preservation of data across JVM sessions.
 *
 * Please also note that, strictly speaking, javax.crypto is not guaranteed to support AES 128-bit CTR on every JVM, nor
 * is the implementation guaranteed to have the required behavior to be secure (specifically, the incrementing counter
 * must be the full 128-bit integer nonce, not merely the last 32 bits, which might overflow and cause nonce values to
 * be repeated, which would defeat the encryption).  If the behavior is not secure or if the mode is not available, a
 * {@link NoSuchAlgorithmException} will be thrown.
 */
public final class DefaultCryptographyProvider implements CryptographyProvider {
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final byte[] SALT =
      new byte[]{-37, 125, 95, 118, 99, 91, 77, 59, -117, -50, -47, 50, -103, 96, 111, -2};
  /**
   * The "session key" for this JVM instance.  This key is randomly generated when the class is loaded and only valid
   * for the current session.  If key generation somehow fails (it shouldn't) this field will be null.
   */
  private static final SecretKey SESSION_KEY = createAES128Key();
  private static final String SYMMETRIC_CIPHER = "AES/CTR/NoPadding";
  private static final int KEY_BIT_SIZE = 128;
  private static final int BLOCK_SIZE_IN_BYTES = 16;
  private static final boolean IS_SYMMETRIC_CIPHER_SECURE = isSymmetricCipherSecure();

  private final SecretKey _key;

  /**
   * Creates a new instance that will use a random, per-session key.  All instances created with this constructor during
   * this JVM session will share this key.
   */
  public DefaultCryptographyProvider() {
    _key = SESSION_KEY;
  }

  /**
   * Creates a new instance that will use the specified key.  This is useful when encrypted data needs to live beyond
   * the current session.  It is incumbent upon the caller to use a strong password.
   *
   * @param key the key to use for encryption and decryption
   * @throws NoSuchAlgorithmException if the key hashing scheme is not available on this JVM
   */
  public DefaultCryptographyProvider(String key) throws NoSuchAlgorithmException {
    _key = createAES128Key(key);
  }

  private static SecretKey createAES128Key() {
    KeyGenerator keyGenerator;
    try {
      keyGenerator = KeyGenerator.getInstance("AES");
    } catch (NoSuchAlgorithmException e) {
      // 128-bit AES is guaranteed to be supported, so this should never happen on Java 7+
      return null;
    }

    keyGenerator.init(KEY_BIT_SIZE, SECURE_RANDOM);

    return keyGenerator.generateKey();
  }

  private static SecretKey createAES128Key(String password) throws NoSuchAlgorithmException {
    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    KeySpec spec = new PBEKeySpec(password.toCharArray(), SALT, 65536, 128);
    try {
      SecretKey sk = factory.generateSecret(spec);
      return new SecretKeySpec(sk.getEncoded(), "AES");
    } catch (InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Checks that the desired symmetric cipher algorithm is available and that the implementation has the behavior we
   * expect.
   *
   * @return whether the symmetric cipher is available and has the expected, secure behavior
   */
  private static boolean isSymmetricCipherSecure() {
    Cipher cipher;
    try {
      cipher = Cipher.getInstance(SYMMETRIC_CIPHER);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      return false;
    }

    byte[] zerosBlock = new byte[BLOCK_SIZE_IN_BYTES];
    byte[] onesBlock = new byte[BLOCK_SIZE_IN_BYTES];
    Arrays.fill(onesBlock, (byte) -1);

    try {
      cipher.init(Cipher.ENCRYPT_MODE, SESSION_KEY, new IvParameterSpec(onesBlock));

      for (byte b : cipher.getIV()) {
        if (b != -1) {
          // this should never, ever happen
          throw new IllegalStateException("IV not properly initialized!");
        }
      }

      // now we need to check that the counter-vector is overflowing to all-0s (from all-1s)
      // this verifies that the counter is 128-bit, rather than 32-bit, which would create an encryption hole should more
      // than 2^39 bits be encrypted and the counter overflowed
      cipher.update(zerosBlock); // gets encrypted with the all-1s counter value
      byte[] secondBlock = cipher.doFinal(zerosBlock); // encrypted with the all-0s value

      cipher.init(Cipher.ENCRYPT_MODE, SESSION_KEY, new IvParameterSpec(zerosBlock));
      byte[] secondBlockMatcher = cipher.doFinal(zerosBlock);

      // these should be the same; if not, the counter is not being treated as a 128-bit integer, which would limit the
      // number of blocks that can be safely encrypted
      if (!Arrays.equals(secondBlock, secondBlockMatcher)) {
        return false;
      }

      // make sure that encryption is actually happening
      if (Arrays.equals(secondBlock, zerosBlock)) {
        // should never happen
        return false;
      }
    } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
      return false;
    }

    return true;
  }

  /**
   * Creates a new {@link CipherOutputStream} that will use a secure symmetric algorithm to encrypt data to the
   * specified target stream.  This method may write to the underlying stream before returning and should be considered
   * a potentially blocking operation.
   *
   * The algorithm used is not guaranteed to be supported by the Java standard library implementation employed by your
   * JVM.  If it is not, a {@link NoSuchAlgorithmException} will be thrown.
   *
   * @param targetStream the stream to which encrypted data will be written
   * @return an output stream that can be used to write encrypted data to the target stream
   */
  @Override
  public CipherOutputStream getOutputStream(OutputStream targetStream) throws NoSuchAlgorithmException, IOException {
    if (!IS_SYMMETRIC_CIPHER_SECURE) {
      throw new NoSuchAlgorithmException();
    }

    try {
      byte[] ivVector = new byte[BLOCK_SIZE_IN_BYTES];
      SECURE_RANDOM.nextBytes(ivVector);

      Cipher cipher = Cipher.getInstance(SYMMETRIC_CIPHER);
      cipher.init(Cipher.ENCRYPT_MODE, _key, new IvParameterSpec(ivVector));

      targetStream.write(ivVector);
      return new CipherOutputStream(targetStream, cipher);
    } catch (NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
      // should never happen
      throw new NoSuchAlgorithmException(e);
    }
  }

  /**
   * Creates a new {@link CipherInputStream} that will use a secure symmetric algorithm to decrypt data from the
   * specified source stream.  This method may read from the underlying stream before returning and should be considered
   * a potentially blocking operation.
   *
   * The algorithm used is not guaranteed to be supported by the Java standard library implementation employed by your
   * JVM.  If it is not, a {@link NoSuchAlgorithmException} will be thrown.
   *
   * @param sourceStream the stream from which encrypted data will be read
   * @return an input stream that can be used to read encrypted data from the source stream
   */
  public CipherInputStream getInputStream(InputStream sourceStream) throws NoSuchAlgorithmException, IOException {
    if (!IS_SYMMETRIC_CIPHER_SECURE) {
      throw new NoSuchAlgorithmException();
    }

    try {
      byte[] ivVector = new byte[BLOCK_SIZE_IN_BYTES];
      int readSoFar = 0;
      int readBytes;
      while ((readBytes = sourceStream.read(ivVector, readSoFar, ivVector.length - readSoFar)) > 0) {
        readSoFar += readBytes;
      }
      if (readSoFar < ivVector.length) {
        throw new IOException("Unable to read initialization vector from the source stream");
      }

      Cipher cipher = Cipher.getInstance(SYMMETRIC_CIPHER);
      cipher.init(Cipher.DECRYPT_MODE, _key, new IvParameterSpec(ivVector));

      return new CipherInputStream(sourceStream, cipher);
    } catch (NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
      // should never happen
      throw new NoSuchAlgorithmException(e);
    }
  }
}
