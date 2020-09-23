package com.linkedin.dagli.objectio.kryo;

import com.linkedin.dagli.objectio.ObjectReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;


/**
 * Writes objects to a file using Kryo serialization.
 *
 * @param <T> the type of object to be written
 */
public class KryoFileWriter<T> extends AbstractKryoWriter<T> {
  private final Path _path;
  private static final int HEADER_SIZE = 13; // # bytes of metadata stored in addition to serialized object data

  /**
   * Creates a new instance using the specified file as a backing store.  If the file does not already exist it will be
   * created.
   *
   * @param path the place where items will be stored.
   */
  public KryoFileWriter(Path path) {
    this(path, new Config());
  }

  /**
   * Creates a new instance using the specified file as a backing store.  If the file does not already exist it will be
   * created.
   *
   * @param path the place where items will be stored.
   * @param config a configuration to use; some parameters may be overridden if the file already exists
   */
  public KryoFileWriter(Path path, Config config) {
    this(path, createWriterInfo(path, config));
  }

  /**
   * Creates a new instance.
   *
   * @param path the path to be written or appended to
   * @param writerInfo a {@link WriterInfo} that bundles the objects needed to instantiate this writer
   */
  private KryoFileWriter(Path path, WriterInfo writerInfo) {
    super(writerInfo._config, writerInfo._outputStream, writerInfo._count);
    _path = path;
  }

  /**
   * Opens or creates an output stream for the provided path, checks for previously-written items (if the path already
   * exists) and bundles this information together with the configuration as a returned WriterInfo object.
   *
   * @param path the path of the file to be written or appended to
   * @param config the configuration for this Kryo writer
   * @return a WriterInfo that bundles the information needed to create the Kryo writer instance
   */
  private static WriterInfo createWriterInfo(Path path, Config config) {
    config = config.clone();
    WriterInfo res = new WriterInfo();
    res._config = config;

    try {
      boolean isNewFile = Files.notExists(path) || Files.size(path) < HEADER_SIZE;
      if (isNewFile) {
        writeHeader(path, 0, config.getCacheHorizon(), config.isUnsafeIO());
      } else {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(path.toFile(), "r")) {
          randomAccessFile.seek(0);
          res._count = randomAccessFile.readLong();
          config.setCacheHorizon(randomAccessFile.readInt());
          config.setUnsafeIO(randomAccessFile.readBoolean());
        }
      }

      res._outputStream = Files.newOutputStream(path, StandardOpenOption.APPEND);
      return res;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Stores the writer's configuration, output stream and existing item count (when appending).
   */
  private static class WriterInfo {
    Config _config = null;
    OutputStream _outputStream = null;
    long _count = 0;
  }

  /**
   * Writes the file's header.
   *
   * @param path the path of the file whose header will be written (at position 0)
   * @param count the number of items stored in this file
   * @param cacheHorizon the cache horizon used when serializing the items
   * @param useUnsafeIO whether or not unsafe IO was used when serializing the items
   */
  private static void writeHeader(Path path, long count, int cacheHorizon, boolean useUnsafeIO) {
    try (RandomAccessFile randomAccessFile = new RandomAccessFile(path.toFile(), "rw")) {
      randomAccessFile.seek(0);
      randomAccessFile.writeLong(count);
      randomAccessFile.writeInt(cacheHorizon);
      randomAccessFile.writeBoolean(useUnsafeIO);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  protected void writeCount(long count) {
    writeHeader(_path, count, _config.getCacheHorizon(), _config.isUnsafeIO());
  }

  /**
   * @return a Kryo reader configuration suitable for reading the items from the file produced by this writer.
   */
  private KryoFileReader.Config getDefaultReaderConfig() {
    KryoFileReader.Config defaultConfig = new KryoFileReader.Config();
    defaultConfig.setStreamTransformer(_config.getStreamTransformer());
    return defaultConfig;
  }

  @Override
  public ObjectReader<T> createReader() {
    return new KryoFileReader<T>(_path, getDefaultReaderConfig());
  }

  /**
   * Creates a reader that will read the items written to this writer using the specified inputBufferSize.
   * See {@link KryoFileReader.Config#setInputBufferSize(int)} for details.
   *
   * @param inputBufferSize the input buffer size to use
   * @return a new reader that will read the items written to this writer
   */
  public ObjectReader<T> createReader(int inputBufferSize) {
    KryoFileReader.Config config = getDefaultReaderConfig();
    config.setInputBufferSize(inputBufferSize);
    return new KryoFileReader<T>(_path, config);
  }
}
