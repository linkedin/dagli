package com.linkedin.dagli.dag;

import com.concurrentli.AtomicWriteOnceReference;
import com.concurrentli.ExclusiveIdempotentMethod;
import com.concurrentli.UncheckedInterruptedException;
import com.concurrentli.UnsafeCircularIntegerBuffer;
import com.concurrentli.UnsafeCircularReferenceBuffer;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.generator.Generator;
import com.linkedin.dagli.objectio.ConcatenatedReader;
import com.linkedin.dagli.objectio.ObjectIterator;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.objectio.ObjectWriter;
import com.linkedin.dagli.preparer.Preparer;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerMode;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.preparer.PreparerResultMixed;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.PreparableTransformer;
import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.transformer.Transformer;
import com.linkedin.dagli.tuple.Tuple2;
import com.linkedin.dagli.view.TransformerView;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * The multithreaded DAG executor can be thought of as executing a DAG by doing the following:
 * (1) Split the input data into "batches" of constant size (except possibly the last batch, if you want to be pedantic)
 * (2) Multithreadedly process these batches across the DAG.  Parallelism includes both executing different nodes
 *     at once and executing the same node on multiple batches at once (where possible).
 * (3) {@link MultithreadedDAGExecutor} streams intermediate values between nodes rather than storing them
 *     (circular buffers are used for this purpose) where possible; when such values must be stored, the location is
 *     configurable, and may be heap (fastest, most memory intensive) or disk.
 *
 * {@link MultithreadedDAGExecutor} is generally used to train DAGs, and {@link FastPreparedDAGExecutor} used to do
 * inference, due to {@link FastPreparedDAGExecutor}'s lower overhead.  However, in rare cases where the DAG is large
 * and the nodes expensive to execute, it may be faster to execute the DAG via {@link MultithreadedDAGExecutor}.
 * Otherwise, use {@link LocalDAGExecutor}, which will perform training with {@link MultithreadedDAGExecutor} and
 * inference with {@link FastPreparedDAGExecutor} without having to manually specify.
 */
public class MultithreadedDAGExecutor extends AbstractDAGExecutor<MultithreadedDAGExecutor> implements DAGExecutor {
  private static final long serialVersionUID = 1L;

  private static final int DEFAULT_THREAD_COUNT = 2 * Runtime.getRuntime().availableProcessors();

  /**
   * The default batch size used by this executor.  Larger batches reduce the per-batch overhead, but also reduce
   * opportunities for parallelization.  In general, larger batches are better when execution of the DAG is inexpensive.
   */
  public static final int DEFAULT_BATCH_SIZE = 5000;

  /**
   * During execution, each (non-root) node in the DAG has a circular buffer that caches its inputs.  This is the
   * default maximum number of batches that will be held in that buffer (in RAM) for each node.  Higher values can
   * improve parallelization at the expense of RAM.
   * machine.
   */
  public static final int DEFAULT_MAX_CONCURRENT_BATCHES = DEFAULT_THREAD_COUNT;

  private final int _batchSize;
  private final int _maxConcurrentBatches;
  private final int _maxThreadCount;
  private final LocalStorage _localStorage;

  /**
   * Sets the batch size.  Larger batches reduce the per-batch overhead, but also reduce opportunities for
   * parallelization.  In general, larger batches are better when execution of the DAG is inexpensive.
   *
   * @param batchSize the batch size to set
   * @return a copy of this executor with the specified batch size
   */
  public MultithreadedDAGExecutor withBatchSize(int batchSize) {
    return new MultithreadedDAGExecutor(batchSize, _maxConcurrentBatches, _maxThreadCount, _localStorage);
  }

  /**
   * Sets the maximum number of concurrent batches to queue per node.  During execution, each (non-root) node in the DAG
   * has a circular buffer that caches its inputs (in RAM).  Higher values can improve parallelization at the expense of
   * memory utilization.  By default, this is twice the number of logical processors on the machine.
   *
   * @param maxConcurrentBatches sets the maximum number of concurrent batches to be used by the executor
   * @return a copy of this executor that will use the specified number of concurrent batches
   */
  public MultithreadedDAGExecutor withConcurrentBatches(int maxConcurrentBatches) {
    return new MultithreadedDAGExecutor(_batchSize, maxConcurrentBatches, _maxThreadCount, _localStorage);
  }

  /**
   * Sets the maximum number of threads used by the executor to process nodes in the DAG.  Note that, because values are
   * processed in batches, a single node will often be executed in parallel with itself (on different batches of
   * inputs).
   *
   * The default value is twice the number of logical processors on the machine.  This is because some nodes may, e.g.
   * block for IO or locks and thus a surplus of threads can help improve parallelism.
   *
   * Note that this is the number of threads used <b>by the executor</b>.  Individual nodes are free to (and often do)
   * implement their own multithreading, which may result in a greater number of total concurrent threads in your
   * program.
   *
   * @param maxThreadCount the maximum number of threads to use
   * @return a copy of this executor that will use the specified maximum number of threads
   */
  public MultithreadedDAGExecutor withMaxThreads(int maxThreadCount) {
    return new MultithreadedDAGExecutor(_batchSize, _maxConcurrentBatches, maxThreadCount, _localStorage);
  }

  /**
   * Sets the storage used by the executor to store intermediate values and results.
   *
   * Although {@link MultithreadedDAGExecutor} avoids storing values wherever possible (preferring to stream data
   * between nodes), in some cases--for example, when a preparable node must be prepared (trained) and then applied
   * (inference) to get the result--a node must see its inputs a second time, which requires that the values be stored
   * so they can be read back.
   *
   * Storing on the heap (in RAM) is of course the fastest, but may exhaust machine replaceholders when the amount of
   * intermediate values and outputs being stored is large.  For such problems, disk-backed storage (e.g. via Kryo) is
   * a more scalable solution.
   *
   * @param storage the storage method to use for intermediate and outputs that must be cached by the executor
   * @return a copy of this executor that will use the specified storage method
   */
  public MultithreadedDAGExecutor withStorage(LocalStorage storage) {
    return new MultithreadedDAGExecutor(_batchSize, _maxConcurrentBatches, _maxThreadCount, storage);
  }

  private LongFunction<ObjectWriter<Object>> getAppendableGenerator() {
    return _localStorage == null ? LocalStorage.MEMORY_HEAP._objectWriterGenerator
        : _localStorage._objectWriterGenerator;
  }

  /**
   * Creates a new executor with the default batch size, maximum concurrent batches to queue for each node, threads and
   * storage mechanism for intermediate and output values.
   */
  public MultithreadedDAGExecutor() {
    this(DEFAULT_BATCH_SIZE, DEFAULT_MAX_CONCURRENT_BATCHES, DEFAULT_THREAD_COUNT, LocalStorage.MEMORY_HEAP);
  }

  @Override
  public String toString() {
    return "MultithreadedDAGExecutor (batch size: " + _batchSize + "; concurrent batches: " + _maxConcurrentBatches
        + "; max threads: " + _maxThreadCount + ")";
  }

  private MultithreadedDAGExecutor(int batchSize, int maxConcurrentBatches, int maxThreadCount,
      LocalStorage storage) {
    _batchSize = batchSize;
    _maxConcurrentBatches = maxConcurrentBatches;
    _maxThreadCount = maxThreadCount;
    _localStorage = storage;
  }

  /**
   * How Scheduling Works
   *
   * 0. Nodes track the state of a given producer (input and output buffers) and schedule tasks
   * 1. Supplier tasks read values into buffers
   * 2. Generator tasks generate values into buffers
   * 3. Preparable tasks use inputs to prepare the transformer
   * 4. Prepared tasks transform their inputs to produce results
   *
   * If the result of a producer is an output or is fed to a later phase, it must be written to
   * a ObjectWriter so that it may be re-read in the future.
   */
  private static class Scheduler {
    private static final Logger LOGGER = LogManager.getLogger();

    public final ExecutorService _threadPool;
    public final MultithreadedDAGExecutor _executor;
    public final DAGStructure<?> _dag;
    public final long _count;
    public final int _batchSize;
    public final long _batchCount;

