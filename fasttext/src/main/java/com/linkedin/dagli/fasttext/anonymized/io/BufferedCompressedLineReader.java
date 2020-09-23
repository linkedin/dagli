package com.linkedin.dagli.fasttext.anonymized.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.zip.GZIPInputStream;


/**
 * A variant of BufferedLineReader that accepts Gzip compressed files.
 */
public class BufferedCompressedLineReader extends BufferedLineReader {
  @Override
  protected InputStream getInputStream() throws IOException {
    return new GZIPInputStream(Files.newInputStream(file_));
  }

  /**
   * Creates a new instance.
   *
   * @param filename the filename of the encrypted file
   * @param charsetName the name of the character set used
   * @throws IOException if something goes wrong while opening the file
   */
  public BufferedCompressedLineReader(String filename, String charsetName) throws IOException {
    super(filename, charsetName);
  }
}
