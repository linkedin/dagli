package com.linkedin.dagli.util.kryo;

import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.objectio.kryo.KryoFileWriter;
import com.linkedin.dagli.objectio.kryo.StreamTransformer;
import com.linkedin.dagli.util.cryptography.Cryptography;
import com.linkedin.dagli.util.environment.DagliSystemProperties;
import com.linkedin.migz.MiGzInputStream;
import com.linkedin.migz.MiGzOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class KryoWriters {
  private KryoWriters() { }

  private static final StreamTransformer COMPRESSION_TRANSFORMER = new StreamTransformer() {
    @Override
    public OutputStream transform(OutputStream out) throws Exception {
      return new MiGzOutputStream(out, Runtime.getRuntime().availableProcessors(), MiGzOutputStream.DEFAULT_BLOCK_SIZE);
    }

    @Override
    public InputStream transform(InputStream in) throws Exception {
      return new MiGzInputStream(in, Runtime.getRuntime().availableProcessors());
    }
  };

  private static final StreamTransformer ENCRYPTION_TRANSFORMER = new StreamTransformer() {
    @Override
    public OutputStream transform(OutputStream out) throws Exception {
      return Cryptography.getOutputStream(out);
    }

    @Override
    public InputStream transform(InputStream in) throws Exception {
      return Cryptography.getInputStream(in);
    }
  };

  /**
   * Create a new temp-file-backed ObjectWriterKryo without compression or encryption
   *
   * @param <T> the type of element to be stored
   * @return a new ObjectWriterKryo that will write and read values to a temp file, uncompressed and unencrypted
   */
  public static <T> KryoFileWriter<T> kryo() {
    return kryoFrom((ObjectReader<T>) null);
  }

  /**
   * Creates an instance of {@link KryoFileWriter} that will store its data in a temporary file without compression
   * or encryption.
   *
   * @param valuesToAdd the initial values to initially place in the ObjectWriter; can be null, in which case no
   *                    values will be added.
   *
   * @param <T> the type of object that will be stored in the {@link KryoFileWriter}
   * @return a new {@link KryoFileWriter} that will store its collection of values in a
   *         temporary file.
   */
  public static <T> KryoFileWriter<T> kryoFrom(Iterable<T> valuesToAdd) {
    return kryoFrom(ObjectReader.wrap(valuesToAdd));
  }

  /**
   * Creates an instance of {@link KryoFileWriter} that will store its data in a temporary file without compression
   * or encryption.
   *
   * @param valuesToAdd the initial values to initially place in the ObjectWriter; can be null, in which case no
   *                    values will be added.
   *
   * @param <T> the type of object that will be stored in the {@link KryoFileWriter}
   * @return a new {@link KryoFileWriter} that will store its collection of values in a
   *         temporary file.
   */
  public static <T> KryoFileWriter<T> kryoFrom(ObjectReader<T> valuesToAdd) {
    return kryoFrom(valuesToAdd, false, false);
  }

  /**
   * Creates an instance of {@link KryoFileWriter} that will store its data in a temporary file.
   *
   * @param valuesToAdd the initial values to initially place in the ObjectWriter; can be null, in which case no
   *                    values will be added.
   * @param useCompression whether to use compression (trading CPU for space)
   * @param useDagliEncryption      whether to use encryption provided by {@link Cryptography}.  By default, data so
   *                                encrypted will not be readable in future sessions, even if the underlying temporary
   *                                file survives that long.  Generally not recommended unless you have a specific
   *                                reason to worry about storing unencrypted data on disk (e.g. data security).
   * @param <T> the type of object that will be stored in the {@link KryoFileWriter}
   * @return a new {@link KryoFileWriter} that will store its collection of values in a
   *         temporary file.
   */
  public static <T> KryoFileWriter<T> kryoFrom(ObjectReader<T> valuesToAdd, boolean useCompression,
      boolean useDagliEncryption) {
    try {
      Path tempPath = Files.createTempFile(Paths.get(DagliSystemProperties.getTempDirectory()), "ObjectWriterKryo", ".dat");
      tempPath.toFile().deleteOnExit();

      KryoFileWriter<T> res = kryoFromPath(tempPath, useCompression, useDagliEncryption);

      if (valuesToAdd != null) {
        res.writeAll(valuesToAdd);
      }

      return res;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Creates an instance of {@link KryoFileWriter} that will read and store its data from/to the specified file.
   *
   * @param useCompression whether to use compression (trading CPU for space)
   * @param useDagliEncryption      whether to use encryption provided by {@link Cryptography}.  By default, data so
   *                                encrypted will not be readable in future sessions.  Generally not recommended unless
   *                                you have a specific reason to worry about storing unencrypted data on disk (e.g.
   *                                data security).
   * @param <T> the type of object that will be stored in the {@link KryoFileWriter}
   * @return a new {@link KryoFileWriter} that will store its collection of values in
   *          the specified file.
   */
  public static <T> KryoFileWriter<T> kryoFromPath(Path path, boolean useCompression,
      boolean useDagliEncryption) {
    return kryoFromPath(path, useCompression, useDagliEncryption, new KryoFileWriter.Config());
  }

  /**
   * Creates an instance of {@link KryoFileWriter} that will read and store its data from/to the specified file.
   *
   * @param useCompression whether to use compression (trading CPU for space)
   * @param useDagliEncryption      whether to use encryption provided by {@link Cryptography}.  By default, data so
   *                                encrypted will not be readable in future sessions.  Generally not recommended unless
   *                                you have a specific reason to worry about storing unencrypted data on disk (e.g.
   *                                data security).
   * @param config the configuration to be used for the {@link KryoFileWriter}.  Note that if useCompression or
   *               useEncryption are true, the stream transformers on the config will be replaced/overridden.
   * @param <T> the type of object that will be stored in the {@link KryoFileWriter}
   * @return a new {@link KryoFileWriter} that will store its collection of values in
   *          the specified file.
   */
  public static <T> KryoFileWriter<T> kryoFromPath(Path path, boolean useCompression,
      boolean useDagliEncryption, KryoFileWriter.Config config) {
    if (useCompression && useDagliEncryption) {
      config.setStreamTransformer(COMPRESSION_TRANSFORMER.andThen(ENCRYPTION_TRANSFORMER));
    } else if (useCompression) {
      config.setStreamTransformer(COMPRESSION_TRANSFORMER);
    } else if (useDagliEncryption) {
      config.setStreamTransformer(ENCRYPTION_TRANSFORMER);
    }

    return new KryoFileWriter<>(path, config);
  }
}
