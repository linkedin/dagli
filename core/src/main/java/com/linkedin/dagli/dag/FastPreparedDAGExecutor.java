package com.linkedin.dagli.dag;

import com.linkedin.dagli.generator.Generator;
import com.linkedin.dagli.objectio.biglist.BigListWriter;
import com.linkedin.dagli.objectio.ObjectIterator;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.objectio.ObjectWriter;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.PreparedTransformer;
import it.unimi.dsi.fastutil.objects.ObjectBigArrayBigList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;


/**
 * An executor designed for very efficient inference in a (prepared) DAG.  This executor cannot prepare preparable DAGs,
 * however.  Use {@link LocalDAGExecutor} if you want to combine the efficient inference of FastPreparedDAGExecutor and
 * the training of {@link MultithreadedDAGExecutor}.
 *
 * Note that a single example/row/input is always executed by {@link FastPreparedDAGExecutor} using one thread; that is,
 * inference for a single example is not multithreaded by the executor (of course, nodes in the DAG may still
 * multithread their processing, but this is independent of the executor).  This is almost always desirable.  However,
 * if you have a huge, expensive pipelined model, consider using {@link MultithreadedDAGExecutor}, which can execute
 * multiple nodes in the DAG at once.
 */
public final class FastPreparedDAGExecutor extends AbstractDAGExecutor<FastPreparedDAGExecutor> {
  private static final long serialVersionUID = 1L;

  /**
   * By default, the executor will create no more than one thread per this many examples/rows provided as input.
   */
  public static final int DEFAULT_MIN_INPUTS_PER_THREAD = 128;

  private int _maxThreads = 1;
  private int _minInputsPerThread = DEFAULT_MIN_INPUTS_PER_THREAD;
  private int _maxMinibatchSize = 1024;
  private boolean _useCommonPool = true;

  /**
   * Returns a copy that will either use the common thread pool, {@link ForkJoinPool#commonPool()}, or a new pool, when
   * performing multithreaded execution.
   *
   * By default, the common pool is used; this is usually the best option as it avoids the cost of creating new threads
   * and thread pools and helps avoid "excessive concurrency" (more threads than logical cores).
   *
   * @param useCommonPool whether or not the common pool should be used
   * @return a copy of this instance that will use the common pool or not depending on the provided flag
   */
  public FastPreparedDAGExecutor withCommonThreadPool(boolean useCommonPool) {
    return clone(c -> c._useCommonPool = useCommonPool);
  }

  /**
   * Returns a copy of this executor that will use no more than the specified maximum number of threads.
   *
   * The default maximum number of threads is 1.
   *
   * @param maxThreads the maximum number of threads to use
   * @return a copy of this executor with the specified maximum number of threads
   */
  public FastPreparedDAGExecutor withMaxThreads(int maxThreads) {
    return clone(c -> c._maxThreads = maxThreads);
  }

  /**
   * Returns a copy of this executor that will require the specified minimum number of examples per thread.  The
   * executor will create no more than one thread per this many examples provided as input.
   *
   * The default minimum number of examples per thread is 128.
   *
   * @param minInputsPerThread the minimum number of examples per thread used
   * @return a copy of this executor with the specified minimum examples per thread
   */
  public FastPreparedDAGExecutor withMinInputsPerThread(int minInputsPerThread) {
    return clone(c -> c._minInputsPerThread = minInputsPerThread);
  }

  /**
   * Returns a copy of this executor that will limit the minibatch size to be no more than the specified value.
   *
   * The minibatch size used is normally the maximum of the preferred minibatch sizes of all the prepared transformers,
   * but it will be constrained to be no more than this limit.  As this substantially affects the memory required by the
   * executor, setting a lower limit may be beneficial in some cases.
   *
   * The default limit is a (rather generous) value of 1024.
   *
   * @param maxMinibatchSize the maximum minibatch size that will be allowed (though the actual minibatch size used may
   *                         be as small as 1 regardless of this value)
   * @return a copy of this executor that will limit the minibatch size to be no more than the specified value
   */
  public FastPreparedDAGExecutor withMaxMinibatchSize(int maxMinibatchSize) {
    return clone(c -> c._maxMinibatchSize = maxMinibatchSize);
  }

  /**
   * Creates a new {@link FastPreparedDAGExecutor}.
   */
  public FastPreparedDAGExecutor() { }

  @Override
  protected <R, N extends PreparedDAGTransformer<R, N>, T extends PreparableDAGTransformer<R, N, T>> DAGExecutionResult<R, N> prepareAndApplyUnsafeImpl(
      T dag, ObjectReader<Object>[] inputValueLists) {
    throw new UnsupportedOperationException(
        "FastPreparedDAGExecutor cannot be used to prepare DAGs, only to apply already-prepared DAGs");
  }

  @Override
  protected <R, T extends PreparedDAGTransformer<R, T>> ObjectReader<?>[] applyUnsafeImpl(T dag,
      ObjectReader<Object>[] inputValueLists) {
    return executeUnsafeImpl(dag.internalAPI().getDAGStructure(), inputValueLists);
  }

