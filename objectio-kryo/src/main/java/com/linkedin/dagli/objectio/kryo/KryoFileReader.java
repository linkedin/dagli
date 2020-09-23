package com.linkedin.dagli.objectio.kryo;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * Reads Kryo-serialized objects from a file previously written by {@link KryoFileWriter}.
 *
 * @param <T> the type of element to read
 */
public class KryoFileReader<T> extends AbstractKryoReader<T> {
  private final Path _path;
  private static final int HEADER_SIZE = 13;

  /**
   * Creates a new instance that will read from the specified path.
   *
   * @param path the place where items will be read from
   */
  public KryoFileReader(Path path) {
    this(path, new Config());
  }

  /**
   * Creates a new instance that will read from the specified path.
   *
   * @param path the place where items will be read from
   * @param config a configuration to use
   */
  public KryoFileReader(Path path, Config config) {
    this(path, config, getFileInfo(path));
  }

  private KryoFileReader(Path path, Config config, FileInfo fileInfo) {
    super(config, fileInfo._cacheHorizon, fileInfo._unsafeIO, fileInfo._count);
    _path = path;
  }

  /**
   * Structure for Kryo-critical information (needed to read Kryo records) stored in files created by
   * {@link KryoFileWriter}.
   *
   * See the getters/setters in {@link AbstractKryoReader.Config} for definitions of these
   * fields.
   */
  private static class FileInfo {
    int _cacheHorizon;
    boolean _unsafeIO;
    long _count;
  }

  /**
   * Gets the Kryo-critical information needed to read Kryo records stored in a file created by {@link KryoFileWriter}.
   *
   * @param path the path of the file
   * @return the required Kryo metadata
   */
  private static FileInfo getFileInfo(Path path) {
    FileInfo res = new FileInfo();
    try (RandomAccessFile randomAccessFile = new RandomAccessFile(path.toFile(), "r")) {
      randomAccessFile.seek(0);
      res._count = randomAccessFile.readLong();
      res._cacheHorizon = randomAccessFile.readInt();
      res._unsafeIO = randomAccessFile.readBoolean();
      return res;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  protected InputStream getInputStream() {
    try {
      InputStream res = Files.newInputStream(_path);
      long toSkip = HEADER_SIZE;
      while (toSkip > 0) {
        toSkip -= res.skip(toSkip);
      }
      return res;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  protected boolean hasBlockingIO() {
    return true;
  }
}
