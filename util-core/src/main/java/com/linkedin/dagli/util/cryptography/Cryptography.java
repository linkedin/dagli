package com.linkedin.dagli.util.cryptography;

import com.linkedin.dagli.util.environment.DagliSystemProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Dagli cryptography methods, provided by a {@link CryptographyProvider}.  By default,
 * {@link DefaultCryptographyProvider} is used.  You can change the cryptography provider in two ways:
 * (1) Via the dagli.cryptoprovider environmental variable set via the command line, e.g. "-Ddagli.cryptoprovider=...".
 * (2) By calling setCrypographyProvider(...)
 *
 * The cryptography provider can only be changed <b>once</b>, before any other Cryptography methods are called.  If a
 * non-default cryptography provider is specified by the dagli.cryptoprovider system property, you will not be able to
 * change it programmatically and an exception will be thrown.
 *
 * Please note that these methods may use encryption algorithms that are not available in your implementation of Java,
 * and client code must handle the potential {@link NoSuchAlgorithmException}.
 */
public class Cryptography {
  private Cryptography() {
  }

  // initially null if the default provider is to be used, or the custom provider from system properties
  private static final AtomicReference<CryptographyProvider> PROVIDER =
      new AtomicReference<>(getProviderOrNullFromSystemProperties());

  private static CryptographyProvider getProvider() {
    return PROVIDER.updateAndGet(existing -> existing == null ? new DefaultCryptographyProvider() : existing);
  }

  // gets the custom provider determined by system properties, or null if the default should be used
  private static CryptographyProvider getProviderOrNullFromSystemProperties() {
    String providerName = DagliSystemProperties.getCryptoProviderName();

    if (providerName.equalsIgnoreCase(DefaultCryptographyProvider.class.getName())) {
      return null;
    }

    try {
      return (CryptographyProvider) Class.forName(providerName).getConstructor().newInstance();
    } catch (Exception e) {
      return new CryptographyProvider() {
        @Override
        public OutputStream getOutputStream(OutputStream targetStream) throws NoSuchAlgorithmException, IOException {
          throw new NoSuchAlgorithmException("The provider " + providerName + " could not be loaded", e);
        }

        @Override
        public InputStream getInputStream(InputStream sourceStream) throws NoSuchAlgorithmException, IOException {
          throw new NoSuchAlgorithmException("The provider " + providerName + " could not be loaded", e);
        }
      };
    }
  }

  /**
   * Sets the cryptography provider to be used.  A custom cryptography provider must only be set ONCE in a given JVM
   * instance, either via this method or the dagli.cryptoprovider system property.
   *
   * Also note that this method should be invoked before invoking any other methods on this class.  Once any other
   * methods have been called, attempting to use this method will, again, throw an IllegalAccessException.
   *
   * @param provider the new provider to use
   * @throws IllegalAccessException if a custom cryptography provider has already been set
   */
  public static void setCryptographyProvider(CryptographyProvider provider) throws IllegalAccessException {
    CryptographyProvider updatedProvider = PROVIDER.updateAndGet(existing -> existing == null ? provider : existing);

    if (updatedProvider != provider) {
      throw new IllegalAccessException(
          "Attempting to set a new cryptography provider when a provider has already been set via a previous call to "
              + "setCryptographyProvider(...) or via the dagli.cryptoprovider system property, or when other methods "
              + "on the Cryptography class have already been used.");
    }
  }

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
  public static OutputStream getOutputStream(OutputStream targetStream) throws NoSuchAlgorithmException, IOException {
    return getProvider().getOutputStream(targetStream);
  }

  /**
   * Creates a new {@link InputStream} that will decrypt data from the specified source stream.  This method may read
   * from the underlying stream before returning and should be considered a potentially blocking operation.
   *
   * @param sourceStream the stream from which encrypted data will be read
   * @return an input stream that can be used to read encrypted data from the source stream
   * @throws NoSuchAlgorithmException if a required encryption algorithm is not supported or available
   * @throws IOException if something goes wrong reading data from the stream
   */
  public static InputStream getInputStream(InputStream sourceStream) throws NoSuchAlgorithmException, IOException {
    return getProvider().getInputStream(sourceStream);
  }
}
