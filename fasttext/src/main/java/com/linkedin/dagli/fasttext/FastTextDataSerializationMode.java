package com.linkedin.dagli.fasttext;

import com.linkedin.dagli.fasttext.anonymized.io.LineReader;
import com.linkedin.dagli.fasttext.anonymized.io.BufferedCompressedAndEncryptedLineReader;
import com.linkedin.dagli.fasttext.anonymized.io.BufferedCompressedLineReader;
import com.linkedin.dagli.fasttext.anonymized.io.BufferedEncryptedLineReader;
import com.linkedin.dagli.fasttext.anonymized.io.BufferedLineReader;


/**
 * Determines whether temporary data files created for the FastText model will be compressed and/or encrypted.
 *
 * Compression can make sense for slower, spinning disks, where disk I/O across many threads on high-core count
 * machines becomes a bottleneck.  It also reduces the space required on disk.
 *
 * Encryption adds computational cost and is only warranted in circumstances where training examples being written to
 * disk in plaintext poses a security concern.
 */
public enum FastTextDataSerializationMode {
  /**
   * The default behavior.  Currently this is identical to NORMAL.
   */
  DEFAULT(false, false),

  /**
   * No compression or encryption is used.  Recommended if:
   * (1) You have an SSD or the temporary files are otherwise being written to fast storage (e.g. RAM disk)
   * (2) You have a small or medium datasets where the data is likely to simply get stored in the RAM disk cache.
   * (3) Your machine has a relatively low number of cores (the cost of compression would outweigh the reduction in
   *     disk I/O).
   */
  NORMAL(false, false),

  /**
   * Data will be compressed.  Recommended if you have a very large data set, a spinning disk, and many cores.
   */
  COMPRESSED(true, false),

  /**
   * Data will be encrypted using {@link com.linkedin.dagli.util.cryptography.Cryptography}.
   */
  ENCRYPTED(false, true),

  /**
   * Data will be both compressed and encrypted.
   */
  COMPRESSED_AND_ENCRYPTED(true, true);

  private boolean _compressed;
  private boolean _encrypted;
  private Class<? extends LineReader> _lineReaderClass;

  FastTextDataSerializationMode(boolean compressed, boolean encrypted) {
    _compressed = compressed;
    _encrypted = encrypted;
  }

  public boolean isCompressed() {
    return _compressed;
  }

  public boolean isEncrypted() {
    return _encrypted;
  }

  Class<? extends LineReader> getLineReaderClass() {
    if (isCompressed() && isEncrypted()) {
      return BufferedCompressedAndEncryptedLineReader.class;
    } else if (isCompressed()) {
      return BufferedCompressedLineReader.class;
    } else if (isEncrypted()) {
      return BufferedEncryptedLineReader.class;
    } else {
      return BufferedLineReader.class;
    }
  }
}