    private Object _threadExceptionMutex = new Object();
    private Exception _threadException = null;

    private static class ReducableSemaphore extends Semaphore {
      public ReducableSemaphore(int permits) {
        super(permits);
      }

      @Override
      public void reducePermits(int reduction) {
        super.reducePermits(reduction);
      }
    }

    public final ReducableSemaphore _pendingTaskSemaphore = new ReducableSemaphore(0);

    private final ObjectReader<Object>[] _outputResults;
    private volatile boolean _outputResultsMemoryBarrier;

    private volatile boolean _preparedMemoryBarrier;
    private final Producer<?>[] _preparedForNewDataProducers;
    private final Producer<?>[] _preparedForPreparationDataProducers;

    public Scheduler(ExecutorService threadPool, MultithreadedDAGExecutor executor, DAGStructure<?> dag, long count,
        boolean shouldApply) {
      _batchSize = executor._batchSize;
      _batchCount = Math.max(1, (count + _batchSize - 1) / _batchSize);
      _count = count;
      _dag = dag;
      _executor = executor;
      _threadPool = threadPool;

      _outputResults = shouldApply ? new ObjectReader[dag._outputIndices.length] : null;
      _preparedForNewDataProducers = new Producer<?>[dag._nodes.length];
      _preparedForPreparationDataProducers = new Producer<?>[dag._nodes.length];
    }

    /**
     * True if we want to produce outputs from the DAG, false if we only want to prepare it and don't care about the
     * output values.
     *
     * @return if we should produce outputs from the DAG
     */
    public boolean wantOutputs() {
      return _outputResults != null;
    }

    public void completedIterable(int nodeIndex, ObjectReader<Object> iterable) {
      if (wantOutputs()) {
        for (int i = 0; i < _dag._outputIndices.length; i++) {
          if (nodeIndex == _dag._outputIndices[i]) {
            _outputResults[i] = iterable;
            _outputResultsMemoryBarrier = true; // set purely to obtain memory barrier
          }
        }
      }
    }
    public ObjectReader<Object>[] getOutputResults() {
      boolean temp = _outputResultsMemoryBarrier; // synchronize
      return _outputResults;
    }

    public void setPrepared(int nodeIndex, Producer<?> preparedForNewData,
        Producer<?> preparedForPreparationData) {
      assert preparedForNewData != null;

      _preparedForNewDataProducers[nodeIndex] = preparedForNewData;
      _preparedForPreparationDataProducers[nodeIndex] = preparedForPreparationData;
      _preparedMemoryBarrier = true;
    }

    /**
     * Checks if a node can be copied directly from the DAG being prepared to the resulting prepared DAG.
     *
     * @param nodeIndex
     * @return
     */
    public boolean isNotIdentityPrepared(int nodeIndex) {
      return (_dag._phases[nodeIndex] > 0 && _dag._nodes[nodeIndex] instanceof Transformer)
          || _dag._nodes[nodeIndex] instanceof TransformerView;
    }
    public Producer<?> getPreparedForNewData(int nodeIndex) {
      if (isNotIdentityPrepared(nodeIndex)) {
        boolean temp = _preparedMemoryBarrier; // don't care about value, just memory barrier
        return _preparedForNewDataProducers[nodeIndex];
      } else {
        return _dag._nodes[nodeIndex];
      }
    }
    public Producer<?> getPreparedForPreparationData(int nodeIndex) {
      if (isNotIdentityPrepared(nodeIndex)) {
        boolean temp = _preparedMemoryBarrier; // don't care about value, just memory barrier
        return _preparedForPreparationDataProducers[nodeIndex];
      } else {
        return _dag._nodes[nodeIndex];
      }
    }
    public Producer<?>[] getPreparedForNewDataParents(int nodeIndex) {
      int[] parentIndices = _dag._parents[nodeIndex];
      Producer<?>[] result = new Producer[parentIndices.length];

      for (int i = 0; i < parentIndices.length; i++) {
        result[i] = getPreparedForNewData(parentIndices[i]);
        assert result[i] != null;
      }

      return result;
    }
    public Producer<?>[] getPreparedForPreparationDataParents(int nodeIndex) {
      int[] parentIndices = _dag._parents[nodeIndex];
      Producer<?>[] result = new Producer[parentIndices.length];

      for (int i = 0; i < parentIndices.length; i++) {
        result[i] = getPreparedForPreparationData(parentIndices[i]);
        assert result[i] != null;
      }

      return result;
    }

    public final void schedule(Task<?> task) {
      {
        _pendingTaskSemaphore.reducePermits(1);

        LOGGER.trace(() -> "Scheduling task " + task.toString() + " (node " + task._node._nodeIndex + ") for batch "
            + task._batchIndex + "; " + _pendingTaskSemaphore.availablePermits() + " permits available");
        assert _pendingTaskSemaphore.availablePermits() <= 0;
      }

      _threadPool.execute(() -> {
        try {
          LOGGER.trace(() -> "Starting task " + task.toString() + " (node " + task._node._nodeIndex + ") for batch "
              + task._batchIndex);
          task.run();
        } catch (Exception e) {
          synchronized (_threadExceptionMutex) {
            if (_threadException != null) {
              return; // only record first exception
            }
            _threadException = e;
          }

          LOGGER.trace(() -> "Exception thrown in task " + task.toString() + " (node " + task._node._nodeIndex
              + ") for batch " + task._batchIndex + "; " + _pendingTaskSemaphore.availablePermits()
              + " permits available: " + e.toString());

          _threadPool.shutdownNow(); // try to end ASAP
          _pendingTaskSemaphore.release(Integer.MAX_VALUE / 2); // unblock the waiting main thread ASAP
        } finally {
          assert _pendingTaskSemaphore.availablePermits() <= 0;
          _pendingTaskSemaphore.release();
          LOGGER.trace(() -> "Finished task " + task.toString() + " (node " + task._node._nodeIndex + ") for batch "
              + task._batchIndex + "; " + _pendingTaskSemaphore.availablePermits() + " permits available");
        }
      });
    }
  }

  private static abstract class Task<N extends Node<N>> implements Runnable {
    public final N _node;
    public final long _batchIndex;

    public Task(N node, long batchIndex) {
      assert batchIndex < node._scheduler._batchCount;
      assert batchIndex >= 0;

      _node = node;
      _batchIndex = batchIndex;
    }

    @Override
    public final void run() {
      onRun();
      _node.onTaskComplete(this);
    }

    protected abstract void onRun();
  }

  private static abstract class Node<S extends Node<S>> {
    private static final Logger LOGGER = LogManager.getLogger();

    protected abstract void onTaskComplete(Task<S> task);
    protected abstract void onOutputReleased(long batchIndex);

    //private final AtomicLong _nextReleasedBatchIndex = new AtomicLong(0);
    private final ExclusiveIdempotentMethod _sequentialOutputReleaser =
        new ExclusiveIdempotentMethod(this::releaseSequentialOutput);

    public final Scheduler _scheduler;
    public final int _nodeIndex;

    // used to, in effect, avoid keeping too much data in memory; default value is # of dependents
    public final UnsafeCircularIntegerBuffer _outputPendingCount;

    // Children are sorted by phase.  The first _samePhaseChildrenCount of these are in the same phase
    public final ChildNode<?>[] _children;

    // Keeps track of which input index to use for each child
    public final int[] _childInputIndices;

    // Keeps track of whether the node is constant-result
    public final boolean _isConstantResult;

    /**
     * The effective phase reflects when a node should run; except for PreparableTransformerNode this is the phase
     * of the corresponding Producer in the original DAG.  PreparableTransformerNode is actually able to run in the
     * previous phase (its corresponding PreparedTransformerNode, however, runs in its original phase)
     *
     * @return the effective phase of the node (when it should run)
     */
    protected int getEffectivePhase() {
      return _scheduler._dag._phases[_nodeIndex];
    }

