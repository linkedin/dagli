package com.linkedin.dagli.objectio;

import it.unimi.dsi.fastutil.BigList;
import java.util.Collection;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;


/**
 * Base class for an {@link ObjectReader} that redirects all operations to its wrapped instance.
 */
public abstract class WrappedObjectReader<T> implements ObjectReader<T> {
  protected final ObjectReader<T> _wrapped;

  /**
   * Creates a new instance that will wrap the provided reader.
   *
   * @param wrapped the reader wrapped by this instance
   */
  public WrappedObjectReader(ObjectReader<? extends T> wrapped) {
    _wrapped = ObjectReader.cast(wrapped);
  }

  @Override
  public long size64() {
    return _wrapped.size64();
  }

  @Override
  public ObjectIterator<T> iterator() {
    return _wrapped.iterator();
  }

  @Override
  public void forEach(Consumer<? super T> action) {
    _wrapped.forEach(action);
  }

  @Override
  public void forEachBatch(int batchSize, Consumer<? super List<? extends T>> action) {
    _wrapped.forEachBatch(batchSize, action);
  }

  @Override
  public void close() {
    _wrapped.close();
  }

  @Override
  public Stream<List<T>> batchStream(int batchSize) {
    return _wrapped.batchStream(batchSize);
  }

  @Override
  public Stream<T> stream() {
    return _wrapped.stream();
  }

  @Override
  public Collection<T> toCollection() {
    return _wrapped.toCollection();
  }

  @Override
  public ObjectReader<T> sample(SampleSegment segment) {
    return _wrapped.sample(segment);
  }

  @Override
  public ObjectReader<T> sample(double segmentRangeStartInclusive, double segmentRangeEndExclusive, long seed) {
    return _wrapped.sample(segmentRangeStartInclusive, segmentRangeEndExclusive, seed);
  }

  @Override
  public ObjectReader<T> sample(double segmentRangeStartInclusive, double segmentRangeEndExclusive) {
    return _wrapped.sample(segmentRangeStartInclusive, segmentRangeEndExclusive);
  }

  @Override
  public <U> ObjectReader<U> lazyMap(Function<T, U> mapper) {
    return _wrapped.lazyMap(mapper);
  }

  @Override
  public <U> ObjectReader<U> lazyFlatMap(Function<T, Iterable<? extends U>> mapper) {
    return _wrapped.lazyFlatMap(mapper);
  }

  @Override
  public ObjectReader<T> lazyFilter(Predicate<T> inclusionTest) {
    return _wrapped.lazyFilter(inclusionTest);
  }

  @Override
  public ObjectReader<T> lazyShuffle(long seed, int bufferSize) {
    return _wrapped.lazyShuffle(seed, bufferSize);
  }

  @Override
  public ObjectReader<T> lazyShuffle(int bufferSize) {
    return _wrapped.lazyShuffle(bufferSize);
  }

  @Override
  public Spliterator<T> spliterator() {
    return _wrapped.spliterator();
  }

  @Override
  public int hashCode() {
    return _wrapped.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof WrappedObjectReader && ((WrappedObjectReader<?>) obj)._wrapped.equals(this._wrapped);
  }

  @Override
  public String toString() {
    return _wrapped.toString();
  }

  @Override
  public List<T> toList() {
    return _wrapped.toList();
  }

  @Override
  public BigList<T> toBigList() {
    return _wrapped.toBigList();
  }
}
