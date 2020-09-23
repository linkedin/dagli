package com.linkedin.dagli.dag;

import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.preparer.PreparerResult;
import java.util.Objects;


/**
 * {@link LocalDAGExecutor} is a user-friendly way to execute a DAG on the local machine in the best way possible, using
 * either FastPreparedDAGExecutor or MultithreadedDAGExecutor behind the scenes (the former is preferred for prepared
 * DAGs).
 */
public class LocalDAGExecutor extends AbstractDAGExecutor<LocalDAGExecutor> implements DAGExecutor {
  private static final int DEFAULT_THREAD_COUNT = 2 * Runtime.getRuntime().availableProcessors();
  private static final int DEFAULT_MIN_INPUTS_PER_THREAD = 128;

  private static final long serialVersionUID = 1L;

  private final MultithreadedDAGExecutor _multithreadedDAGExecutor;
  private final FastPreparedDAGExecutor _fastPreparedDAGExecutor;

  /**
   * The maximum number of threads that will be used by the DAG executor.
   * The default value is twice the number of logical CPU cores.
   *
   * @param maxThreads the maximum number of threads used by the executor
   * @return a copy of this instance that uses the specified number of threads
   */
  public LocalDAGExecutor withMaxThreads(int maxThreads) {
    return new LocalDAGExecutor(_multithreadedDAGExecutor.withMaxThreads(maxThreads),
        _fastPreparedDAGExecutor.withMaxThreads(maxThreads));
  }

  public LocalDAGExecutor withBatchSize(int batchSize) {
    return new LocalDAGExecutor(_multithreadedDAGExecutor.withBatchSize(batchSize), _fastPreparedDAGExecutor);
  }

  public LocalDAGExecutor withConcurrentBatches(int maxConcurrentBatches) {
    return new LocalDAGExecutor(_multithreadedDAGExecutor.withConcurrentBatches(maxConcurrentBatches),
        _fastPreparedDAGExecutor);
  }

  public LocalDAGExecutor withStorage(LocalStorage storage) {
    return new LocalDAGExecutor(_multithreadedDAGExecutor.withStorage(storage), _fastPreparedDAGExecutor);
  }

  public LocalDAGExecutor() {
    this(new MultithreadedDAGExecutor()
            .withBatchSize(MultithreadedDAGExecutor.DEFAULT_BATCH_SIZE)
            .withConcurrentBatches(MultithreadedDAGExecutor.DEFAULT_MAX_CONCURRENT_BATCHES)
            .withMaxThreads(DEFAULT_THREAD_COUNT),
         new FastPreparedDAGExecutor()
             .withMinInputsPerThread(DEFAULT_MIN_INPUTS_PER_THREAD)
            .withMaxThreads(DEFAULT_THREAD_COUNT));
  }

  private LocalDAGExecutor(MultithreadedDAGExecutor mtde, FastPreparedDAGExecutor fpde) {
    _multithreadedDAGExecutor = Objects.requireNonNull(mtde);
    _fastPreparedDAGExecutor = Objects.requireNonNull(fpde);
  }

  @Override
  protected <R, N extends PreparedDAGTransformer<R, N>, T extends PreparableDAGTransformer<R, N, T>> DAGExecutionResult<R, N> prepareAndApplyUnsafeImpl(
      T dag, ObjectReader<Object>[] inputValueLists) {
    return _multithreadedDAGExecutor.prepareAndApplyUnsafeImpl(dag, inputValueLists);
  }

  @Override
  protected <R, N extends PreparedDAGTransformer<R, N>, T extends PreparableDAGTransformer<R, N, T>> PreparerResult<N>
  prepareUnsafeImpl(T dag, ObjectReader<Object>[] inputValueLists) {
    return _multithreadedDAGExecutor.prepareUnsafeImpl(dag, inputValueLists);
  }

  @Override
  protected <R, T extends PreparedDAGTransformer<R, T>> ObjectReader<?>[] applyUnsafeImpl(T dag,
      ObjectReader<Object>[] inputValueLists) {
    return _fastPreparedDAGExecutor.applyUnsafeImpl(dag, inputValueLists);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LocalDAGExecutor that = (LocalDAGExecutor) o;
    return _multithreadedDAGExecutor.equals(that._multithreadedDAGExecutor) && _fastPreparedDAGExecutor.equals(
        that._fastPreparedDAGExecutor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_multithreadedDAGExecutor, _fastPreparedDAGExecutor);
  }
}