  private <R> ObjectReader<?>[] executeUnsafeImpl(DAGStructure<R> dag, ObjectReader<Object>[] inputValueLists) {
    long count = inputValueLists[0].size64();
    Object[] executionStates = dag.createExecutionStateArray(count);
    final int minibatchSize = Math.max(1, Math.min(_maxMinibatchSize, dag._maxMinibatchSize));
    final int threadCount = (int) Math.min(_maxThreads, count / _minInputsPerThread);
    final ObjectIterator<Object>[] objectIterators =
        Arrays.stream(inputValueLists).map(ObjectReader::iterator).toArray(ObjectIterator[]::new);
    if (threadCount <= 1) {
      return executeUnsafeImplThread(dag, objectIterators, 0, count, minibatchSize, executionStates);
    } else {
      ForkJoinPool pool = _useCommonPool ? ForkJoinPool.commonPool() : new ForkJoinPool(threadCount);

      ArrayList<Callable<ObjectReader<Object>[]>> callables = new ArrayList<>(threadCount);
      long inputsPerThread = (count + threadCount - 1) / threadCount;
      for (int i = 0; i < threadCount; i++) {
        long offset = i * inputsPerThread;
        long runCount = (i == threadCount - 1) ? (count - i * inputsPerThread) : inputsPerThread;

        final BigListWriter<Object>[] inputValueAppendables =
            new BigListWriter[inputValueLists.length];
        for (int j = 0; j < inputValueAppendables.length; j++) {
          inputValueAppendables[j] = new BigListWriter<>(new ObjectBigArrayBigList<>(runCount));
          inputValueAppendables[j].write(objectIterators[j], runCount);
        }

        callables.add(() -> executeUnsafeImplThread(dag, Arrays.stream(inputValueAppendables)
                .map(ObjectWriter::createReader)
                .map(ObjectReader::iterator)
                .toArray(ObjectIterator[]::new), offset,
            runCount, minibatchSize, executionStates));
      }

      List<Future<ObjectReader<Object>[]>> futures = pool.invokeAll(callables);

      ObjectWriter<Object>[] resultLists = getEmptyResultList(dag, count);

      try {
        for (int i = 0; i < threadCount; i++) {
          ObjectReader<Object>[] partialResultLists = futures.get(i).get();
          for (int j = 0; j < resultLists.length; j++) {
            resultLists[j].write(partialResultLists[j].iterator(), partialResultLists[j].size64());
          }
        }
      } catch (InterruptedException | ExecutionException e) {
        // interrupts immediately end execution; there are no retries
        throw new RuntimeException(e);
      }

      return Arrays.stream(resultLists).map(ObjectWriter::createReader).toArray(ObjectReader[]::new);
    }
  }

  private static <R> BigListWriter<Object>[] getEmptyResultList(DAGStructure<R> dag, long count) {
    BigListWriter<Object>[] resLists = new BigListWriter[dag._outputs.size()];
    for (int i = 0; i < resLists.length; i++) {
      resLists[i] = new BigListWriter<>(new ObjectBigArrayBigList<>(count));
    }
    return resLists;
  }

  private static <R> ObjectReader<Object>[] executeUnsafeImplThread(DAGStructure<R> dag,
      ObjectIterator<Object>[] inputValueLists, long offset, long count, int minibatchSize, Object[] executionStates) {
    Object[][] argBuffers = new Object[dag._maxParentCount][minibatchSize]; // input position x minibatch index
    Object[][] resBuffer = new Object[dag._nodes.length][minibatchSize]; // node index x minibatch index

    BigListWriter<Object>[] resLists = getEmptyResultList(dag, count);

    for (long firstExampleIndex = offset; firstExampleIndex < offset + count; firstExampleIndex += minibatchSize) {
      int currentMinibatchSize = (int) Math.min(minibatchSize, offset + count - firstExampleIndex);

      for (int j = 0; j < inputValueLists.length; j++) {
        inputValueLists[j].next(resBuffer[j], 0, currentMinibatchSize);
      }

      apply(firstExampleIndex, currentMinibatchSize, dag, resBuffer, argBuffers, executionStates);

      for (int j = 0; j < dag._outputIndices.length; j++) {
        resLists[j].write(resBuffer[dag._outputIndices[j]], 0, currentMinibatchSize);
      }
    }

    // in general, getting the reader would not be safe without closing the writer, but it's safe for BigListWriters
    return Arrays.stream(resLists).map(ObjectWriter::createReader).toArray(ObjectReader[]::new);
  }

  protected static <R> void apply(long firstExampleIndex, int minibatchSize, DAGStructure<R> dag, Object[][] resultBuffer,
      Object[][] argBuffer, Object[] executionStates) {
    for (int i = dag._placeholders.size(); i < dag._nodes.length; i++) {
      Producer<?> node = dag._nodes[i];
      if (node instanceof Generator) {
        for (int exampleOffset = 0; exampleOffset < minibatchSize; exampleOffset++) {
          resultBuffer[i][exampleOffset] = ((Generator) node).generate(firstExampleIndex + exampleOffset);
        }
      } else if (node instanceof PreparedTransformer) {
        int[] parents = dag._parents[i];
        for (int j = 0; j < parents.length; j++) {
          argBuffer[j] = resultBuffer[parents[j]];
        }

        ((PreparedTransformer) node).internalAPI()
            .applyAllUnsafe(executionStates[i], minibatchSize, argBuffer, resultBuffer[i]);
      } else {
        throw new IllegalStateException("DAG is not prepared; this executor only accepts prepared DAGs");
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FastPreparedDAGExecutor that = (FastPreparedDAGExecutor) o;
    return _maxThreads == that._maxThreads && _minInputsPerThread == that._minInputsPerThread;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_maxThreads, _minInputsPerThread);
  }
}