    public final void sendOutput(long batchIndex, Object[] result) {
      assert batchIndex >= _outputPendingCount.getFirstElementIndex();
      assert batchIndex < _outputPendingCount.getFirstElementIndex() + _outputPendingCount.length();

      LOGGER.trace(() -> this.toString() + " (node " + _nodeIndex + ") sendOutput on batch " + batchIndex);
      for (int i = 0; i < _children.length; i++) {
        _children[i].acceptInput(this, _childInputIndices[i], batchIndex, result);
      }
    }

    private void releaseSequentialOutput() {
      long finishedBatchIndex;
      while ((finishedBatchIndex = _outputPendingCount.advanceIfEqual(0)) >= 0) {
        onOutputReleased(finishedBatchIndex);
      }
    }

    /**
     * Notifies a node that one of its children has finished with a batch of outputs provided by this node.
     * The purpose of this is to track when an output is no longer being held by any child and thus additional
     * outputs may be generated.
     *
     * @param batchIndex the batch the child is finished with
     * @return true if no more children are using this batch, false if some children still are
     */
    public boolean releaseOutput(long batchIndex) {
      LOGGER.trace(() -> this.toString() + " (node " + _nodeIndex + ") releaseOutput on batch " + batchIndex);
      assert _outputPendingCount.get(batchIndex) > 0;
      assert _children.length > 0;
      if (_outputPendingCount.getAndAdd(batchIndex, -1) == 1) {
        if (batchIndex == _outputPendingCount.getFirstElementIndex()) {
          _sequentialOutputReleaser.tryRun();
        }
        return true;
      }
      return false;
    }

    private static int indexOfNthMatch(int[] haystack, int sought, int n) {
      assert n >= 1;

      for (int i = 0; i < haystack.length; i++) {
        if (haystack[i] == sought && --n == 0) {
          return i;
        }
      }

      return -1;
    }

    public Node(Scheduler scheduler, int nodeIndex, ChildNode<?>[] children, boolean hasOtherDependents) {
      // a node must either be a leaf or have children
      assert children.length > 0 || scheduler._dag.isOutput(nodeIndex) || hasOtherDependents;

      _scheduler = scheduler;
      _isConstantResult = _scheduler._dag._nodes[nodeIndex].hasConstantResult();
      _nodeIndex = nodeIndex;
      _children = children;
      _childInputIndices = new int[_children.length];
      Int2IntOpenHashMap childCount = new Int2IntOpenHashMap(_children.length);
      for (int i = 0; i < _children.length; i++) {
        ChildNode<?> child = _children[i];
        if (child._parents.length == 1) {
          _childInputIndices[i] = 0;
        } else {
          int n = childCount.addTo(child._nodeIndex, 1) + 1;
          _childInputIndices[i] = indexOfNthMatch(_scheduler._dag._parents[child._nodeIndex], _nodeIndex, n);
        }
      }
      _outputPendingCount = new UnsafeCircularIntegerBuffer(_scheduler._executor._maxConcurrentBatches, children.length);
    }
  }

  public static <K, V> Map<K, List<V>> partition(V[] values, Function<V, K> keyForValue) {
    HashMap<K, List<V>> result = new HashMap<>();
    for (int i = 0; i < values.length; i++) {
      K key = keyForValue.apply(values[i]);
      List<V> list = result.computeIfAbsent(key, k -> new ArrayList<>());
      list.add(values[i]);
    }

    return result;
  }

  public static <T extends Node<?>> Map<Integer, List<T>> partitionByEffectivePhase(T[] nodes) {
    return partition(nodes, node -> node.getEffectivePhase());
  }

  private static void fillInputBuffer(Object[][] batches, Object[] buffer, int index) {
    for (int i = 0; i < buffer.length; i++) {
      buffer[i] = batches[i][index];
    }
  }

  public abstract static class ChildNode<S extends ChildNode<S>> extends Node<S> {
    private static final Logger LOGGER = LogManager.getLogger();

    public final UnsafeCircularIntegerBuffer _pendingInputCount;
    public final UnsafeCircularReferenceBuffer<Object[][]> _pendingInputs;
    public final AtomicWriteOnceReference<Node>[] _parents; // initially unknown; set when accepting inputs

    private final ExclusiveIdempotentMethod _sequentialInputDispatcher =
        new ExclusiveIdempotentMethod(this::dispatchSequentialInputs);

    public ChildNode(Scheduler scheduler, int nodeIndex, ChildNode<?>[] children, int parentCount, int prerequisites, boolean hasOtherDependents) {
      super(scheduler, nodeIndex, children, hasOtherDependents);
      _pendingInputCount = new UnsafeCircularIntegerBuffer(_scheduler._executor._maxConcurrentBatches, parentCount);
      _pendingInputs =
          new UnsafeCircularReferenceBuffer<>(_scheduler._executor._maxConcurrentBatches, () -> new Object[parentCount][]);
      _parents = new AtomicWriteOnceReference[parentCount];
      for (int i = 0; i < _parents.length; i++) {
        _parents[i] = new AtomicWriteOnceReference<>();
      }

      if (prerequisites > 0) {
        for (int i = 0; i < _scheduler._executor._maxConcurrentBatches; i++) {
          _pendingInputCount.getAndAdd(i, prerequisites);
        }
      }
    }

    private static final Object[][] DELETED_MARKER = new Object[0][0];

    protected abstract void onSequentialInput(long batchIndex, Object[][] inputBatches);
    protected abstract void onRandomInput(long batchIndex, Object[][] inputBatches);

    public void releaseParentsOutput(long batchIndex) {
      LOGGER.trace(() -> this.toString() + " (node " + _nodeIndex + ") releaseParentsOutput on batch " + batchIndex);
      for (int i = 0; i < _parents.length; i++) {
        _parents[i].get().releaseOutput(batchIndex);
      }
    }

    public boolean releaseOutput(long batchIndex) {
      if (super.releaseOutput(batchIndex)) {
        // finished with this batch; alert our parents
        releaseParentsOutput(batchIndex);
        return true;
      } else {
        // not finished...alert nobody
        return false;
      }
    }

    public void satisfiedPrerequisite() {
      for (int i = 0; i < _scheduler._executor._maxConcurrentBatches; i++) {
        updatePendingInput(i, _pendingInputs.get(i));
      }
    }

    public final void acceptInput(Node sender, int inputIndex, long batchIndex, Object[] result) {
      LOGGER.trace(() -> this.toString() + " (node " + _nodeIndex + ") acceptInput on batch " + batchIndex + ", input "
          + inputIndex + " from node " + sender._nodeIndex);

      Object[][] inputs = _pendingInputs.get(batchIndex);
      inputs[inputIndex] = result;
      _parents[inputIndex].trySet(sender);

      updatePendingInput(batchIndex, inputs);
    }

    private void dispatchSequentialInputs() {
      long finishedBatchIndex;
      while ((finishedBatchIndex = _pendingInputCount.advanceIfEqual(0)) >= 0) {
        Object[][] seqInputs = _pendingInputs.getAndSet(finishedBatchIndex, DELETED_MARKER);
        while (_pendingInputs.advanceIfReferenceEqual(DELETED_MARKER) >= 0) { }

        onSequentialInput(finishedBatchIndex, seqInputs);
      }
    }

    // update bookkeeping given that one of the requisite inputs for the batch is ready (it doesn't matter which--counts
    // are used) and then dispatch onSequentialInput and onRandomInput events when/if all inputs for a batch are
    // available.
    private void updatePendingInput(long batchIndex, Object[][] inputs) {
      assert _pendingInputCount.get(batchIndex) >= 1;

      if (_pendingInputCount.getAndAdd(batchIndex, -1) == 1) {
        if (_pendingInputCount.getFirstElementIndex() == batchIndex) {
          _sequentialInputDispatcher.tryRun();
        }
        onRandomInput(batchIndex, inputs);
      }
    }
  }

  private static class TransformTask extends Task<PreparedTransformerNode> {
    private final Object[][] _batch;
    public TransformTask(Object[][] batch, PreparedTransformerNode owner, long batchIndex) {
      super(owner, batchIndex);
      _batch = batch;
    }

