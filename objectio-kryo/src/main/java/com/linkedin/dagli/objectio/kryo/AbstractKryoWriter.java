package com.linkedin.dagli.objectio.kryo;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.esotericsoftware.kryo.kryo5.unsafe.UnsafeOutput;
import com.linkedin.dagli.objectio.ObjectWriter;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;


/**
 * {@link ObjectWriter} that serializes its data with Kryo and stores it to a stream.
 *
 * Please note: no compatibility of the underlying streams should be expected across versions; this may change in the
 * future.
 */
abstract class AbstractKryoWriter<T> implements ObjectWriter<T> {
  private final Kryo _kryo = createKryo();
  protected Output _output;
  protected ManuallyFlushedOutputStream _proximateOutputStream; // need a pointer to this so we can flush for real
  protected Config _config;

  private long _appendedCount;

  private boolean _isClosed = false;

  /**
   * Stores the total number of objects written to this writer.  This is only called by
   * {@link AbstractKryoWriter} once, when the writer is closed.
   *
   * @param count the number of objects written
   */
  protected abstract void writeCount(long count);

  /**
   * Kryo likes to flush...constantly.  This isn't good for efficiency.  We use this class to control when flushes
   * <b>really</b> happen.
   */
  protected static class ManuallyFlushedOutputStream extends FilterOutputStream {
    /**
     * Creates an output stream built on top of the specified
     * underlying output stream.
     *
     * @param   out   the underlying output stream
     */
    public ManuallyFlushedOutputStream(OutputStream out) {
      super(out);
    }

    /**
     * @return the underlying stream wrapped by this instance
     */
    public OutputStream getUnderlyingStream() {
      return this.out;
    }

    @Override
    public void flush() throws IOException {
      // do nothing
    }

    /**
     * Flushes the underlying stream "for real"
     *
     * @throws IOException if flushing throws an IOException
     */
    public void reallyFlush()  throws IOException {
      super.flush(); // actually flush
    }
  }

  /**
   * Represents the configuration used for Kryo serialization.
   */
  public static class Config implements Cloneable {
    private static final int DEFAULT_OUTPUT_BUFFER_SIZE = 1024 * 1024;
    private static final int DEFAULT_CACHE_HORIZON = 1024;

    private boolean _unsafeIO = true;
    private int _initialOutputBufferSize = DEFAULT_OUTPUT_BUFFER_SIZE;
    private int _cacheHorizon = DEFAULT_CACHE_HORIZON;
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
     * is the identity function (i.e. no transformation)
     *
     * @return the transformer that will be used
     */
    public StreamTransformer getStreamTransformer() {
      return _streamTransformer;
    }

    /**
     * Sets the {@link StreamTransformer} that will be used to transform serialized bytes.  The default transformer
     * is the identity function (i.e. no transformation)
     *
     * @param streamTransformer the transformer to use
     */
    public Config setStreamTransformer(StreamTransformer streamTransformer) {
      _streamTransformer = streamTransformer;
      return this;
    }

    /**
     * If true, faster "unsafe" IO will be used.  Unsafe IO is not portable across architectures (i.e. do not expect
     * data serialized on x86 to work on a PowerPC processor).
     *
     * @return true if unsafe IO will be used, false otherwise
     */
    public boolean isUnsafeIO() {
      return _unsafeIO;
    }

    /**
     * Sets whether or not faster "unsafe" IO will be used.  Unsafe IO is not portable across architectures (i.e. do not
     * expect data serialized on x86 to work on a PowerPC processor).
     *
     * The default setting is to use unsafe IO.
     *
     * @return true if unsafe IO will be used, false otherwise
     */
    public Config setUnsafeIO(boolean useUnsafeIO) {
      _unsafeIO = useUnsafeIO;
      return this;
    }

    /**
     * @return the initial size of the output buffer used by Kryo.  This buffer size will be increased if necessary to
     * write larger objects.
     */
    public int getInitialOutputBufferSize() {
      return _initialOutputBufferSize;
    }

    /**
     * Sets the initial size of the output buffer used by Kryo.  This buffer size will be increased if necessary to
     * write larger objects.  Larger values may increase write speed at the expense of memory consumption.
     *
     * The default value is 1MB.
     *
     * @param initialOutputBufferSize the initial buffer size to use
     * @return this {@link Config} instance
     */
    public Config setInitialOutputBufferSize(int initialOutputBufferSize) {
      _initialOutputBufferSize = initialOutputBufferSize;
      return this;
    }

    /**
     * Gets the "cache horizon".  When Kryo serializes objects, these objects and their sub-objects (those referenced by
     * the serialized object) are cached so that, when serializing subsequent items that refer to them, these objects
     * do not need to be re-written to the underlying stream (instead, a backreference is written).  The cache horizon
     * controls how many objects are written between resets of that cache.
     *
     * Larger cache horizons potentially speed up writing and reduce the storage space required but increase memory
     * consumption.  The default cache horizon is 1024.
     *
     * @return the cache horizon
     */
    public int getCacheHorizon() {
      return _cacheHorizon;
    }

    /**
     * Sets the "cache horizon".  When Kryo serializes objects, these objects and their sub-objects (those referenced by
     * the serialized object) are cached so that, when serializing subsequent items that refer to them, these objects
     * do not need to be re-written to the underlying stream (instead, a backreference is written).  The cache horizon
     * controls how many objects are written between resets of that cache.
     *
     * Larger cache horizons potentially speed up writing and reduce the storage space required but increase memory
     * consumption.  The default cache horizon is 1024.
     *
     * @param cacheHorizon the size of the cache horizon to use
     * @return this {@link Config} instance
     */
    public Config setCacheHorizon(int cacheHorizon) {
      _cacheHorizon = cacheHorizon;
      return this;
    }
  }

  /**
   * Creates and returns a new Kryo instance.
   * @return a new Kryo instance
   */
  private static Kryo createKryo() {
    Kryo res = new Kryo();
    res.setAutoReset(false);
    res.setRegistrationRequired(false);
    res.setReferences(true);
    return res;
  }

  /**
   * Reset iff we're at the end of the current caching period.
   */
  private void maybeReset() {
    if (_appendedCount % _config._cacheHorizon == 0) {
      _kryo.reset();
    }
  }

  /**
   * Creates a new instance of a Kryo writer.
   *
   * @param config the configuration to use
   * @param outputStream the stream to which Kryo-serialized objects will be written
   * @param initialCount the number of objects already written to this stream previous (e.g. when appending to a file)
   */
  public AbstractKryoWriter(Config config, OutputStream outputStream, long initialCount) {
    _config = config;
    _output = _config._unsafeIO ? new UnsafeOutput(_config._initialOutputBufferSize, -1)
        : new Output(_config._initialOutputBufferSize, -1);

    _appendedCount = initialCount;

    _proximateOutputStream =
        new ManuallyFlushedOutputStream(config.getStreamTransformer().transformUnchecked(outputStream));
    _output.setOutputStream(_proximateOutputStream);
  }

  @Override
  public void write(T obj) {
    _appendedCount++;
    _kryo.writeClassAndObject(_output, obj);
    maybeReset();
  }

  @Override
  public void close() {
    if (!_isClosed) {
      _isClosed = true;
      _output.close();
      writeCount(_appendedCount);
    }
  }

  @Override
  public long size64() {
    return _appendedCount;
  }
}
