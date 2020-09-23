package com.linkedin.dagli.util.environment;

import com.linkedin.dagli.util.cryptography.DefaultCryptographyProvider;


/**
 * This class is a way to get and set program-wide system properties that are peculiar to the machine, not a
 * particular DAG or transformer.  Generally you don't need to set these unless your machine has an unusual
 * configuration, e.g. a tiny /tmp directory such that you need to specify an alternate temp directory.
 *
 * Libraries intended for use in other code should never set (and never need to set) system properties.  Only
 * actual applications (i.e. things being invoked through their main(...) method) should modify these.
 *
 * Dagli system properties may be set programmatically or using "-D[property name]" command-line arguments to the
 * Java interpreter.  In the absence of either of these they fall back to the relevant Java system property and/or some
 * reasonable default.  See the javadoc of the individual properties for details.
 *
 * Property names available:
 * dagli.tmpdir: the temporary directory Dagli should use (defaults to java.io.tmpdir)
 * dagli.cryptoprovider: the class name of the {@link com.linkedin.dagli.util.cryptography.CryptographyProvider} Dagli
 *                       should use to implement {@link com.linkedin.dagli.util.cryptography.Cryptography}.
 */
public class DagliSystemProperties {
  private static final String TEMP_DIRECTORY_PROPERTY = "dagli.tmpdir";
  private static final String CRYPTO_PROVIDER_PROPERTY = "dagli.crypoprovider";

  private DagliSystemProperties() { }

  /**
   * Gets the full class name of the {@link com.linkedin.dagli.util.cryptography.CryptographyProvider} that Dagli should
   * use to provide cryptography services via the {@link com.linkedin.dagli.util.cryptography.Cryptography} class.
   *
   * Class names must include all namespaces, e.g. "com.linkedin.dagli.util.cryptography.DefaultCryptographyProvider".
   *
   * The default value is "com.linkedin.dagli.util.cryptography.DefaultCryptographyProvider".
   *
   * @return the name of the cryptography provider class that should be used
   */
  public static String getCryptoProviderName() {
    String result = System.getProperty(CRYPTO_PROVIDER_PROPERTY);
    if (result == null) {
      return DefaultCryptographyProvider.class.getName();
    } else {
      return result;
    }
  }

  /**
   * Gets the temporary directory that Dagli (and transformers) should use for storing temporary files and folders.
   * This may be set with the system property dagli.tmpdir; if no directory is set, this defaults to java.io.tmpdir
   * instead, which itself defaults to the machine's temp folder (e.g. /tmp).
   *
   * @return the temp directory that Dagli should use for storing temporary files and folders.
   */
  public static String getTempDirectory() {
    String result = System.getProperty(TEMP_DIRECTORY_PROPERTY);
    if (result == null) {
      return System.getProperty("java.io.tmpdir");
    } else {
      return result;
    }
  }

  /**
   * Sets the temporary directory that Dagli (and transformers) should use for storing temporary files and folders.
   * This sets the system property dagli.tmpdir; if no directory is set, the temp directory defaults to java.io.tmpdir
   * instead, which itself defaults to the machine's temp folder (e.g. /tmp).
   *
   * Normally you would only set the temp directory if your system's default temp directory were unsuitable, e.g. if
   * it were a small RAM-backed tmpfs filesystem.
   *
   * @return the previous value of dagli.tmpdir, or null if there was none explicitly set.
   */
  public static String setTempDirectory(String tempDirectory) {
    if (tempDirectory == null) {
      return System.clearProperty(TEMP_DIRECTORY_PROPERTY);
    } else {
      return System.setProperty(TEMP_DIRECTORY_PROPERTY, tempDirectory);
    }
  }
}