    @Override
    protected void onRun() {
      Object[] results = new Object[_batch[0].length];
      Tuple2<PreparedTransformer<?>, Object> preparedAndExecutionCache =
          _node.getPreparedTransformerAndExecutionCache();

      preparedAndExecutionCache.get0()
          .internalAPI()
          .applyAllUnsafe(preparedAndExecutionCache.get1(), results.length, _batch, results);

      _node.sendOutput(_batchIndex, results);
    }
  }

  private static class PreparedTransformerNode extends ChildNode<PreparedTransformerNode> {
    // stores the prepared transformer and its execution cache object (if any)
    private final AtomicWriteOnceReference<Tuple2<PreparedTransformer<?>, Object>> _preparedTransformerAndExecutionCache = new AtomicWriteOnceReference<>();
    private final ReentrantLock _setPreparedLock = new ReentrantLock();

    // stores the constant results of this instance (if applicable)--an Object[] of the batch size used by this
    // scheduler is employed so that the same array may be reused
    private AtomicWriteOnceReference<Object[]> _constantResults = new AtomicWriteOnceReference<>();

    public PreparedTransformerNode(Scheduler scheduler, int nodeIndex, ChildNode<?>[] children) {
      super(scheduler, nodeIndex, children, scheduler._dag._parents[nodeIndex].length,
          isPreparable(scheduler._dag, nodeIndex) ? 1 : 0, false);

      // transformers in phase 0 (which are always prepared) have the same parents in the prepared DAG as they did
      // in the preparable DAG
      if (_scheduler._dag._phases[nodeIndex] == 0) {
        setPreparedTransformerAndExecutionCache((PreparedTransformer<?>) scheduler._dag._nodes[nodeIndex]);
      }
    }

    private void setPreparedTransformerAndExecutionCache(PreparedTransformer<?> prepared) {
      _preparedTransformerAndExecutionCache.set(
          Tuple2.of(prepared, prepared.internalAPI().createExecutionCache(_scheduler._count)));
    }

    public void setPreparedTransformer(PreparedTransformer<?> prepared) {
      assert isPreparable(_scheduler._dag, _nodeIndex);
      setPreparedTransformerAndExecutionCache(prepared);
      satisfiedPrerequisite();
    }

    public Tuple2<PreparedTransformer<?>, Object> getPreparedTransformerAndExecutionCache() {
      return _preparedTransformerAndExecutionCache.get();
    }

    @Override
    protected void onTaskComplete(Task<PreparedTransformerNode> task) { }

    @Override
    protected void onOutputReleased(long batchIndex) { }

    @Override
    protected void onSequentialInput(long batchIndex, Object[][] inputBatches) { }

    /**
     * Prepared transformers will (unless in phase 0) end up with new parents; we need to create new prepared
     * transformers like the original (but with the new parents) and tell the scheduler about them
     *
     * @param scheduler the scheduler to update
     * @param nodeIndex the index of the prepared transformer
     * @return the transformer with new parents, for new data
     */
    public static PreparedTransformer<?> setPreparedTransformerWithNewParentsOnScheduler(Scheduler scheduler, int nodeIndex) {
      assert !isPreparable(scheduler._dag, nodeIndex);

      if (scheduler._dag._phases[nodeIndex] == 0) {
        // in phase 0, prepared transformers are identity-prepared and don't require new instances with different
        // parents to be created; just return the original
        return (PreparedTransformer<?>) scheduler._dag._nodes[nodeIndex];
      }

      Producer<?>[] parentsForNewData = scheduler.getPreparedForNewDataParents(nodeIndex);
      PreparedTransformer<?> preparedForNewData =
          ((PreparedTransformer<?>) scheduler._dag._nodes[nodeIndex])
              .internalAPI()
              .withInputsUnsafe(Arrays.asList(parentsForNewData));

      Producer<?>[] parentsForPreparationData = scheduler.getPreparedForPreparationDataParents(nodeIndex);
      PreparedTransformer<?> preparedForPreparationData =
          ((PreparedTransformer<?>) scheduler._dag._nodes[nodeIndex])
              .internalAPI()
              .withInputsUnsafe(Arrays.asList(parentsForPreparationData));

      scheduler.setPrepared(nodeIndex, preparedForNewData, preparedForPreparationData);
      return preparedForNewData;
    }

    @Override
    protected void onRandomInput(long batchIndex, Object[][] inputBatches) {
      if (batchIndex < _scheduler._executor._maxConcurrentBatches && _preparedTransformerAndExecutionCache.get() == null) {
        // we know that batchIndex cannot be greater than maxConcurrentBatches until this method has successfully
        // returned at least once; this greatly reduces checks of the volatile
        assert !isPreparable(_scheduler._dag, _nodeIndex);
        try {
          _setPreparedLock.lock();
          if (_preparedTransformerAndExecutionCache.get() == null) {
            setPreparedTransformerAndExecutionCache(
                setPreparedTransformerWithNewParentsOnScheduler(_scheduler, _nodeIndex));
          }
        } finally {
          _setPreparedLock.unlock();
        }
      }

      // before processing any input, we need to be sure that our final prepared instantiation is available,
      // because child transformers will use this as their parent
      assert getPreparedTransformerAndExecutionCache() != null;
      assert inputBatches.length == _scheduler._dag._parents[_nodeIndex].length;

      if (_isConstantResult) {
        propagateConstantResult(batchIndex, inputBatches);
      } else {
        _scheduler.schedule(new TransformTask(inputBatches, this, batchIndex));
      }
    }

    private void propagateConstantResult(long batchIndex, Object[][] inputBatches) {
      // Can't be null at the time this method is called:
      Tuple2<PreparedTransformer<?>, Object> preparedAndExecutionCache = getPreparedTransformerAndExecutionCache();

      Object[] constantResults = _constantResults.get(); // might be null

      if (constantResults == null) {
        // run the transformer once, against the first example in the batch
        Object constantResult = preparedAndExecutionCache.get0()
            .internalAPI()
            .applyUnsafe(preparedAndExecutionCache.get1(), inputBatches, 0);

        // now create a default-batch-sized array of this value
        constantResults = new Object[_scheduler._batchSize];
        Arrays.fill(constantResults, constantResult);

        // save it for later
        _constantResults.trySet(constantResults);
      }

      // we might have to make a shorter-length copy of the constantResults array if our current batch is smaller than
      // normal (should only happen for the last batch)
      int currentBatchSize = inputBatches[0].length;
      sendOutput(batchIndex,
          currentBatchSize == constantResults.length ? constantResults : Arrays.copyOf(constantResults, currentBatchSize));
    }
  }

  private static class PreparationTask extends Task<PreparableTransformerNode> {
    private final Object[][] _batch;
    public PreparationTask(Object[][] batch, PreparableTransformerNode owner, long batchIndex) {
      super(owner, batchIndex);
      _batch = batch;
    }

    @Override
    protected void onRun() {
      Object[] buffer = new Object[_batch.length];

      for (int i = 0; i < _batch[0].length; i++) {
        fillInputBuffer(_batch, buffer, i);
        _node._preparer.processUnsafe(buffer);
      }
      if (_batchIndex == _node._scheduler._batchCount - 1) {
        _node.onReadyToFinish();
      }
    }
  }

  private static ChildNode<?>[] getNonViews(ChildNode<?>[] children) {
    return Arrays.stream(children).filter(c -> !(c instanceof TransformerViewNode)).toArray(ChildNode[]::new);
  }

  private static TransformerViewNode[] getViews(ChildNode<?>[] children) {
    return Arrays.stream(children).filter(c -> c instanceof TransformerViewNode).toArray(TransformerViewNode[]::new);
  }

  private static class PreparationFinishTask extends Task<PreparableTransformerNode> {

    public PreparationFinishTask(PreparableTransformerNode preparableNode) {
      super(preparableNode, preparableNode._scheduler._batchCount - 1);
    }

