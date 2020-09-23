package com.linkedin.dagli.util;

import com.linkedin.dagli.util.cryptography.Cryptography;
import com.linkedin.dagli.util.cryptography.CryptographyProvider;
import com.linkedin.dagli.util.cryptography.DefaultCryptographyProvider;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


public class CryptographyTest {
  @Test
  public void streamTest() throws IOException, NoSuchAlgorithmException {
    streamTest(new DefaultCryptographyProvider());
    streamTest(new DefaultCryptographyProvider("1337pw your password should be better than this!"));
  }

  /**
   * Tests is a cryptography provider can successfully encrypt and decrypt a test message, and verifies that the
   * encrypted form does not exactly match the original message.  Note that this test provides no guarantee about the
   * level of security of a given cryptography provider.
   *
   * @param provider the provider to test
   * @throws IOException if the cryptographic input or output stream throws an exception
   * @throws NoSuchAlgorithmException if this error is thrown when trying to get the crypto input or output streams
   */
  public void streamTest(CryptographyProvider provider) throws IOException, NoSuchAlgorithmException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    OutputStream encryptionStream = provider.getOutputStream(baos);

    String message = "It was the best of times, it was the worst of times.  So it goes.";
    byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

    encryptionStream.write(messageBytes);
    encryptionStream.close();

    Assertions.assertFalse(Arrays.equals(messageBytes, baos.toByteArray()));

    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    InputStream decryptionStream = provider.getInputStream(bais);
    String readMessage = new BufferedReader(new InputStreamReader(decryptionStream, StandardCharsets.UTF_8)).readLine();

    Assertions.assertEquals(message, readMessage);
  }

  @Test
  public void compressedStreamTest() throws IOException, NoSuchAlgorithmException {
    byte[] data = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    java.nio.file.Path tempFile = Files.createTempFile("CryptographyTest", ".dat");
    tempFile.toFile().deleteOnExit();
    OutputStream os = Files.newOutputStream(tempFile);

    OutputStream encryptionStream = new GZIPOutputStream(Cryptography.getOutputStream(os));

    for (int i = 0; i < 100000; i++) {
      encryptionStream.write(data);
    }
    encryptionStream.close();

    InputStream is = Files.newInputStream(tempFile);
    InputStream decryptionStream = new GZIPInputStream(Cryptography.getInputStream(is));
    for (int i = 0; i < 100000; i++) {
      Assertions.assertEquals(i % 10, decryptionStream.read());
    }
    decryptionStream.close();

    Files.delete(tempFile);
  }

  @Test
  @Disabled // too expensive to run normally
  public void testBigData() throws NoSuchAlgorithmException, IOException {
    OutputStream os = new OutputStream() {
      @Override
      public void write(byte[] b) throws IOException { }

      @Override
      public void write(byte[] b, int off, int len) throws IOException { }

      @Override
      public void write(int b) throws IOException { }
    };

    OutputStream cos = Cryptography.getOutputStream(os);

    byte[] outBuffer = new byte[1024 * 1024];

    // try to write 128GB
    for (int i = 0; i < 1024 * 128; i++) {
      cos.write(outBuffer);
    }
  }
}
