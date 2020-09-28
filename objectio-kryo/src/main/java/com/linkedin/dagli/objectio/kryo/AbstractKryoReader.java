package com.linkedin.dagli.objectio.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.UnsafeInput;
import com.linkedin.dagli.objectio.ObjectIterator;
import com.linkedin.dagli.objectio.ObjectReader;
import java.io.InputStream;
import java.util.NoSuchElementException;


/**
 * {@link ObjectReader} that reads Kryo-serialized objects from a stream.
 *
 * <strong>You must trust the data that you are reading.</strong>  Using data of uncertain provenance (e.g. supplied
 * by users) will create a security hole, since deserializing such data can allow for arbitrary code execution.
 *
 * No compatibility of the underlying streams should be expected across versions; this may change in the future.
 */
abstract class AbstractKryoReader<T> implements ObjectReader<T> {
  /**
   * The configuration for this instance
   */
  protected final Config _config;

  private final int _cacheHorizon;
  private final boolean _unsafeIO;
  private final long _count;

  /**
   * @return the input stream that will be used to read Kryo-serialized objects
   */
  protected abstract InputStream getInputStream();

  /**
   * @return true if this instance requires blocking I/O, false if reading objects may block
   */
  protected abstract boolean hasBlockingIO();

  /**
   * Configuration class that stores the settings for a Kryo reader.
   */
  public static class Config implements Cloneable {
    private static final int DEFAULT_INPUT_BUFFER_SIZE = 1024 * 1024;

    private int _inputBufferSize = DEFAULT_INPUT_BUFFER_SIZE;
    private StreamTransformer _streamTransformer = StreamTransformer.IDENTITY;

    @Override
    public Config clone() {
      try {
        return (Config) super.clone();
      } catch (CloneNotSupportedException e) {
        throw new RuntimeException(e);
      }
    }

    /**
     * Gets the {@link StreamTransformer} that will be used to transform serialized bytes.  The default transformer
     * is the identity function (i.e. no transformation).
     *
     * @return the transformer that will be used
     */
    public StreamTransformer getStreamTransformer() {
      return _streamTransformer;
    }

    /**
     * Sets the {@link StreamTransformer} that will be used to transform serialized bytes.  The default transformer
     * is the identity function (i.e. no transformation).
     *
     * @param streamTransformer the transformer to use
     * @return this {@link Config}
     */
    public Config setStreamTransformer(StreamTransformer streamTransformer) {
      _streamTransformer = streamTransformer;
      return this;
    }

    /**
     * @return the buffer size used by Kryo when deserializing objects
     */
    public int getInputBufferSize() {
      return _inputBufferSize;
    }

    /**
     * Sets the buffer size used by Kryo when deserializing objects.  Larger values may improve performance at the cost
     * of memory consumption.  The default value is 1MB.
     *
     * @param inputBufferSize the buffer size to use
     * @return this {@link Config}
     */
    public Config setInputBufferSize(int inputBufferSize) {
      _inputBufferSize = inputBufferSize;
      return this;
    }
  }

  /**
   * Creates a new Kryo reader.
   *  @param config the configuration to use for the reader
   * @param cacheHorizon the cache horizon: how many objects should be deserialized between resets of the store of
   *                     cached sub-objects.  Must match the horizon specified when the objects were originally
   *                     serialized!
   * @param unsafeIO whether to use unsafe IO.  Must match the setting used when objects were originally serialized
   * @param count the number of objects that will be read by this reader
   */
  protected AbstractKryoReader(Config config, int cacheHorizon, boolean unsafeIO, long count) {
    _config = config;
    _cacheHorizon = cacheHorizon;
    _unsafeIO = unsafeIO;
    _count = count;
  }

  @Override
  public long size64() {
    return _count;
  }

  @Override
  public Iterator<T> iterator() {
    return new Iterator<>(this);
  }

  @Override
  public void close() { }

  /**
   * An iterator from Kryo-serialized objects.
   *
   * @param <T> the type of objects to be read
   */
  public static class Iterator<T> implements ObjectIterator<T> {
    StackTraceElement[] stack;
    boolean closed = false;

    private final Input _input;
    private final long _toReadCount;
    private final long _cacheHorizon;
    private final Kryo _kryo;
    private final boolean _hasBlockingIO;

    private long _readCount = 0;

    /**
     * Creates a new iterator that will read objects provided by the corresponding reader
     *
     * @param owner the reader to be iterated over
     */
    public Iterator(AbstractKryoReader<T> owner) {
      stack = Thread.currentThread().getStackTrace();

      InputStream inputStream = owner._config.getStreamTransformer().transformUnchecked(owner.getInputStream());
      _input = owner._unsafeIO ? new UnsafeInput(inputStream, owner._config._inputBufferSize)
          : new Input(inputStream, owner._config._inputBufferSize);

      _hasBlockingIO = owner.hasBlockingIO();
      _toReadCount = owner._count;
      _cacheHorizon = owner._cacheHorizon;

      _kryo = new Kryo();
      _kryo.setRegistrationRequired(false);
      _kryo.setAutoReset(false);
    }

    @Override
    public boolean hasNext() {
      return _readCount < _toReadCount;
    }

    @Override
    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      return readObject();
    }

    @Override
    public int tryNextAvailable(Object[] destination, int offset, int count) {
      if (_hasBlockingIO) {
        return 0;
      }

      // non-blocking
      return next(destination, offset, count);
    }

    /**
     * @return the next object deserialized from the input
     */
    private T readObject() {
      @SuppressWarnings("unchecked")
      T res = (T) _kryo.readClassAndObject(_input);
      _readCount++;
      if (_readCount % _cacheHorizon == 0) {
        _kryo.reset();
      }
      return res;
    }

    @Override
    public int next(Object[] destination, int offset, int count) {
      count = (int) Math.min(count, _toReadCount - _readCount);

      for (int i = offset; i < offset + count; i++) {
        destination[i] = readObject();
      }

      return count;
    }

    @Override
    public void close() {
      _input.close();
      closed = true;
    }
  }
}
