package com.linkedin.dagli.util.stream;

import java.util.LongSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import java.util.stream.LongStream;


/**
 * An {@link AutoClosingLongStream} wraps another {@link LongStream} and calls {@link LongStream#close()} on the wrapped
 * stream when a terminal operation is called (other than {@link LongStream#iterator()} or
 * {@link LongStream#spliterator()} or the underlying stream throws an exception.  It does <strong>not</strong> close the
 * stream via {@link Object#finalize()}; "dangling" streams for which a terminal operation is never invoked should thus
 * be avoided.
 *
 * {@link AutoClosingLongStream} cannot clean up resources if an exception occurs outside the
 * wrapped stream, after the stream is created but before a terminal operation is called.  For example:
 * {@code
 *   AutoClosedLongStream<T> stream = new AutoClosedLongStream<>(...)
 *   int z = 4 / 0;
 *   stream.forEach(...);
 * }
 *
 * For this reason, using a try-finally block is still safer than using this class (since any exception within the block
 * will guarantee the resource, in this case a stream, is closed).  However, since clients may not be aware of the need
 * to close streams, and because a try-finally block can be cumbersome, {@link AutoClosingLongStream} offers an
 * alternative that will ensure the stream is closed in most real-world scenarios.
 *
 * Note that obtaining a {@link LongStream#iterator()} or {@link LongStream#spliterator()} from this stream will
 * <strong>not</strong> close it; doing so would be dangerous as the iterator may depend on a resource owned by the
 * stream.  Although the iterator has a {@link Object#finalize()} method that will cause the stream to close, this is
 * obviously at the whim of the garbage collector and not desirable; we consequently recommend against using this class
 * if either of these terminal operations will be used.
 */
public final class AutoClosingLongStream extends AbstractAutoClosingStream<Long, LongStream, AutoClosingLongStream>
    implements LongStream {
  /**
   * Creates a new instance that will wrap the provided stream if the provided stream is not already a
   * {@link AutoClosingLongStream}.  Otherwise, returns <code>wrappedStream</code>.
   */
  public static <T> AutoClosingLongStream wrap(LongStream wrappedStream) {
    return wrappedStream instanceof AutoClosingLongStream ? (AutoClosingLongStream) wrappedStream
        : new AutoClosingLongStream(wrappedStream);
  }

  /**
   * Creates a new instance that will wrap the provided stream.  All method calls on this stream will be forwarded to
   * the wrapped stream.
   *
   * @param wrappedStream the stream to wrap
   * @throws IllegalArgumentException if <code>wrappedStream</code> is already an {@link AutoClosingLongStream}
   */
  public AutoClosingLongStream(LongStream wrappedStream) {
    super(wrappedStream);
  }

  @Override
  public AutoClosingLongStream filter(LongPredicate predicate) {
    return setWrapped(() -> _wrapped.filter(predicate));
  }

  @Override
  public AutoClosingLongStream map(LongUnaryOperator mapper) {
    return setWrapped(() -> _wrapped.map(mapper));
  }

  @Override
  public <U> AutoClosingStream<U> mapToObj(LongFunction<? extends U> mapper) {
    return newStream(() -> _wrapped.mapToObj(mapper));
  }

  @Override
  public AutoClosingIntStream mapToInt(LongToIntFunction mapper) {
    return newIntStream(() -> _wrapped.mapToInt(mapper));
  }

  @Override
  public AutoClosingDoubleStream mapToDouble(LongToDoubleFunction mapper) {
    return newDoubleStream(() -> _wrapped.mapToDouble(mapper));
  }

  @Override
  public AutoClosingLongStream flatMap(LongFunction<? extends LongStream> mapper) {
    return setWrapped(() -> _wrapped.flatMap(mapper));
  }

  @Override
  public AutoClosingLongStream distinct() {
    return setWrapped(() -> _wrapped.distinct());
  }

  @Override
  public AutoClosingLongStream sorted() {
    return setWrapped(() -> _wrapped.sorted());
  }

  @Override
  public AutoClosingLongStream peek(LongConsumer action) {
    return setWrapped(() -> _wrapped.peek(action));
  }

  @Override
  public AutoClosingLongStream limit(long maxSize) {
    return setWrapped(() -> _wrapped.limit(maxSize));
  }

  @Override
  public AutoClosingLongStream skip(long n) {
    return setWrapped(() -> _wrapped.skip(n));
  }

  @Override
  public void forEach(LongConsumer action) {
    terminal(() -> _wrapped.forEach(action));
  }

  @Override
  public void forEachOrdered(LongConsumer action) {
    terminal(() -> _wrapped.forEachOrdered(action));
  }

  @Override
  public long[] toArray() {
    return terminal(_wrapped::toArray);
  }

  @Override
  public long reduce(long identity, LongBinaryOperator op) {
    return terminal(() -> _wrapped.reduce(identity, op));
  }

  @Override
  public OptionalLong reduce(LongBinaryOperator op) {
    return terminal(() -> _wrapped.reduce(op));
  }

  @Override
  public <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner) {
    return terminal(() -> _wrapped.collect(supplier, accumulator, combiner));
  }

  @Override
  public long sum() {
    return terminal(_wrapped::sum);
  }

  @Override
  public OptionalLong min() {
    return terminal(_wrapped::min);
  }

  @Override
  public OptionalLong max() {
    return terminal(_wrapped::max);
  }

  @Override
  public long count() {
    return terminal(_wrapped::count);
  }

  @Override
  public OptionalDouble average() {
    return terminal(_wrapped::average);
  }

  @Override
  public LongSummaryStatistics summaryStatistics() {
    return terminal(_wrapped::summaryStatistics);
  }

  @Override
  public boolean anyMatch(LongPredicate predicate) {
    return terminal(() -> _wrapped.anyMatch(predicate));
  }

  @Override
  public boolean allMatch(LongPredicate predicate) {
    return terminal(() -> _wrapped.allMatch(predicate));
  }

  @Override
  public boolean noneMatch(LongPredicate predicate) {
    return terminal(() -> _wrapped.noneMatch(predicate));
  }

  @Override
  public OptionalLong findFirst() {
    return terminal(_wrapped::findFirst);
  }

  @Override
  public OptionalLong findAny() {
    return terminal(_wrapped::findAny);
  }

  @Override
  public AutoClosingDoubleStream asDoubleStream() {
    return newDoubleStream(_wrapped::asDoubleStream);
  }

  @Override
  public AutoClosingStream<Long> boxed() {
    return newStream(_wrapped::boxed);
  }

  @Override
  public PrimitiveIterator.OfLong iterator() {
    PrimitiveIterator.OfLong iter = closeOnException(_wrapped::iterator);
    return new PrimitiveIterator.OfLong() {
      @Override
      public long nextLong() {
        return iter.nextLong();
      }

      @Override
      public boolean hasNext() {
        return iter.hasNext();
      }

      @Override
      protected void finalize() throws Throwable {
        super.finalize();
        _wrapped.close();
      }
    };
  }

  @Override
  public Spliterator.OfLong spliterator() {
    Spliterator.OfLong iter = closeOnException(_wrapped::spliterator);
    return new Spliterator.OfLong() {
      @Override
      public OfLong trySplit() {
        return iter.trySplit();
      }

      @Override
      public boolean tryAdvance(LongConsumer action) {
        return iter.tryAdvance(action);
      }

      @Override
      public long estimateSize() {
        return iter.estimateSize();
      }

      @Override
      public int characteristics() {
        return iter.characteristics();
      }

      @Override
      protected void finalize() throws Throwable {
        super.finalize();
        _wrapped.close();
      }
    };
  }
}
