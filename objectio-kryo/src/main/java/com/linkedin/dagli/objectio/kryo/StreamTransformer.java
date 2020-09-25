package com.linkedin.dagli.objectio.kryo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;


/**
 * A {@link StreamTransformer} represents a pair of transformation methods:
 * - an {@link OutputStream} transformer that wraps a provided {@link OutputStream} to provide, e.g. encryption and/or
 *   compression
 * - an {@link InputStream} transformer that wraps a provided {@link InputStream} to "reverse" whatever the output
 *   stream wrapper did, e.g. decryption and/or decompression.
 */
public interface StreamTransformer {
  /**
   * Returns a new StreamTransformer that transforms with the following mapping:
   * <pre>{@code
   * outputStream -> this.transform(other.transform(outputStream))
   * inputStream -> this.transform(other.transform(inputStream))
   * }</pre>
   *
   * @param other the other transformer to compose with this one
   * @return a new transformer created by composing this transformer with another.  Written bytes are transformed with
   *         this transformer first, then the other.  Read bytes are transformed by the other transformer first, then
   *         with this one.
   */
  default StreamTransformer andThen(StreamTransformer other) {
    final StreamTransformer original = this;
    return new StreamTransformer() {
      @Override
      public OutputStream transform(OutputStream out) throws Exception {
        return original.transform(other.transform(out));
      }

      @Override
      public InputStream transform(InputStream in) throws Exception {
        return original.transform(other.transform(in));
      }
    };
  }

  /**
   * Wraps the provided {@link OutputStream} to add, e.g. encryption and/or compression for values written to that
   * stream.
   *
   * @param out the original {@link OutputStream}
   * @return a new {@link OutputStream} that transforms written bytes "somehow" and writes them to the original stream
   */
  OutputStream transform(OutputStream out) throws Exception;

  /**
   * Wraps the provided {@link InputStream} to add, e.g. decryption and/or decompression for values written to that
   * stream.
   *
   * @param in the original {@link InputStream}
   * @return a new {@link InputStream} that reads bytes from the original stream and transforms them "somehow"
   */
  InputStream transform(InputStream in) throws Exception;

  /**
   * Wraps the provided {@link OutputStream} to add, e.g. encryption and/or compression for values written to that
   * stream.
   *
   * @param out the original {@link OutputStream}
   * @return a new {@link OutputStream} that transforms written bytes "somehow" and writes them to the original stream
   */
  default OutputStream transformUnchecked(OutputStream out) {
    try {
      return transform(out);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Wraps the provided {@link InputStream} to add, e.g. decryption and/or decompression for values written to that
   * stream.
   *
   * @param in the original {@link InputStream}
   * @return a new {@link InputStream} that reads bytes from the original stream and transforms them "somehow"
   */
  default InputStream transformUnchecked(InputStream in) {
    try {
      return transform(in);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * A StreamTransformer that returns the original streams unaltered.
   */
  StreamTransformer IDENTITY = new StreamTransformer() {
    @Override
    public OutputStream transform(OutputStream out) {
      return out;
    }

    @Override
    public InputStream transform(InputStream in) {
      return in;
    }
  };
}
