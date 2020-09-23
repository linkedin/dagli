package com.linkedin.dagli.util.stream;

import com.linkedin.dagli.util.closeable.Closeables;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;


/**
 * Wraps another {@link Spliterator} to provider spliteration of the items of the wrapped spliterator grouped into
 * "batches" of a desired size.
 *
 * @param <T> the type of element to be batch-spliterated
 */
public class BatchSpliterator<T> implements Spliterator<List<T>>, AutoCloseable {
  private final Spliterator<T> _wrapped;
  private final int _batchSize;

  /**
   * Creates a new {@link BatchSpliterator} that wraps the given underlying spliterator, batching its values into lists
   * of a desired {@code batchSize}.
   *
   * Due to exhaustion of elements (when the underlying spliterator ran out of elements in its current split), the
   * iterated lists may be smaller than the desired batch size.
   *
   * @param batchSize the desired (and maximum) size of the spliterated lists of values from the underlying spliterator;
   *                  the value lists may be smaller if the spliterator exhausts its elements (in its current split)
   * @param wrapped the wrapped spliterator
   */
  public BatchSpliterator(int batchSize, Spliterator<T> wrapped) {
    _batchSize = batchSize;
    _wrapped = wrapped;
  }

  @Override
  public boolean tryAdvance(Consumer<? super List<T>> action) {
    ArrayList<T> next = new ArrayList<>(_batchSize);
    for (int i = 0; i < _batchSize; i++) {
      if (!_wrapped.tryAdvance(next::add)) {
        if (next.isEmpty()) {
          return false;
        }
        break;
      }
    }

    action.accept(next);
    return true;
  }

  @Override
  public Spliterator<List<T>> trySplit() {
    if (_wrapped.estimateSize() <= _batchSize) {
      // don't split
      return null;
    }

    Spliterator<T> wrappedSplit = _wrapped.trySplit();
    return wrappedSplit == null ? null : new BatchSpliterator<>(_batchSize, wrappedSplit);
  }

  @Override
  public long estimateSize() {
    long estimate = _wrapped.estimateSize();
    if (estimate == Long.MAX_VALUE) {
      return Long.MAX_VALUE;
    }

    // calculate number of batches remaining
    return (_wrapped.estimateSize() + _batchSize - 1) / _batchSize;
  }

  @Override
  public int characteristics() {
    // the size is unknowable because it will vary depending on how the splits are performed
    return characteristics(_wrapped.characteristics());
  }

  /**
   * Gets what the characteristics for an instance of this class would be if it was wrapping a spliterator with the
   * given characteristics
   *
   * @param wrappedCharacteristics the characteristics of the hypothetically wrapped spliterator
   * @return the characteristics that the {@link BatchSpliterator} would have if it wrapped a spliterator with the given
   *         characteristics
   */
  public static int characteristics(int wrappedCharacteristics) {
    return (wrappedCharacteristics & ~SIZED & ~SUBSIZED & ~SORTED) | NONNULL;
  }

  @Override
  public void close() {
    Closeables.tryClose(_wrapped);
  }
}
