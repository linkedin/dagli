package com.linkedin.dagli.fasttext.anonymized.io;

import com.linkedin.dagli.util.cryptography.Cryptography;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;


/**
 * A variant of BufferedLineReader that accepts Gzip compressed Dagli-encrypted files.
 */
public class BufferedCompressedAndEncryptedLineReader extends BufferedLineReader {
  @Override
  protected InputStream getInputStream() throws IOException {
    try {
      return new GZIPInputStream(Cryptography.getInputStream(Files.newInputStream(file_)));
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
  public BufferedCompressedAndEncryptedLineReader(String filename, String charsetName) throws IOException {
    super(filename, charsetName);
  }
}
