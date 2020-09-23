package com.linkedin.dagli.util.cryptography;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;


/**
 * A provider of cryptographic methods for Dagli.  See {@link DefaultCryptographyProvider} for an example
 * implementation.
 *
 * Please note that these methods may use encryption algorithms that are not available in your implementation of Java,
 * and client code must handle the potential {@link NoSuchAlgorithmException}.
 */
public interface CryptographyProvider {
  /**
   * Creates a new {@link OutputStream} that will encrypt data written to the specified target stream.  This
   * method may write to the underlying stream before returning and should be considered a potentially blocking
   * operation.
   *
   * @param targetStream the stream to which encrypted data will be written
   * @return an output stream that can be used to write encrypted data to the target stream
   * @throws NoSuchAlgorithmException if a required encryption algorithm is not supported or available
   * @throws IOException if something goes wrong writing data to the stream
   */
  OutputStream getOutputStream(OutputStream targetStream) throws NoSuchAlgorithmException, IOException;

  /**
   * Creates a new {@link InputStream} that will decrypt data from the specified source stream.  This method may read
   * from the underlying stream before returning and should be considered a potentially blocking operation.
   *
   * @param sourceStream the stream from which encrypted data will be read
   * @return an input stream that can be used to read encrypted data from the source stream
   * @throws NoSuchAlgorithmException if a required encryption algorithm is not supported or available
   * @throws IOException if something goes wrong reading data from the stream
   */
  InputStream getInputStream(InputStream sourceStream) throws NoSuchAlgorithmException, IOException;
}