    @Override
    protected void onRun() {
      // for streamPrepared nodes, we can pass a null to finishUnsafe; otherwise, we need to pass a reader that can
      // make a second pass over the data
      PreparerResultMixed<? extends PreparedTransformer<?>, ? extends PreparedTransformer<?>> prepared =
          _node._preparer.finishUnsafe(
              _node.isStreamPrepared() ? null : new ConcatenatedReader<>(Object[]::new, _node._objectReaders));

      PreparedTransformer<?> preparedForPreparationData = prepared.getPreparedTransformerForPreparationData()
          .internalAPI()
          .withInputsUnsafe(Arrays.asList(_node._scheduler.getPreparedForPreparationDataParents(_node._nodeIndex)));

      PreparedTransformer<?> preparedForNewData = prepared.getPreparedTransformerForNewData()
          .internalAPI()
          .withInputsUnsafe(Arrays.asList(_node._scheduler.getPreparedForNewDataParents(_node._nodeIndex)));

      _node._scheduler.setPrepared(_node._nodeIndex, preparedForNewData, preparedForPreparationData);

      if (_node._preparedTransformerNode != null) {
        _node._preparedTransformerNode.setPreparedTransformer(preparedForPreparationData);
      }

      // schedule views (if any)
      for (TransformerViewNode view : _node._transformerViewNodes) {
        view.startPreparation();
      }
    }
  }

  private static class PreparableTransformerNode extends ChildNode<PreparableTransformerNode> {
    public final Preparer<?, ?> _preparer;
    private final ArrayDeque<Object[][]> _inputQueue;
    private final TransformerViewNode[] _transformerViewNodes;
    public final PreparedTransformerNode _preparedTransformerNode;

    private boolean _taskPending = false;
    private long _nextBatchIndex = 0;
    private ReentrantLock _schedulerLock = new ReentrantLock();

    private final AtomicInteger _outstandingFinishDependencies;

    // batch-prepared nodes will use these to make a second pass on the preparation data; for stream-prepared nodes,
    // this array will be null
    private final ObjectReader<?>[] _objectReaders;

    public void onObjectReaderReady(int parentIndex, ObjectReader<?> objectReader) {
      assert _objectReaders[parentIndex] == null;
      _objectReaders[parentIndex] = objectReader;
      finishDependencyResolved();
    }

    public void onReadyToFinish() {
      finishDependencyResolved();
    }

    private void finishDependencyResolved() {
      assert _outstandingFinishDependencies.get() > 0;

      if (_outstandingFinishDependencies.decrementAndGet() == 0) {
        _scheduler.schedule(new PreparationFinishTask(this));
      }
    }

    /**
     * Returns true if the preparable is "stream prepared".  Such preparers do not require ObjectReaders to be passed
     * to their finish() method.
     *
     * @return true iff the preparer is stream-prepared
     */
    public boolean isStreamPrepared() {
      return _preparer.getMode() == PreparerMode.STREAM;
    }

    public PreparableTransformerNode(Scheduler scheduler, int nodeIndex, ChildNode<?>[] nonViewChildren,
        TransformerViewNode[] views, PreparedTransformerNode preparedTransformerNode, boolean hasOtherDependents) {
      super(scheduler, nodeIndex, nonViewChildren, scheduler._dag._parents[nodeIndex].length, 0,
          views.length > 0 || hasOtherDependents);
      _inputQueue = new ArrayDeque<>(_scheduler._executor._maxConcurrentBatches);
      _preparedTransformerNode = preparedTransformerNode;
      _preparer = ((PreparableTransformer<?, ?>) _scheduler._dag._nodes[nodeIndex]).internalAPI().getPreparer(
          PreparerContext.builder(_scheduler._count).setExecutor(_scheduler._executor).build());
      _transformerViewNodes = views;

      if (isStreamPrepared()) {
        _outstandingFinishDependencies = new AtomicInteger(1);
        _objectReaders = null;
      } else {
        int parentCount = scheduler._dag._parents[nodeIndex].length;
        _outstandingFinishDependencies = new AtomicInteger(1 + parentCount);
        _objectReaders = new ObjectReader[parentCount];
      }

      assert _preparedTransformerNode != null || nonViewChildren.length == 0;
    }

    // PreparableTransformerNodes can run in the phase prior to when their resultant PreparedTransformerNodes run
    @Override
    protected int getEffectivePhase() {
      return _scheduler._dag._phases[_nodeIndex] - 1;
    }

    @Override
    protected void onTaskComplete(Task<PreparableTransformerNode> task) {
      if (task instanceof PreparationFinishTask) {
        return; // we're done
      }

      tryStartTask(null);

      // tell our parents that the input buffer we're consuming is now released (well, about to be, anyway)
      releaseParentsOutput(task._batchIndex);
    }

    private void tryStartTask(Object[][] newInputBatch) {
      Object[][] val;
      long batchIndex;
      try {
        _schedulerLock.lock();
        if (newInputBatch != null) {
          _inputQueue.add(newInputBatch);
        } else {
          _taskPending = false;
        }

        if (_taskPending || _inputQueue.isEmpty()) {
          return; // can't schedule now
        }
        val = _inputQueue.removeFirst();
        _taskPending = true;
        batchIndex = _nextBatchIndex++;
      } finally {
        _schedulerLock.unlock();
      }

      _scheduler.schedule(
          new PreparationTask(val, this, batchIndex));
    }

    @Override
    protected void onOutputReleased(long batchIndex) {
      throw new IllegalStateException("PreparableTransformerNode should never have its output released");
    }

    @Override
    protected void onSequentialInput(long batchIndex, Object[][] inputBatches) {
      assert inputBatches.length == _parents.length;

      tryStartTask(inputBatches);
    }

    @Override
    protected void onRandomInput(long batchIndex, Object[][] inputBatches) {
      // noop
    }
  }

  private static class BatchAppendTask extends Task<BatchAppendNode> {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Object[] _batch;
    public BatchAppendTask(Object[] batch, BatchAppendNode owner, long batchIndex) {
      super(owner, batchIndex);
      _batch = batch;
    }

    @Override
    protected void onRun() {
      LOGGER.trace(
          () -> "Starting append on " + this.toString() + " (node " + _node._nodeIndex + "), batch " + _batchIndex);
      _node._batchAppendable.write(_batch, 0, _batch.length);
      LOGGER.trace(
          () -> "Finished append on " + this.toString() + " (node " + _node._nodeIndex + "), batch " + _batchIndex);
    }
  }

  private static class BatchAppendNode extends ChildNode<BatchAppendNode> {
    public final ObjectWriter<Object> _batchAppendable;
    private final ArrayDeque<Object[]> _inputQueue;
    private final PreparableTransformerNode[] _uniqueSubscribers;

    public BatchAppendNode(Scheduler scheduler, int nodeIndex, ChildNode<?>[] children,
        PreparableTransformerNode[] subscribers, ObjectWriter<Object> batchAppendable) {
      super(scheduler, nodeIndex, children, 1, 0, subscribers.length > 0);
      assert children.length > 0 || subscribers.length > 0 || scheduler._dag.isOutput(nodeIndex);
      _inputQueue = new ArrayDeque<>(_scheduler._executor._maxConcurrentBatches);
      _batchAppendable = batchAppendable;
      _uniqueSubscribers = Arrays.stream(subscribers).distinct().toArray(PreparableTransformerNode[]::new);
    }

    private boolean _taskPending = false;
    private long _nextBatchIndex = 0;
    private final ReentrantLock _schedulerLock = new ReentrantLock();

    @Override
    protected void onTaskComplete(Task<BatchAppendNode> task) {
      tryStartTask(null);

      // tell our parents that the input buffer we're consuming is now released (well, about to be, anyway)
      releaseParentsOutput(task._batchIndex);

      if (task._batchIndex == _scheduler._batchCount - 1) {
        // stop writing and schedule child nodes
        _batchAppendable.close();
        ObjectReader<Object> reader = _batchAppendable.createReader();
        notifySubscribers(reader);

        if (_children.length > 0) {
          new ObjectReaderNode(_scheduler, _nodeIndex, _children, reader).start();
        } else {
          // make sure _scheduler.completedIterable(...) is called
          ObjectReaderNode.registerChildlessReader(_scheduler, _nodeIndex, reader);
        }
      }
    }

    private void notifySubscribers(ObjectReader<Object> reader) {
      for (PreparableTransformerNode sub : _uniqueSubscribers) {
        int[] childParents = _scheduler._dag._parents[sub._nodeIndex];
        for (int i = 0; i < childParents.length; i++) {
          if (childParents[i] == _nodeIndex) {
            sub.onObjectReaderReady(i, reader);
          }
        }
      }
    }

