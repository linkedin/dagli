package com.linkedin.dagli.objectio.kryo;

import java.io.ByteArrayInputStream;
import java.io.InputStream;


/**
 * A reader of Kryo data from memory.
 *
 * @param <T> the type of element in the iterable
 */
class KryoMemoryReader<T> extends AbstractKryoReader<T> {
  private final byte[] _data;

  /**
   * Creates an instance that will use memory as its backing store.
   */
  public KryoMemoryReader(byte[] data, long count, KryoMemoryWriter.Config writerConfig,
      KryoMemoryReader.Config readerConfig) {
    super(readerConfig, writerConfig.getCacheHorizon(), writerConfig.isUnsafeIO(), count);
    _data = data;
  }

  @Override
  protected InputStream getInputStream() {
    return new ByteArrayInputStream(_data);
  }

  @Override
  protected boolean hasBlockingIO() {
    return false;
  }
}
