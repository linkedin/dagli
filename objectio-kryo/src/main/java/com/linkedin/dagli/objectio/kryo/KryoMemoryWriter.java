package com.linkedin.dagli.objectio.kryo;

import com.linkedin.dagli.objectio.ObjectReader;
import java.io.ByteArrayOutputStream;


/**
 * A writer that writes Kryo-serialized objects to memory.
 *
 * @param <T> the type of element in the iterable
 */
public class KryoMemoryWriter<T> extends AbstractKryoWriter<T> {

  /**
   * Creates an instance that will use memory as its backing store.
   */
  public KryoMemoryWriter() {
    this(new Config());
  }

  /**
   * Creates an instance that will use memory as its backing store.
   *
   * @param config the config to use
   */
  public KryoMemoryWriter(Config config) {
    super(config, new ByteArrayOutputStream(), 0);
  }

  @Override
  protected void writeCount(long count) {
    // noop
  }

  @Override
  public ObjectReader<T> createReader() {
    return new KryoMemoryReader<>(((ByteArrayOutputStream) _proximateOutputStream.getUnderlyingStream()).toByteArray(),
        size64(), _config, new KryoMemoryReader.Config());
  }
}
