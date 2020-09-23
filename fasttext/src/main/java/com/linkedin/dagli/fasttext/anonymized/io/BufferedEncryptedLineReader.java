package com.linkedin.dagli.fasttext.anonymized.io;

import com.linkedin.dagli.util.cryptography.Cryptography;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;


/**
 * A variant of BufferedLineReader that accepts Dagli-encrypted files.
 */
public class BufferedEncryptedLineReader extends BufferedLineReader {
  @Override
  protected InputStream getInputStream() throws IOException {
    try {
      return Cryptography.getInputStream(Files.newInputStream(file_));
    } catch (NoSuchAlgorithmException e) {
      throw new UnsupportedOperationException(e);
    }
  }

  /**
   * Creates a new instance.
   *
   * @param filename the filename of the encrypted file
   * @param charsetName the name of the character set used
   * @throws IOException if something goes wrong while opening the file
   */
  public BufferedEncryptedLineReader(String filename, String charsetName) throws IOException {
    super(filename, charsetName);
  }
}
