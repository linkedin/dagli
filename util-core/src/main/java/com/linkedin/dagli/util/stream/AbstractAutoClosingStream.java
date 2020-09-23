package com.linkedin.dagli.util.stream;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.BaseStream;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;


/**
 * Base class for Streams that close themselves on terminal operations.
 *
 * For a brief discussion of why Java's built-in streams do not auto-close on terminal operations, you may be interested
 * in reading:
 * https://stackoverflow.com/questions/28813637/why-doesnt-java-close-stream-after-a-terminal-operation-is-issued
 *
 * @param <T> the type of the element enumerated by the stream
 * @param <W> the type of built-in stream being wrapped (e.g. IntStream)
 * @param <S> the type of the derived auto-closing stream (descendant of this class)
 */
abstract class AbstractAutoClosingStream<T, W extends BaseStream<T, W>, S extends W>
    implements BaseStream<T, W> {

  W _wrapped;

  /**
   * Creates a new instance that will wrap the provided stream.  All method calls on this stream will be forwarded to
   * the wrapped stream.
   *
   * @param wrappedStream the stream to wrap
   * @throws IllegalArgumentException if <code>wrappedStream</code> is already an AbstractAutoClosingStream
   */
  AbstractAutoClosingStream(W wrappedStream) {
    if (wrappedStream instanceof AbstractAutoClosingStream) {
      throw new IllegalArgumentException(
          "Cannot create an AutoClosingStream that wraps another AutoClosingStream.  If you want to ensure "
              + "auto-closure of an existing stream of uncertain provenance, call AutoClosingStream::wrap(...)");
    }

    _wrapped = wrappedStream;
  }

  <R> R closeOnException(Supplier<R> resultSupplier) {
    try {
      return resultSupplier.get();
    } catch (Throwable e) {
      _wrapped.close();
      throw e;
    }
  }

  void terminal(Runnable resultSupplier) {
    try {
      resultSupplier.run();
    } finally {
      _wrapped.close();
    }
  }


  <R> R terminal(Supplier<R> resultSupplier) {
    try {
      return resultSupplier.get();
    } finally {
      _wrapped.close();
    }
  }

  @SuppressWarnings("unchecked") // we know that this is of type S because all of this class's descendants honor this
  S setWrapped(Supplier<W> streamSupplier) {
    try {
      _wrapped = streamSupplier.get();
      return (S) this;
    } catch (Throwable e) {
      _wrapped.close();
      throw e;
    }
  }

  <R> AutoClosingStream<R> newStream(Supplier<Stream<R>> streamSupplier) {
    try {
      return new AutoClosingStream<>(streamSupplier.get());
    } catch (Throwable e) {
      _wrapped.close();
      throw e;
    }
  }

  AutoClosingIntStream newIntStream(Supplier<IntStream> streamSupplier) {
    try {
      return new AutoClosingIntStream(streamSupplier.get());
    } catch (Throwable e) {
      _wrapped.close();
      throw e;
    }
  }

  AutoClosingLongStream newLongStream(Supplier<LongStream> streamSupplier) {
    try {
      return new AutoClosingLongStream(streamSupplier.get());
    } catch (Throwable e) {
      _wrapped.close();
      throw e;
    }
  }

  AutoClosingDoubleStream newDoubleStream(Supplier<DoubleStream> streamSupplier) {
    try {
      return new AutoClosingDoubleStream(streamSupplier.get());
    } catch (Throwable e) {
      _wrapped.close();
      throw e;
    }
  }

  @Override
  public final int hashCode() {
    return _wrapped.hashCode() + this.getClass().hashCode();
  }

  @Override
  public final boolean equals(Object obj) {
    return obj != null && obj.getClass().equals(this.getClass()) && Objects.equals(this._wrapped,
        ((AbstractAutoClosingStream) obj)._wrapped);
  }

  @Override
  public final String toString() {
    return "AutoClosing<" + _wrapped.toString() + ">";
  }

  @Override
  public final boolean isParallel() {
    return _wrapped.isParallel();
  }

  @Override
  public final S sequential() {
    return setWrapped(() ->  _wrapped.sequential());
  }

  @Override
  public final S parallel() {
    return setWrapped(() -> _wrapped.parallel());
  }

  @Override
  public final S unordered() {
    return setWrapped(() -> _wrapped.unordered());
  }

  @Override
  public final S onClose(Runnable closeHandler) {
    return setWrapped(() -> _wrapped.onClose(closeHandler));
  }

  @Override
  public final void close() {
    _wrapped.close();
  }
}
