package com.linkedin.dagli.util.stream;

import java.util.IntSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;


/**
 * An {@link AutoClosingIntStream} wraps another {@link IntStream} and calls {@link IntStream#close()} on the wrapped
 * stream when a terminal operation is called (other than {@link IntStream#iterator()} or
 * {@link IntStream#spliterator()} or the underlying stream throws an exception.  It does <strong>not</strong> close the
 * stream via {@link Object#finalize()}; "dangling" streams for which a terminal operation is never invoked should thus
 * be avoided.
 *
 * {@link AutoClosingIntStream} cannot clean up resources if an exception occurs outside the
 * wrapped stream, after the stream is created but before a terminal operation is called.  For example:
 * {@code
 *   AutoClosedIntStream<T> stream = new AutoClosedIntStream<>(...)
 *   int z = 4 / 0;
 *   stream.forEach(...);
 * }
 *
 * For this reason, using a try-finally block is still safer than using this class (since any exception within the block
 * will guarantee the resource, in this case a stream, is closed).  However, since clients may not be aware of the need
 * to close streams, and because a try-finally block can be cumbersome, {@link AutoClosingIntStream} offers an
 * alternative that will ensure the stream is closed in most real-world scenarios.
 *
 * Note that obtaining a {@link IntStream#iterator()} or {@link IntStream#spliterator()} from this stream will
 * <strong>not</strong> close it; doing so would be dangerous as the iterator may depend on a resource owned by the
 * stream.  Although the iterator has a {@link Object#finalize()} method that will cause the stream to close, this is
 * obviously at the whim of the garbage collector and not desirable; we consequently recommend against using this class
 * if either of these terminal operations will be used.
 */
public final class AutoClosingIntStream extends AbstractAutoClosingStream<Integer, IntStream, AutoClosingIntStream>
    implements IntStream {
  /**
   * Creates a new instance that will wrap the provided stream if the provided stream is not already a
   * {@link AutoClosingIntStream}.  Otherwise, returns <code>wrappedStream</code>.
   */
  public static <T> AutoClosingIntStream wrap(IntStream wrappedStream) {
    return wrappedStream instanceof AutoClosingIntStream ? (AutoClosingIntStream) wrappedStream
        : new AutoClosingIntStream(wrappedStream);
  }

  /**
   * Creates a new instance that will wrap the provided stream.  All method calls on this stream will be forwarded to
   * the wrapped stream.
   *
   * @param wrappedStream the stream to wrap
   * @throws IllegalArgumentException if <code>wrappedStream</code> is already an {@link AutoClosingIntStream}
   */
  public AutoClosingIntStream(IntStream wrappedStream) {
    super(wrappedStream);
  }

  @Override
  public AutoClosingIntStream filter(IntPredicate predicate) {
    return setWrapped(() -> _wrapped.filter(predicate));
  }

  @Override
  public AutoClosingIntStream map(IntUnaryOperator mapper) {
    return setWrapped(() -> _wrapped.map(mapper));
  }

  @Override
  public <U> AutoClosingStream<U> mapToObj(IntFunction<? extends U> mapper) {
    return newStream(() -> _wrapped.mapToObj(mapper));
  }

  @Override
  public AutoClosingLongStream mapToLong(IntToLongFunction mapper) {
    return newLongStream(() -> _wrapped.mapToLong(mapper));
  }

  @Override
  public AutoClosingDoubleStream mapToDouble(IntToDoubleFunction mapper) {
    return newDoubleStream(() -> _wrapped.mapToDouble(mapper));
  }

  @Override
  public AutoClosingIntStream flatMap(IntFunction<? extends IntStream> mapper) {
    return setWrapped(() -> _wrapped.flatMap(mapper));
  }

  @Override
  public AutoClosingIntStream distinct() {
    return setWrapped(() -> _wrapped.distinct());
  }

  @Override
  public AutoClosingIntStream sorted() {
    return setWrapped(() -> _wrapped.sorted());
  }

  @Override
  public AutoClosingIntStream peek(IntConsumer action) {
    return setWrapped(() -> _wrapped.peek(action));
  }

  @Override
  public AutoClosingIntStream limit(long maxSize) {
    return setWrapped(() -> _wrapped.limit(maxSize));
  }

  @Override
  public AutoClosingIntStream skip(long n) {
    return setWrapped(() -> _wrapped.skip(n));
  }

  @Override
  public void forEach(IntConsumer action) {
    terminal(() -> _wrapped.forEach(action));
  }

  @Override
  public void forEachOrdered(IntConsumer action) {
    terminal(() -> _wrapped.forEachOrdered(action));
  }

  @Override
  public int[] toArray() {
    return terminal(_wrapped::toArray);
  }

  @Override
  public int reduce(int identity, IntBinaryOperator op) {
    return terminal(() -> _wrapped.reduce(identity, op));
  }

  @Override
  public OptionalInt reduce(IntBinaryOperator op) {
    return terminal(() -> _wrapped.reduce(op));
  }

  @Override
  public <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner) {
    return terminal(() -> _wrapped.collect(supplier, accumulator, combiner));
  }

  @Override
  public int sum() {
    return terminal(_wrapped::sum);
  }

  @Override
  public OptionalInt min() {
    return terminal(_wrapped::min);
  }

  @Override
  public OptionalInt max() {
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
  public IntSummaryStatistics summaryStatistics() {
    return terminal(_wrapped::summaryStatistics);
  }

  @Override
  public boolean anyMatch(IntPredicate predicate) {
    return terminal(() -> _wrapped.anyMatch(predicate));
  }

  @Override
  public boolean allMatch(IntPredicate predicate) {
    return terminal(() -> _wrapped.allMatch(predicate));
  }

  @Override
  public boolean noneMatch(IntPredicate predicate) {
    return terminal(() -> _wrapped.noneMatch(predicate));
  }

  @Override
  public OptionalInt findFirst() {
    return terminal(_wrapped::findFirst);
  }

  @Override
  public OptionalInt findAny() {
    return terminal(_wrapped::findAny);
  }

  @Override
  public AutoClosingLongStream asLongStream() {
    return newLongStream(_wrapped::asLongStream);
  }

  @Override
  public AutoClosingDoubleStream asDoubleStream() {
    return newDoubleStream(_wrapped::asDoubleStream);
  }

  @Override
  public AutoClosingStream<Integer> boxed() {
    return newStream(_wrapped::boxed);
  }

  @Override
  public PrimitiveIterator.OfInt iterator() {
    PrimitiveIterator.OfInt iter = closeOnException(_wrapped::iterator);
    return new PrimitiveIterator.OfInt() {
      @Override
      public int nextInt() {
        return iter.nextInt();
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
  public Spliterator.OfInt spliterator() {
    Spliterator.OfInt iter = closeOnException(_wrapped::spliterator);
    return new Spliterator.OfInt() {
      @Override
      public OfInt trySplit() {
        return iter.trySplit();
      }

      @Override
      public boolean tryAdvance(IntConsumer action) {
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
