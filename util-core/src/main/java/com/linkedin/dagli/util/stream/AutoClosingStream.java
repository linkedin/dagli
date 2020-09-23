package com.linkedin.dagli.util.stream;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;


/**
 * An {@link AutoClosingStream} wraps another {@link Stream} and calls {@link Stream#close()} on the wrapped stream when
 * a terminal operation is called (other than {@link Stream#iterator()} or {@link Stream#spliterator()}) or the
 * underlying stream throws an exception.  It does <strong>not</strong> close the stream via {@link Object#finalize()};
 * "dangling" streams for which a terminal operation is never invoked should thus be avoided.
 *
 * {@link AutoClosingStream} cannot clean up resources if an exception occurs outside the
 * wrapped stream, after the stream is created but before a terminal operation is called.  For example:
 * {@code
 *   AutoClosedStream<T> stream = new AutoClosedStream<>(...)
 *   int z = 4 / 0;
 *   stream.forEach(...);
 * }
 *
 * For this reason, using a try-finally block is still safer than using this class (since any exception within the block
 * will guarantee the resource, in this case a stream, is closed).  However, since clients may not be aware of the need
 * to close streams, and because a try-finally block can be cumbersome, {@link AutoClosingStream} offers an alternative
 * that will ensure the stream is closed in most real-world scenarios.
 *
 * Note that obtaining a {@link Stream#iterator()} or {@link Stream#spliterator()} from this stream will
 * <strong>not</strong> close it; doing so would be dangerous as the iterator may depend on a resource owned by the
 * stream.  Although the iterator has a {@link Object#finalize()} method that will cause the stream to close, this is
 * obviously at the whim of the garbage collector and not desirable; we consequently recommend against using this class
 * if either of these terminal operations will be used.
 */
public final class AutoClosingStream<T> extends AbstractAutoClosingStream<T, Stream<T>, AutoClosingStream<T>>
    implements Stream<T> {
  /**
   * Creates a new instance that will wrap the provided stream if the provided stream is not already a
   * {@link AutoClosingStream}.  Otherwise, returns <code>wrappedStream</code>.
   */
  public static <T> AutoClosingStream<T> wrap(Stream<T> wrappedStream) {
    return wrappedStream instanceof AutoClosingStream ? (AutoClosingStream<T>) wrappedStream
        : new AutoClosingStream<>(wrappedStream);
  }

  /**
   * Creates a new instance that will wrap the provided stream.  All method calls on this stream will be forwarded to
   * the wrapped stream.
   *
   * @param wrappedStream the stream to wrap
   * @throws IllegalArgumentException if <code>wrappedStream</code> is already an {@link AutoClosingStream}
   */
  public AutoClosingStream(Stream<T> wrappedStream) {
    super(wrappedStream);
  }

  @Override
  public AutoClosingStream<T> filter(Predicate<? super T> predicate) {
    return setWrapped(() -> _wrapped.filter(predicate));
  }

  @Override
  public <R> AutoClosingStream<R> map(Function<? super T, ? extends R> mapper) {
    return newStream(() -> _wrapped.map(mapper));
  }

  @Override
  public IntStream mapToInt(ToIntFunction<? super T> mapper) {
    return newIntStream(() -> _wrapped.mapToInt(mapper));
  }

  @Override
  public LongStream mapToLong(ToLongFunction<? super T> mapper) {
    return newLongStream(() -> _wrapped.mapToLong(mapper));
  }

  @Override
  public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
    return newDoubleStream(() -> _wrapped.mapToDouble(mapper));
  }

  @Override
  public <R> AutoClosingStream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
    return newStream(() -> _wrapped.flatMap(mapper));
  }

  @Override
  public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
    return newIntStream(() -> _wrapped.flatMapToInt(mapper));
  }

  @Override
  public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
    return newLongStream(() -> _wrapped.flatMapToLong(mapper));
  }

  @Override
  public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
    return newDoubleStream(() -> _wrapped.flatMapToDouble(mapper));
  }

  @Override
  public AutoClosingStream<T> distinct() {
    return setWrapped(() -> _wrapped.distinct());
  }

  @Override
  public AutoClosingStream<T> sorted() {
    return setWrapped(() -> _wrapped.sorted());
  }

  @Override
  public AutoClosingStream<T> sorted(Comparator<? super T> comparator) {
    return setWrapped(() -> _wrapped.sorted(comparator));
  }

  @Override
  public AutoClosingStream<T> peek(Consumer<? super T> action) {
    return setWrapped(() -> _wrapped.peek(action));
  }

  @Override
  public AutoClosingStream<T> limit(long maxSize) {
    return setWrapped(() -> _wrapped.limit(maxSize));
  }

  @Override
  public AutoClosingStream<T> skip(long n) {
    return setWrapped(() -> _wrapped.skip(n));
  }

  @Override
  public void forEach(Consumer<? super T> action) {
    terminal(() -> _wrapped.forEach(action));
  }

  @Override
  public void forEachOrdered(Consumer<? super T> action) {
    terminal(() -> _wrapped.forEachOrdered(action));
  }

  @Override
  public Object[] toArray() {
    return terminal(() -> _wrapped.toArray());
  }

  @Override
  public <A> A[] toArray(IntFunction<A[]> generator) {
    return terminal(() -> _wrapped.toArray(generator));
  }

  @Override
  public T reduce(T identity, BinaryOperator<T> accumulator) {
    return terminal(() -> _wrapped.reduce(identity, accumulator));
  }

  @Override
  public Optional<T> reduce(BinaryOperator<T> accumulator) {
    return terminal(() -> _wrapped.reduce(accumulator));
  }

  @Override
  public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
    return terminal(() -> _wrapped.reduce(identity, accumulator, combiner));
  }

  @Override
  public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
    return terminal(() -> _wrapped.collect(supplier, accumulator, combiner));
  }

  @Override
  public <R, A> R collect(Collector<? super T, A, R> collector) {
    return terminal(() -> _wrapped.collect(collector));
  }

  @Override
  public Optional<T> min(Comparator<? super T> comparator) {
    return terminal(() -> _wrapped.min(comparator));
  }

  @Override
  public Optional<T> max(Comparator<? super T> comparator) {
    return terminal(() -> _wrapped.max(comparator));
  }

  @Override
  public long count() {
    return terminal(_wrapped::count);
  }

  @Override
  public boolean anyMatch(Predicate<? super T> predicate) {
    return terminal(() -> _wrapped.anyMatch(predicate));
  }

  @Override
  public boolean allMatch(Predicate<? super T> predicate) {
    return terminal(() -> _wrapped.allMatch(predicate));
  }

  @Override
  public boolean noneMatch(Predicate<? super T> predicate) {
    return terminal(() -> _wrapped.noneMatch(predicate));
  }

  @Override
  public Optional<T> findFirst() {
    return terminal(_wrapped::findFirst);
  }

  @Override
  public Optional<T> findAny() {
    return terminal(_wrapped::findAny);
  }

  @Override
  public Iterator<T> iterator() {
    Iterator<T> iter = closeOnException(_wrapped::iterator);
    return new Iterator<T>() {
      @Override
      public T next() {
        return iter.next();
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
  public Spliterator<T> spliterator() {
    Spliterator<T> iter = closeOnException(_wrapped::spliterator);
    return new Spliterator<T>() {
      @Override
      public boolean tryAdvance(Consumer<? super T> action) {
        return iter.tryAdvance(action);
      }

      @Override
      public Spliterator<T> trySplit() {
        return iter.trySplit();
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