    private void tryStartTask(Object[] newInputBatch) {
      Object[] val;
      long batchIndex;
      try {
        _schedulerLock.lock();
        if (newInputBatch != null) {
          _inputQueue.add(newInputBatch);
        } else {
          _taskPending = false;
        }

        if (_taskPending || _inputQueue.isEmpty()) {
          return; // can't schedule now
        }
        val = _inputQueue.removeFirst();
        _taskPending = true;
        batchIndex = _nextBatchIndex++;
      } finally {
        _schedulerLock.unlock();
      }

      _scheduler.schedule(
          new BatchAppendTask(val, this, batchIndex));
    }

    @Override
    protected void onOutputReleased(long batchIndex) {
      throw new IllegalStateException("BatchAppendNode should never have its output released");
    }

    private final AtomicLong _sequentialTracker = new AtomicLong(0);

    @Override
    protected void onSequentialInput(long batchIndex, Object[][] inputBatches) {
      assert batchIndex == _sequentialTracker.getAndIncrement();
      assert inputBatches.length == 1;

      tryStartTask(inputBatches[0]);
    }

    @Override
    protected void onRandomInput(long batchIndex, Object[][] inputBatches) {
      // noop
    }
  }

  private static abstract class RootNode<S extends RootNode<S>> extends Node<S> {
    protected abstract void onStart();
    public final void start() {
      onStart();
    }

    public RootNode(Scheduler scheduler, int nodeIndex, ChildNode<?>[] children) {
      super(scheduler, nodeIndex, children, false);
    }
  }

  /**
   * Taskless node type that serves to generate the appropriate ObjectIteratorTasks when started
   */
  private static class ObjectReaderNode extends RootNode<ObjectReaderNode> {
    public final ObjectReader<Object> _objectReader;

    public ObjectReaderNode(Scheduler scheduler, int nodeIndex, ChildNode<?>[] children,
        ObjectReader<Object> objectReader) {
      super(scheduler, nodeIndex, children);
      _objectReader = objectReader;
    }

    /**
     * Used to register a completed reader with the scheduler when there are no immediate children to consume it.
     *
     * @param scheduler the scheduler
     * @param nodeIndex the index of the node creating the reader
     * @param objectReader the reader itself
     */
    public static void registerChildlessReader(Scheduler scheduler, int nodeIndex, ObjectReader<Object> objectReader) {
      scheduler.completedIterable(nodeIndex, objectReader);
    }

    @Override
    protected void onTaskComplete(Task<ObjectReaderNode> task) { }

    @Override
    protected void onOutputReleased(long batchIndex) { }

    @Override
    protected void onStart() {
      _scheduler.completedIterable(_nodeIndex, _objectReader);
      Map<Integer, List<ChildNode<?>>> phasedChildren = partitionByEffectivePhase(_children);
      for (Map.Entry<Integer, List<ChildNode<?>>> pair : phasedChildren.entrySet()) {
        ChildNode<?>[] children = pair.getValue().toArray(new ChildNode<?>[0]);
        new ObjectIteratorNode(_scheduler, _nodeIndex, children, _objectReader).start();
      }
    }
  }

  private static class ObjectIteratorTask extends Task<ObjectIteratorNode> {
    private final ObjectIterator<Object> _batchIterator;

    public ObjectIteratorTask(ObjectIterator<Object> batchIterator, ObjectIteratorNode node, long batchIndex) {
      super(node, batchIndex);
      _batchIterator = batchIterator;
    }

    @Override
    protected void onRun() {
      long remaining = _node._scheduler._count - _node._scheduler._batchSize * _batchIndex;
      int toRead = (int) Math.min(remaining, _node._scheduler._batchSize);
      Object[] batch = new Object[toRead];
      while (toRead > 0) {
        int lastRead = _batchIterator.next(batch, batch.length - toRead, toRead);
        if (lastRead == 0) {
          int originalToRead = (int) Math.min(remaining, _node._scheduler._batchSize);
          throw new IllegalStateException(
              "A ObjectIterator (a collection of values) was not as large as expected.  The executor tried to read "
                  + originalToRead
                  + " items for the current batch but was only able to read " + (originalToRead - toRead)
                  + " of them.  This most likely means the inputs (lists of values corresponding to the placeholders)"
                  + " provided to the DAG were not all of the same size");
        }
        toRead -= lastRead;
      }
      _node.sendOutput(_batchIndex, batch);
    }
  }

  private static class ObjectIteratorNode extends RootNode<ObjectIteratorNode> {
    private final ObjectIterator<Object> _placeholderData;

    public ObjectIteratorNode(Scheduler scheduler, int nodeIndex, ChildNode<?>[] children,
        ObjectReader<Object> placeholderData) {
      super(scheduler, nodeIndex, children);
      _placeholderData = placeholderData.iterator();
    }

    private long _nextBatchIndex = 0;
    private long _pendingOutputs = 0;
    private boolean _activeTask = false;
    private ReentrantLock _lock = new ReentrantLock();

    @Override
    public void onStart() {
      try {
        _lock.lock();
        long batchIndex = tryPermitSchedule();
        assert batchIndex >= 0;
        schedule(batchIndex);
      } finally {
        _lock.unlock();
      }
    }

    // must have lock when called
    private long tryPermitSchedule() {
      assert _lock.isHeldByCurrentThread();

      if (_pendingOutputs >= _outputPendingCount.length() || _activeTask || _nextBatchIndex >= _scheduler._batchCount) {
        return -1;
      }

      _activeTask = true;
      _pendingOutputs++;

      return _nextBatchIndex++;
    }

    private void schedule(long batchIndex) {
      _scheduler.schedule(new ObjectIteratorTask(_placeholderData, this, batchIndex));
    }

    @Override
    protected void onTaskComplete(Task<ObjectIteratorNode> task) {
      // if we finished the last task, close the iterator
      if (task._batchIndex == _scheduler._batchCount - 1) {
        _placeholderData.close();
      }

      long batchIndex;
      try {
        _lock.lock();
        _activeTask = false;
        batchIndex = tryPermitSchedule();
        if (batchIndex < 0) {
          return;
        }
      } finally {
        _lock.unlock();
      }
      schedule(batchIndex);
    }

    @Override
    protected void onOutputReleased(long releasedBatchIndex) {
      long batchIndex;
      try {
        _lock.lock();
        _pendingOutputs--;
        batchIndex = tryPermitSchedule();
        if (batchIndex < 0) {
          return;
        }
      } finally {
        _lock.unlock();
      }
      schedule(batchIndex);
    }
  }

  private static class GenerationTask extends Task<GeneratorNode> {
    public GenerationTask(GeneratorNode node, long batchIndex) {
      super(node, batchIndex);
    }

    @Override
    protected void onRun() {
      long batchOffset = _batchIndex * _node._scheduler._batchSize;
      int count = Math.toIntExact(Math.min(_node._scheduler._count - batchOffset, _node._scheduler._batchSize));
      Object[] res = new Object[count];
      for (int i = 0; i < count; i++) {
        res[i] = _node._generator.generate(batchOffset + i);
      }
      _node.sendOutput(_batchIndex, res);
    }
  }

  private static class GeneratorNode extends RootNode<GeneratorNode> {
    final Generator<?> _generator;


    public GeneratorNode(Scheduler scheduler, int nodeIndex, ChildNode<?>[] children) {
      super(scheduler, nodeIndex, children);
      _generator = (Generator<?>) scheduler._dag._nodes[nodeIndex];
    }

    @Override
    protected void onTaskComplete(Task task) { }

    @Override
    protected void onOutputReleased(long batchIndex) {
      long nextBatchIndex = batchIndex + _scheduler._executor._maxConcurrentBatches;
      if (nextBatchIndex < _scheduler._batchCount) {
        _scheduler.schedule(new GenerationTask(this, nextBatchIndex));
      }
    }

