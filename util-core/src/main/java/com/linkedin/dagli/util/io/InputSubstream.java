package com.linkedin.dagli.util.io;

import java.io.IOException;
import java.io.InputStream;


/**
 * A "virtual stream" backed by another stream (which provides all the read data), which has been previously written
 * via {@link OutputSubstream}.  This can be used to provide a stream to a consumer that expects to "own" the stream,
 * while still safely allowing for subsequent reads.
 *
 * {@link #close()} the stream to consume any remaining bytes of the substream and make the wrapped stream ready to
 * read subsequent data.  Closing the substream does not close the underlying stream.
 */
public class InputSubstream extends InputStream {
  private final InputStream _wrapped;
  private int _remainingInChunk;
  private boolean _closed = false;
  private boolean _ignoreClose = false;

  /**
   * Creates a new instance that will be backed by the provided stream.
   *
   * @param wrapped the backing stream providing the read bytes
   */
  public InputSubstream(InputStream wrapped) {
    _wrapped = wrapped;
  }

  /**
   * Configures this stream to ignore (or not) calls to {@link #close()}.
   *
   * The stream will not allow reads beyond the end of the substream regardless of whether closing is enabled.
   *
   * The default behavior is to not ignore a user-requested close.
   *
   * @param ignore whether to ignore calls to the close() method or not
   * @return this instance
   */
  public InputSubstream setIgnoreClose(boolean ignore) {
    _ignoreClose = ignore;
    return this;
  }

  private void nextChunk() throws IOException {
    if (!_closed) {
      byte[] buf = new byte[4];
      int readSoFar = 0;
      while (readSoFar < 4) {
        readSoFar += _wrapped.read(buf, readSoFar, 4 - readSoFar);
      }
      _remainingInChunk = fromBytes(buf);
      if (_remainingInChunk == 0) {
        _closed = true;
      }
    }
  }

  @Override
  public int available() throws IOException {
    if (_closed) {
      return 0;
    }
    int wrappedAvailable = _wrapped.available();
    if (_remainingInChunk == 0 && wrappedAvailable >= 4) {
      nextChunk();
      wrappedAvailable -= 4;
    }

    // return the number of bytes we can be sure are available:
    return Math.min(_remainingInChunk, wrappedAvailable);
  }

  // convert bytes (encoded in most-significant-bit-first format) to int
  private static int fromBytes(byte[] bytes) {
    return ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16) | ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
  }

  @Override
  public int read() throws IOException {
    byte[] buf = new byte[1];
    if (read(buf, 0, 1) == -1) {
      return -1;
    }
    return buf[0];
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    if (_closed) {
      return -1;
    }

    if (_remainingInChunk == 0) {
      nextChunk();
      if (_closed) {
        return -1;
      }
    }

    int read = _wrapped.read(b, off, Math.min(_remainingInChunk, len));
    _remainingInChunk -= read;
    return read;
  }

  @Override
  public void close() throws IOException {
    if (!_ignoreClose && !_closed) {
      byte[] buffer = new byte[1024];
      while (read(buffer) >= 1) { } // exhaust the substream
      assert _closed;
    }
  }
}
