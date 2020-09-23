package com.linkedin.dagli.util.io;

import java.io.IOException;
import java.io.OutputStream;


/**
 * A "virtual stream" backed by another stream (which receives all written data) that can subsequently be read back via
 * a {@link InputSubstream}.  This can be used to provide a stream to a consumer that expects to "own" the stream, while
 * still safely allowing for subsequent writes.
 *
 * This stream <strong>must</strong> be {@link #close()}d, but closing it will not close the underlying stream.
 */
public class OutputSubstream extends OutputStream {
  private final OutputStream _wrapped;
  private final byte[] _buffer;
  private int _bufferPosition = 0;
  private boolean _closed = false;

  /**
   * Creates a new instance that will be backed by the provided stream and use the specified buffer size.
   *
   * The buffer should not be made too large, as this is also the maximum amount of "wasted" extra bytes that may be
   * written to the underlying stream, but also not too small (as smaller buffers require writing more metadata to the
   * underlying stream).  {@code 1024} is a reasonable value, but you may want to go higher or lower depending on the
   * number of bytes you plan to write.
   *
   * @param wrapped the underlying stream that will receive the substream's written data
   * @param bufferSize a buffer size that also determines the size of chunks of data written to the underlying stream
   */
  public OutputSubstream(OutputStream wrapped, int bufferSize) {
    _wrapped = wrapped;
    _buffer = new byte[bufferSize];
  }

  @Override
  public void write(int b) throws IOException {
    if (_closed) {
      throw new IOException("Stream closed");
    }

    if (_bufferPosition == _buffer.length) {
      writeBuffer();
    }
    _buffer[_bufferPosition++] = (byte) b;
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    if (_closed) {
      throw new IOException("Stream closed");
    }

    while (len > 0) {
      if (_bufferPosition == _buffer.length) {
        writeBuffer();
      }

      int toCopy = Math.min(len, _buffer.length - _bufferPosition);
      System.arraycopy(b, off, _buffer, _bufferPosition, toCopy);
      off += toCopy;
      len -= toCopy;
      _bufferPosition += toCopy;
    }
  }

  // writes an int with most-significant-bit-first encoding
  private static byte[] intToBytes(int value) {
    return new byte[]{(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value};
  }

  private void writeBuffer() throws IOException {
    if (_bufferPosition > 0) {
      _wrapped.write(intToBytes(_bufferPosition));
      _wrapped.write(_buffer, 0, _bufferPosition);
      _bufferPosition = 0;
    }
  }

  @Override
  public void flush() throws IOException {
    if (_closed) {
      throw new IOException("Stream closed");
    }

    writeBuffer();
    _wrapped.flush();
  }

  @Override
  public void close() throws IOException {
    if (!_closed) {
      writeBuffer();
      _wrapped.write(intToBytes(0)); // end-of-substream marker
      _closed = true;
    }
  }
}