    @Override
    protected void onStart() {
      int limit = Math.toIntExact(Math.min(_scheduler._executor._maxConcurrentBatches, _scheduler._batchCount));
      for (int i = 0; i < limit; i++) {
        _scheduler.schedule(new GenerationTask(this, i));
      }
    }
  }

  private static class TransformerViewGenerationTask extends Task<TransformerViewNode> {
    private final Object _value;
    public TransformerViewGenerationTask(TransformerViewNode node, long batchIndex, Object value) {
      super(node, batchIndex);
      _value = value;
    }

    @Override
    protected void onRun() {
      long batchOffset = _batchIndex * _node._scheduler._batchSize;
      int count = Math.toIntExact(Math.min(_node._scheduler._count - batchOffset, _node._scheduler._batchSize));
      Object[] res = new Object[count];
      Arrays.fill(res, _value);
      _node.sendOutput(_batchIndex, res);
    }
  }

  private static class TransformerViewPreparationTask extends Task<TransformerViewNode> {
    public TransformerViewPreparationTask(TransformerViewNode node, long batchIndex) {
      super(node, batchIndex);
    }

    @Override
    protected void onRun() {
      int parentIndex = _node._scheduler._dag._parents[_node._nodeIndex][0];
      PreparedTransformer preparedForNewData = (PreparedTransformer) _node._scheduler.getPreparedForNewData(parentIndex);
      PreparedTransformer preparedForPreparationData = (PreparedTransformer) _node._scheduler.getPreparedForPreparationData(parentIndex);

      TransformerView view = (TransformerView) _node._scheduler._dag._nodes[_node._nodeIndex];
      Object valueForNewData = view.internalAPI().prepare(preparedForNewData);
      Object valueForPreparationData =
          view.internalAPI().prepareForPreparationData(preparedForPreparationData, preparedForNewData);

      _node.setValue(valueForPreparationData, valueForNewData);
    }
  }

  private static class TransformerViewNode extends ChildNode<TransformerViewNode> {
    final AtomicWriteOnceReference<Object> _value = new AtomicWriteOnceReference<>();

    public TransformerViewNode(Scheduler scheduler, int nodeIndex, ChildNode<?>[] children) {
      // parentCount == 0 because we don't depend on input values sent by the viewed PreparableTransformer
      // also, we pass hasOtherDependents as true since we may not have any children but the TransformerView still must
      // always be "prepared" and thus the prepared DAG itself is "dependent" on them
      super(scheduler, nodeIndex, children, 0, 0, true);
    }

    public void setValue(Object valueForPreparationData, Object valueForNewData) {
      _scheduler.setPrepared(_nodeIndex, new Constant<>(valueForNewData), new Constant<>(valueForPreparationData));
      _value.set(valueForPreparationData);
      if (_children.length > 0) {
        // ^ it's possible for us to have no children if we're being prepared without applying the prepared DAG to the
        // training data and we're in the last phase, in which case we should schedule any generation tasks
        int limit = Math.toIntExact(Math.min(_scheduler._executor._maxConcurrentBatches, _scheduler._batchCount));
        for (int i = 0; i < limit; i++) {
          _scheduler.schedule(new TransformerViewGenerationTask(this, i, _value.get()));
        }
      }
    }

    @Override
    protected void onTaskComplete(Task task) { }

    @Override
    protected void onOutputReleased(long batchIndex) {
      long nextBatchIndex = batchIndex + _scheduler._executor._maxConcurrentBatches;
      if (nextBatchIndex < _scheduler._batchCount) {
        _scheduler.schedule(new TransformerViewGenerationTask(this, nextBatchIndex, _value.get()));
      }
    }

    public void startPreparation() {
      TransformerViewPreparationTask prepTask = new TransformerViewPreparationTask(this, 0);
      _scheduler.schedule(prepTask);
    }

    @Override
    protected void onSequentialInput(long batchIndex, Object[][] inputBatches) {
      throw new IllegalStateException("TransformerViews should never receive input values");
    }

    @Override
    protected void onRandomInput(long batchIndex, Object[][] inputBatches) {
      throw new IllegalStateException("TransformerViews should never receive input values");
    }
  }

  private static boolean isPreparable(DAGStructure<?> dag, int nodeIndex) {
    return dag._nodes[nodeIndex] instanceof PreparableTransformer;
  }

  private static boolean isBatchPreparable(Node node) {
    return node instanceof PreparableTransformerNode
        && !((PreparableTransformerNode) node).isStreamPrepared();
  }

  private static void createNode(Node<?>[] earlyPhaseNodeArray,
      PreparedTransformerNode[] latePhaseNodeArray, int nodeIndex, Scheduler scheduler,
      ObjectReader<Object>[] inputValueLists,
      LongFunction<ObjectWriter<Object>> appendableGenerator, boolean shouldApply) {
    DAGStructure<?> dag = scheduler._dag;

    int phase = dag._phases[nodeIndex];
    int[] childIndices = dag._children[nodeIndex];

    // if our earliest child's phase is greater than 0, and we have only non-preparable children in that phase,
    // we can "upgrade" our phase to that earliest child's phase, which avoids unnecessary buffering of results of
    // generators
    if (childIndices.length > 0 && dag.isRoot(nodeIndex)) {
      int newPhase = dag._phases[childIndices[0]]; // possibly assume phase of first (lowest-phase) child
      for (int childIndex : childIndices) {
        if (dag._phases[childIndex] > newPhase) {
          break;
        }
        if (isPreparable(dag, childIndex)) {
          // preparable child; no upgrade possible
          newPhase = phase;
          break;
        }
      }
      phase = newPhase;
    }

    List<ChildNode<?>> directChildren = new ArrayList<>();
    List<ChildNode<?>> transitiveChildren = new ArrayList<>();

    for (int childIndex : childIndices) {
      if (!shouldApply && childIndex >= earlyPhaseNodeArray.length) {
        continue; // skip moot children
      }
      int childPhase = dag._phases[childIndex];
      if (childPhase == phase) {
        assert !isPreparable(dag, childIndex); // preparable children should not be in same phase!
        directChildren.add((ChildNode<?>) earlyPhaseNodeArray[childIndex]);
      } else if (childPhase == phase + 1 && isPreparable(dag, childIndex)) {
        // special case: preparable is in next stage, so it can be prepared as a direct child
        directChildren.add((ChildNode<?>) earlyPhaseNodeArray[childIndex]); // preparation
        if (latePhaseNodeArray[childIndex] != null) {
          transitiveChildren.add(latePhaseNodeArray[childIndex]); // prepared
        }
      } else {
        transitiveChildren.add((ChildNode<?>) earlyPhaseNodeArray[childIndex]);
        if (isPreparable(dag, childIndex) && latePhaseNodeArray[childIndex] != null) {
          transitiveChildren.add(latePhaseNodeArray[childIndex]);
        }
      }
    }

    // Views must be direct children of preparable nodes
    assert transitiveChildren.stream().noneMatch(n -> n instanceof TransformerViewNode);

    // placeholders are a special case, because their "results" are already cached in ObjectReaders
    if (nodeIndex < dag._placeholders.size()) {
      if (dag._children[nodeIndex].length > 0) {
        // superfluous placeholders are allowed in DAGs, but if they don't have children we don't need to schedule them
        directChildren.addAll(transitiveChildren);
        earlyPhaseNodeArray[nodeIndex] =
            new ObjectReaderNode(scheduler, nodeIndex, directChildren.toArray(new ChildNode[0]),
                inputValueLists[nodeIndex]);

        // make sure the Preparables that need the placeholder's ObjectReader have it available
        for (int i = 0; i < directChildren.size(); i++) {
          if (isBatchPreparable(directChildren.get(i))) {
            ((PreparableTransformerNode) directChildren.get(i)).onObjectReaderReady(
                earlyPhaseNodeArray[nodeIndex]._childInputIndices[i], inputValueLists[nodeIndex]);
          }
        }
      } else {
        scheduler.completedIterable(nodeIndex, inputValueLists[nodeIndex]);
      }
      return;
    }

    PreparableTransformerNode[] batchPreparableChildren = Stream.concat(transitiveChildren.stream(), directChildren.stream())
        .filter(MultithreadedDAGExecutor::isBatchPreparable)
        .toArray(PreparableTransformerNode[]::new);

    // if we have transitive children, or are a output, or have a batch-preparable direct child, we need to have a
    // BatchAppendNode child
    if (!transitiveChildren.isEmpty() || (shouldApply && dag.isOutput(nodeIndex))
        || batchPreparableChildren.length > 0) {
      // if not applying, nothing in the last phase should ever be buffered into a batch appendable:
      assert shouldApply || !dag.isLastPhase(nodeIndex);
      directChildren.add(
          new BatchAppendNode(scheduler, nodeIndex, transitiveChildren.toArray(new ChildNode[0]),
              batchPreparableChildren, appendableGenerator.apply(scheduler._count)));
    }

    Producer<?> producer = dag._nodes[nodeIndex];
    ChildNode<?>[] childrenArray = directChildren.toArray(new ChildNode[0]);
    if (producer instanceof Generator) {
      // generators with no effective children (because those children were last-phase and shouldApply is false) don't
      // need to be scheduled as they'd be no-ops
      if (childrenArray.length > 0) {
        earlyPhaseNodeArray[nodeIndex] = new GeneratorNode(scheduler, nodeIndex, childrenArray);
      }
    } else if (producer instanceof PreparedTransformer) {
      earlyPhaseNodeArray[nodeIndex] = new PreparedTransformerNode(scheduler, nodeIndex, childrenArray);
    } else if (producer instanceof PreparableTransformer) {
      ChildNode<?>[] nonViews = getNonViews(childrenArray);
      if (nonViews.length > 0) {
        // if we don't need to apply the DAG to the training data, we don't need to run the transformers prepared
        // from the preparables in the last phase and there should be no non-view children at this point:
        assert shouldApply || !dag.isLastPhase(nodeIndex);

        latePhaseNodeArray[nodeIndex] = new PreparedTransformerNode(scheduler, nodeIndex, nonViews);
      }
      earlyPhaseNodeArray[nodeIndex] =
          new PreparableTransformerNode(scheduler, nodeIndex, nonViews, getViews(childrenArray),
              latePhaseNodeArray[nodeIndex],
              !shouldApply && Arrays.stream(dag._children[nodeIndex]).anyMatch(dag::isLastPhase));
    } else if (producer instanceof TransformerView) {
      earlyPhaseNodeArray[nodeIndex] =
          new TransformerViewNode(scheduler, nodeIndex, childrenArray);
    } else {
      throw new IllegalArgumentException("Unknown producer type");
    }
  }

