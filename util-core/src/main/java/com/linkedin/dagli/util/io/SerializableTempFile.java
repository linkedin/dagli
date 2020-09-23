package com.linkedin.dagli.util.io;

import com.linkedin.dagli.util.environment.DagliSystemProperties;
import com.linkedin.dagli.util.exception.Exceptions;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;


/**
 * Holds a reference to a temporary file.  When serialized, the file is copied into the serialization stream;
 * when deserialized, it is copied back out.
 *
 * The original file wrapped by this class does not actually have to be temporary; it is up to the client to schedule
 * it for deletion (or other cleanup) as desired.  However, when the instance is deserialized a new file with a copy
 * of the data will be created, and that new file <b>will</b> be temporary.
 *
 * Equality testing of {@link SerializableTempFile}s is supported and compares the data stored in the temp files (via
 * an MD5 hash) rather than relying on file names.
 */
public class SerializableTempFile implements Serializable {
  private static final int BUFFER_SIZE = 1024 * 1024;
  private static final String DEFAULT_PREFIX = "serializable_temp_file_";
  private static final String DEFAULT_SUFFIX = ".dat";

  private File _tempFile;
  private String _tempFilePrefix;
  private String _tempFileSuffix;

  // used for equality testing
  private transient byte[] _md5 = null;
  private transient long _lastModified = 0;

  /**
   * Gets the file associated with this instance.  If this is a new instance, this will be the original file.  If it
   * is a deserialized instance, it will be a temporary file copy of the original.
   *
   * @return the temporary file
   */
  public File getFile() {
    return _tempFile;
  }

  /**
   * Creates a new SerializableTempFile from an existing temp file.  Strictly speaking, the temp file does not have
   * to be "temporary", and this is not enforced, nor is the file made "temporary" after calling this constructor.
   *
   * @param tempFile the file being made serializable
   */
  public SerializableTempFile(File tempFile) {
    this(tempFile, DEFAULT_PREFIX, DEFAULT_SUFFIX);
  }

  /**
   * Creates a new SerializableTempFile from an existing temp file.  Strictly speaking, the temp file does not have
   * to be "temporary", and this is not enforced, nor is the file made "temporary" after calling this constructor.
   *
   * @param tempFile the file being made serializable
   * @param deserializedTempFilePrefix prefix that will be used to name the temp file when deserialized
   * @param deserializedTempFileSuffix suffix that will be used to name the temp file when deserialized
   */
  public SerializableTempFile(File tempFile, String deserializedTempFilePrefix, String deserializedTempFileSuffix) {
    _tempFile = tempFile;
    _tempFilePrefix = deserializedTempFilePrefix;
    _tempFileSuffix = deserializedTempFileSuffix;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(calculateHash());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof SerializableTempFile)) {
      return false;
    } else if (this._tempFile.equals(((SerializableTempFile) obj)._tempFile)) {
      // two instances backed by the same file are equal
      return true;
    }

    return Arrays.equals(this.calculateHash(), ((SerializableTempFile) obj).calculateHash());
  }

  /**
   * Calculates (or recalculates) an MD5 hash for the underlying temp file as needed (but uses a cached hash if
   * possible), then returns the hash.  If the file does not exist, null will be returned.
   *
   * Calculation of the hash requires reading all the data of the underlying file and is expensive.
   *
   * @return an MD5 hash code or null if the underlying temp file does not exist
   */
  private byte[] calculateHash() {
    if (_md5 != null && _lastModified == _tempFile.lastModified()) {
      return _md5;
    }

    try (FileInputStream fis = new FileInputStream(_tempFile)) {
      final MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] buffer = new byte[BUFFER_SIZE];
      int read;
      while ((read = fis.read(buffer)) > 0) {
        md.update(buffer, 0, read);
      }

      _md5 = md.digest();
      _lastModified = _tempFile.lastModified();
      return _md5;
    } catch (FileNotFoundException e) {
      // we don't bubble the exception, but we do zero-out our stored hash and timestamp
      _md5 = null;
      _lastModified = 0;
      return null;
    } catch (NoSuchAlgorithmException | IOException e) {
      // NoSuchAlgorithmException this should never happen, as MD5 should be supported by every Java implementation
      // IOException is entirely unexpected and needs to be reported to the client
      throw Exceptions.asRuntimeException(e);
    }
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    out.writeObject(_tempFilePrefix);
    out.writeObject(_tempFileSuffix);

    try (FileInputStream fis = new FileInputStream(_tempFile)) {
      byte[] buffer = new byte[BUFFER_SIZE];
      int read;
      while ((read = fis.read(buffer)) > 0) {
        out.write(buffer, 0, read);
      }
    }
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    _tempFilePrefix = (String) in.readObject();
    _tempFileSuffix = (String) in.readObject();

    _tempFile =
        Files.createTempFile(Paths.get(DagliSystemProperties.getTempDirectory()), _tempFilePrefix, _tempFileSuffix)
            .toFile();
    _tempFile.deleteOnExit();

    try (FileOutputStream fos = new FileOutputStream(_tempFile)) {
      final MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] buffer = new byte[BUFFER_SIZE];
      int read;
      while ((read = in.read(buffer)) > 0) {
        md.update(buffer, 0, read);
        fos.write(buffer, 0, read);
      }
      _md5 = md.digest();
      _lastModified = _tempFile.lastModified();
    } catch (NoSuchAlgorithmException e) {
      // this should never happen, as MD5 should be supported by every Java implementation
      throw Exceptions.asRuntimeException(e);
    }

  }
}
