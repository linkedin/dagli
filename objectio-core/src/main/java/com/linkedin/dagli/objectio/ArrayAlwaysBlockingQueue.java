package com.linkedin.dagli.objectio;

import com.concurrentli.UncheckedInterruptedException;
import java.util.concurrent.ArrayBlockingQueue;


/**
 * ArrayBlockingQueue, despite its misleading name, will not actually block when excessive work is scheduled on a client
 * ThreadPoolExecutor (which calls offer rather than put).  This class, however, will always block; its offer method
 * thus always returns true.
 *
 * @param <T> the type of element to enqueue
 */
class ArrayAlwaysBlockingQueue<T> extends ArrayBlockingQueue<T> {
  /**
   * Creates a new instance with the specified capacity.
   *
   * @param capacity the capacity of the queue.
   */
  public ArrayAlwaysBlockingQueue(int capacity) {
    super(capacity);
  }

  @Override
  public boolean add(T item) {
    return offer(item);
  }

  @Override
  public boolean offer(T item) {
    try {
      put(item);
      return true;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new UncheckedInterruptedException(e);
    }
  }
}