  @Override
  protected <R, N extends PreparedDAGTransformer<R, N>, T extends PreparableDAGTransformer<R, N, T>>
  DAGExecutionResult<R, N> prepareAndApplyUnsafeImpl(T dag, ObjectReader<Object>[] inputValueLists) {
    return executeUnsafe(dag, inputValueLists, true);
  }

  @Override
  protected <R, N extends PreparedDAGTransformer<R, N>, T extends PreparableDAGTransformer<R, N, T>>
  PreparerResult<N> prepareUnsafeImpl(
      T dag, ObjectReader<Object>[] inputValueLists) {
    return this.<R, N>executeUnsafe(dag, inputValueLists, false).getPreparerResult();
  }

  @Override
  protected <R, T extends PreparedDAGTransformer<R, T>> ObjectReader<?>[] applyUnsafeImpl(T dag,
      ObjectReader<Object>[] inputValueLists) {
    return executeUnsafe(dag, inputValueLists, true).getOutputs();
  }

  private <R, N extends PreparedDAGTransformer<R, N>> DAGExecutionResult<R, N> executeUnsafe(DAGTransformer<R, ?> dag,
      ObjectReader<Object>[] inputValueLists, boolean shouldApply) {

    long count = inputValueLists[0].size64();
    DAGStructure<R> dagStructure = dag.internalAPI().getDAGStructure();

    Scheduler scheduler = new Scheduler(Executors.newFixedThreadPool(_maxThreadCount), this, dagStructure, count, shouldApply);

    int effectiveNodeCount = shouldApply ? dagStructure._nodes.length : dagStructure.firstPreparedTransformerInPhase(dagStructure.getLastPhase());
    Node<?>[] earlyPhaseNodeArray = new Node<?>[effectiveNodeCount];
    PreparedTransformerNode[] latePhaseNodeArray = new PreparedTransformerNode[effectiveNodeCount];

    // build nodes from last in topographical order to first
    for (int i = earlyPhaseNodeArray.length - 1; i >= 0; i--) {
      createNode(earlyPhaseNodeArray, latePhaseNodeArray, i, scheduler, inputValueLists, getAppendableGenerator(),
          shouldApply);
    }

    // the game is afoot--start all root nodes
    for (int i = 0; i < dagStructure._placeholders.size() + dagStructure._generators.size(); i++) {
      assert dagStructure.isOutput(i) || ((dagStructure._children[i].length > 0 && dagStructure._children[i][0] < effectiveNodeCount) ^ (
          earlyPhaseNodeArray[i] == null));
      if (earlyPhaseNodeArray[i] != null) {
        ((RootNode<?>) earlyPhaseNodeArray[i]).start();
      }
    }

    // avoid holding references to the nodes--allow GC to collect them when/if possible
    earlyPhaseNodeArray = null;
    latePhaseNodeArray = null;

    try {
      // release one permit such that the total will be 1 (and acquirable) when all tasks have finished
      scheduler._pendingTaskSemaphore.release();

      // wait for all tasks to conclude
      scheduler._pendingTaskSemaphore.acquire();
      if (scheduler._threadException != null) {
        throw new RuntimeException("MultithreadedDAGExecutor terminated execution because it encountered an "
            + "unexpected exception in a worker thread: " + scheduler._threadException.toString(),
            scheduler._threadException);
      }

      // if !shouldApply, we ignored any prepared transformers in the final phase--we need to configure those now:
      for (int nodeIndex = effectiveNodeCount; nodeIndex < dagStructure._nodes.length; nodeIndex++) {
        PreparedTransformerNode.setPreparedTransformerWithNewParentsOnScheduler(scheduler, nodeIndex);
      }

      final PreparedTransformer<R> preparedForNewDataDAG;
      final PreparedTransformer<R> preparedForPreparationDataDAG;

      if (dag instanceof PreparedDAGTransformer) {
        preparedForNewDataDAG = (PreparedDAGTransformer<R, ?>) dag;
        preparedForPreparationDataDAG = (PreparedDAGTransformer<R, ?>) dag;
      } else {
        PreparableDAGTransformer<R, ?, ?> preparableDAG = (PreparableDAGTransformer<R, ?, ?>) dag;
        preparedForNewDataDAG = preparableDAG.internalAPI().createPreparedDAG(dagStructure._placeholders,
            Arrays.stream(dagStructure._outputIndices).mapToObj(scheduler::getPreparedForNewData).collect(Collectors.toList()));

        preparedForPreparationDataDAG = preparableDAG.internalAPI().createPreparedDAG(dagStructure._placeholders,
            Arrays.stream(dagStructure._outputIndices).mapToObj(scheduler::getPreparedForPreparationData).collect(Collectors.toList()));
      }

      assert shouldApply ? Arrays.stream(scheduler.getOutputResults()).noneMatch(Objects::isNull)
          : scheduler.getOutputResults() == null;

      return new DAGExecutionResult<R, N>(
          new PreparerResult.Builder<N>()
            .withTransformerForNewData((N) preparedForNewDataDAG)
            .withTransformerForPreparationData((N) preparedForPreparationDataDAG)
            .build(),
          scheduler.getOutputResults());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new UncheckedInterruptedException(e);
    } finally {
      // death to all threads
      scheduler._threadPool.shutdown();
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
    MultithreadedDAGExecutor that = (MultithreadedDAGExecutor) o;
    return _batchSize == that._batchSize && _maxConcurrentBatches == that._maxConcurrentBatches
        && _maxThreadCount == that._maxThreadCount && _localStorage == that._localStorage;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_batchSize, _maxConcurrentBatches, _maxThreadCount, _localStorage);
  }
}
