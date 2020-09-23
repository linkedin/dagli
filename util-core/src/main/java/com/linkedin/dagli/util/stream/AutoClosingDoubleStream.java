package com.linkedin.dagli.util.stream;

import java.util.DoubleSummaryStatistics;
import java.util.OptionalDouble;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;


/**
 * An {@link AutoClosingDoubleStream} wraps another {@link DoubleStream} and calls {@link DoubleStream#close()} on the wrapped
 * stream when a terminal operation is called (other than {@link DoubleStream#iterator()} or
 * {@link DoubleStream#spliterator()} or the underlying stream throws an exception.  It does <strong>not</strong> close the
 * stream via {@link Object#finalize()}; "dangling" streams for which a terminal operation is never invoked should thus
 * be avoided.
 *
 * {@link AutoClosingDoubleStream} cannot clean up resources if an exception occurs outside the
 * wrapped stream, after the stream is created but before a terminal operation is called.  For example:
 * {@code
 *   AutoClosedDoubleStream<T> stream = new AutoClosedDoubleStream<>(...)
 *   int z = 4 / 0;
 *   stream.forEach(...);
 * }
 *
 * For this reason, using a try-finally block is still safer than using this class (since any exception within the block
 * will guarantee the resource, in this case a stream, is closed).  However, since clients may not be aware of the need
 * to close streams, and because a try-finally block can be cumbersome, {@link AutoClosingDoubleStream} offers an
 * alternative that will ensure the stream is closed in most real-world scenarios.
 *
 * Note that obtaining a {@link DoubleStream#iterator()} or {@link DoubleStream#spliterator()} from this stream will
 * <strong>not</strong> close it; doing so would be dangerous as the iterator may depend on a resource owned by the
 * stream.  Although the iterator has a {@link Object#finalize()} method that will cause the stream to close, this is
 * obviously at the whim of the garbage collector and not desirable; we consequently recommend against using this class
 * if either of these terminal operations will be used.
 */
public final class AutoClosingDoubleStream extends AbstractAutoClosingStream<Double, DoubleStream, AutoClosingDoubleStream>
    implements DoubleStream {
  /**
   * Creates a new instance that will wrap the provided stream if the provided stream is not already a
   * {@link AutoClosingDoubleStream}.  Otherwise, returns <code>wrappedStream</code>.
   */
  public static <T> AutoClosingDoubleStream wrap(DoubleStream wrappedStream) {
    return wrappedStream instanceof AutoClosingDoubleStream ? (AutoClosingDoubleStream) wrappedStream
        : new AutoClosingDoubleStream(wrappedStream);
  }

  /**
   * Creates a new instance that will wrap the provided stream.  All method calls on this stream will be forwarded to
   * the wrapped stream.
   *
   * @param wrappedStream the stream to wrap
   * @throws IllegalArgumentException if <code>wrappedStream</code> is already an {@link AutoClosingDoubleStream}
   */
  public AutoClosingDoubleStream(DoubleStream wrappedStream) {
    super(wrappedStream);
  }

  @Override
  public AutoClosingDoubleStream filter(DoublePredicate predicate) {
    return setWrapped(() -> _wrapped.filter(predicate));
  }

  @Override
  public AutoClosingDoubleStream map(DoubleUnaryOperator mapper) {
    return setWrapped(() -> _wrapped.map(mapper));
  }

  @Override
  public <U> AutoClosingStream<U> mapToObj(DoubleFunction<? extends U> mapper) {
    return newStream(() -> _wrapped.mapToObj(mapper));
  }

  @Override
  public AutoClosingLongStream mapToLong(DoubleToLongFunction mapper) {
    return newLongStream(() -> _wrapped.mapToLong(mapper));
  }

  @Override
  public AutoClosingIntStream mapToInt(DoubleToIntFunction mapper) {
    return newIntStream(() -> _wrapped.mapToInt(mapper));
  }

  @Override
  public AutoClosingDoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper) {
    return setWrapped(() -> _wrapped.flatMap(mapper));
  }

  @Override
  public AutoClosingDoubleStream distinct() {
    return setWrapped(() -> _wrapped.distinct());
  }

  @Override
  public AutoClosingDoubleStream sorted() {
    return setWrapped(() -> _wrapped.sorted());
  }

  @Override
  public AutoClosingDoubleStream peek(DoubleConsumer action) {
    return setWrapped(() -> _wrapped.peek(action));
  }

  @Override
  public AutoClosingDoubleStream limit(long maxSize) {
    return setWrapped(() -> _wrapped.limit(maxSize));
  }

  @Override
  public AutoClosingDoubleStream skip(long n) {
    return setWrapped(() -> _wrapped.skip(n));
  }

  @Override
  public void forEach(DoubleConsumer action) {
    terminal(() -> _wrapped.forEach(action));
  }

  @Override
  public void forEachOrdered(DoubleConsumer action) {
    terminal(() -> _wrapped.forEachOrdered(action));
  }

  @Override
  public double[] toArray() {
    return terminal(_wrapped::toArray);
  }

  @Override
  public double reduce(double identity, DoubleBinaryOperator op) {
    return terminal(() -> _wrapped.reduce(identity, op));
  }

  @Override
  public OptionalDouble reduce(DoubleBinaryOperator op) {
    return terminal(() -> _wrapped.reduce(op));
  }

  @Override
  public <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
    return terminal(() -> _wrapped.collect(supplier, accumulator, combiner));
  }

  @Override
  public double sum() {
    return terminal(_wrapped::sum);
  }

  @Override
  public OptionalDouble min() {
    return terminal(_wrapped::min);
  }

  @Override
  public OptionalDouble max() {
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
  public DoubleSummaryStatistics summaryStatistics() {
    return terminal(_wrapped::summaryStatistics);
  }

  @Override
  public boolean anyMatch(DoublePredicate predicate) {
    return terminal(() -> _wrapped.anyMatch(predicate));
  }

  @Override
  public boolean allMatch(DoublePredicate predicate) {
    return terminal(() -> _wrapped.allMatch(predicate));
  }

  @Override
  public boolean noneMatch(DoublePredicate predicate) {
    return terminal(() -> _wrapped.noneMatch(predicate));
  }

  @Override
  public OptionalDouble findFirst() {
    return terminal(_wrapped::findFirst);
  }

  @Override
  public OptionalDouble findAny() {
    return terminal(_wrapped::findAny);
  }

  @Override
  public AutoClosingStream<Double> boxed() {
    return newStream(_wrapped::boxed);
  }

  @Override
  public PrimitiveIterator.OfDouble iterator() {
    PrimitiveIterator.OfDouble iter = closeOnException(_wrapped::iterator);
    return new PrimitiveIterator.OfDouble() {
      @Override
      public double nextDouble() {
        return iter.nextDouble();
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
  public Spliterator.OfDouble spliterator() {
    Spliterator.OfDouble iter = closeOnException(_wrapped::spliterator);
    return new Spliterator.OfDouble() {
      @Override
      public OfDouble trySplit() {
        return iter.trySplit();
      }

      @Override
      public boolean tryAdvance(DoubleConsumer action) {
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
