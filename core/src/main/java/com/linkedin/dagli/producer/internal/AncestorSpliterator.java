package com.linkedin.dagli.producer.internal;

import com.linkedin.dagli.producer.ChildProducer;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.util.collection.LinkedNode;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * This class is used internally by Dagli to provide streams of the ancestors of producers; the parents of each node
 * are retrieved through a constructor-provided function, which facilitates the navigation of graphs that do not rely on
 * the parents list provided by the producers themselves.
 *
 * This class is part of Dagli's internals and should not be used directly by client code.  Instead, use the public
 * API methods that themselves leverage this class, e.g. {@link ChildProducer#ancestors(ChildProducer, int)}.
 */
public class AncestorSpliterator implements Spliterator<LinkedNode<Producer<?>>> {
  private final ReferenceOpenHashSet<Producer<?>> _visited = new ReferenceOpenHashSet<>();
  private ArrayDeque<LinkedNode<Producer<?>>> _queue = new ArrayDeque<>();
  private ArrayDeque<LinkedNode<Producer<?>>> _nextQueue = new ArrayDeque<>();
  private final Function<? super ChildProducer<?>, ? extends List<? extends Producer<?>>> _parentsAccessor;
  private final int _maxDepth;
  int _depth = 0;

  /**
   * Enumerates the ancestors of specified leaf node, excluding that initial leaf node.
   *
   * @param producer the producer that is the starting point in the search and will <strong>not</strong> be included in
   *                 it
   * @param maxDepth the maximum depth to search
   * @param parentsAccessor a function that retrieves the parents for each producer
   */
  public AncestorSpliterator(ChildProducer<?> producer, int maxDepth,
      Function<? super ChildProducer<?>, ? extends List<? extends Producer<?>>> parentsAccessor) {
    _parentsAccessor = parentsAccessor;
    _maxDepth = maxDepth;

    LinkedNode<Producer<?>> startingNode = new LinkedNode<>(producer);
    _parentsAccessor.apply(producer).forEach(parent -> enqueue(startingNode, parent));
    swapQueues();
  }

  /**
   * Enumerates the ancestors of a list of starting nodes, including the starting nodes themselves.
   *
   * @param producers the producers that are the starting point in the search and will be included in it
   * @param maxDepth the maximum depth to search
   * @param parentsAccessor a function that retrieves the parents for each producer
   */
  public AncestorSpliterator(List<? extends Producer<?>> producers, int maxDepth,
      Function<? super ChildProducer<?>, ? extends List<? extends Producer<?>>> parentsAccessor) {
    _parentsAccessor = parentsAccessor;
    _maxDepth = maxDepth;

    for (Producer<?> producer : producers) {
      if (_visited.add(producer)) {
        _nextQueue.add(new LinkedNode<>(producer));
      }
    }

    swapQueues();
  }

  private void enqueue(LinkedNode<Producer<?>> parentNode, Producer<?> producer) {
    if (_visited.add(producer)) {
      _nextQueue.add(parentNode.add(producer));
    }
  }

  private void swapQueues() {
    // swap _queue and _nextQueue
    ArrayDeque<LinkedNode<Producer<?>>> tempQueue = _queue;
    _queue = _nextQueue;
    _nextQueue = tempQueue;
  }

  @Override
  public boolean tryAdvance(Consumer<? super LinkedNode<Producer<?>>> action) {
    if (_depth == _maxDepth) {
      return false;
    }

    if (_queue.isEmpty()) {
      if (_nextQueue.isEmpty()) {
        // ancestors exhausted
        _depth = _maxDepth;
        return false;
      }

      // we may have reached max depth--if so, stop now
      _depth++;
      if (_depth == _maxDepth) {
        return false;
      }

      swapQueues();
    }

    LinkedNode<Producer<?>> next = _queue.remove();

    if (next.getItem() instanceof ChildProducer) {
      _parentsAccessor.apply((ChildProducer<?>) next.getItem()).forEach(parent -> enqueue(next, parent));
    }

    action.accept(next);
    return true;
  }

  @Override
  public AncestorSpliterator trySplit() {
    return null;
  }

  @Override
  public long estimateSize() {
    return Long.MAX_VALUE;
  }

  @Override
  public int characteristics() {
    return ORDERED + NONNULL;
  }
}
