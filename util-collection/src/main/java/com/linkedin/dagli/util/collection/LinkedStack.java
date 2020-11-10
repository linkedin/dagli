package com.linkedin.dagli.util.collection;

import it.unimi.dsi.fastutil.Size64;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * An <strong>immutable</strong>, singly-linked list suitable for use as a stack (a last-in, first-out queue) or as
 * simply an efficiently "forkable" collection of items.
 *
 * No public constructor is provided; instead, use the {@link #empty()}, {@link #of(Object)}, {@link #of(Object[])} or
 * {@link #from(Iterable)} static methods to create your stack.
 *
 * Iteration order is the reverse of the order that items were added to the linked list (the last item added will be
 * the first in the iteration order).
 *
 * Pushing, popping and peeking at elements on the stack is constant-time.  Because this implementation uses a minimal
 * memory footprint, getting the size of the stack requires time linear in the number of elements.
 */
public class LinkedStack<T> implements Serializable, Iterable<T>, Size64 {
  private static final long serialVersionUID = 1;
  private static final LinkedStack<?> EMPTY = new LinkedStack<>();

  private final LinkedStack<T> _previous; // null if this is an empty stack
  private final T _item; // the item stored by this node; may be null (and will always be null if an empty stack)

  /**
   * Gets an empty stack.
   *
   * All empty stacks are equivalent, but no guarantee is made that all empty stacks will be the exact same object (as
   * determined by reference-equals).
   *
   * @param <T> the type of element that will be stored in the stack
   * @return an empty stack
   */
  @SuppressWarnings("unchecked")
  public static <T> LinkedStack<T> empty() {
    return (LinkedStack<T>) EMPTY;
  }

  /**
   * Convenience method that returns a stack containing the provided element.
   *
   * @param initialItem the element to place on the stack
   */
  public static <T> LinkedStack<T> of(T initialItem) {
    return new LinkedStack<>(initialItem);
  }

  /**
   * Convenience method that returns a stack containing the provided elements.
   *
   * @param initialItems the elements to place on the stack
   */
  @SafeVarargs
  public static <T> LinkedStack<T> of(T... initialItems) {
    return LinkedStack.from(Arrays.asList(initialItems));
  }

  /**
   * Convenience method that returns a stack containing the provided elements.
   *
   * @param initialItems the elements to place on the stack
   */
  public static <T> LinkedStack<T> from(Iterable<T> initialItems) {
    return LinkedStack.<T>empty().pushAll(initialItems);
  }

  /**
   * Creates a new, empty stack.
   */
  protected LinkedStack() {
    this(null, null);
  }

  /**
   * Creates a new stack containing a single element.
   *
   * @param initialItem the initial element
   */
  protected LinkedStack(T initialItem) {
    this(empty(), initialItem);
  }

  /**
   * Creates a new stack by appending an item to an existing stack.
   *
   * @param previous an existing stack from which this will be created by appending an item
   * @param item the item to add
   */
  protected LinkedStack(LinkedStack<T> previous, T item) {
    _previous = previous;
    _item = item;
  }

  private void assertNotEmpty() {
    if (_previous == null) {
      throw new EmptyStackException();
    }
  }

  /**
   * @return true if this stack is empty (containing no elements), false otherwise.
   */
  public boolean isEmpty() {
    return _previous == null;
  }

  /**
   * Pops and discards the most recently added element from the stack, returning the resulting stack omitting the popped
   * element.
   *
   * This stack is not modified.
   *
   * @return the stack resulting from popping an element from the end of this stack
   * @throws EmptyStackException if the stack is empty
   */
  public LinkedStack<T> pop() {
    assertNotEmpty();
    return _previous;
  }

  /**
   * Pops the most recently added element from the stack and passes it to the provided consumer method, returning the
   * resulting stack that omits the popped element.
   *
   * This stack is not modified.
   *
   * @param elementConsumer a consumer method that receives the popped element
   * @return the stack resulting from popping an element from the end of this stack
   * @throws EmptyStackException if the stack is empty
   */
  public LinkedStack<T> pop(Consumer<T> elementConsumer) {
    assertNotEmpty();
    elementConsumer.accept(_item);
    return _previous;
  }

  /**
   * @return the most-recently added element on the stack
   * @throws EmptyStackException if the stack is empty
   */
  public T peek() {
    assertNotEmpty();
    return _item;
  }

  /**
   * Returns a new stack containing the same elements as this stack, with the provided item appended to the end.
   *
   * This is a constant-time operation and does not modify this stack.
   *
   * @param item the item to add to the end of the stack
   * @return a new stack containing this stack's elements with the provided item appended to the end
   */
  public LinkedStack<T> push(T item) {
    return new LinkedStack<>(this, item);
  }

  /**
   * Returns a stack containing the same elements as this stack, with the provided items appended to the end.
   *
   * This operation does not modify this stack.
   *
   * @param items the items to add to the end of the stack
   * @return a stack containing this stack's elements with the provided items appended to the end
   */
  public LinkedStack<T> pushAll(Iterable<T> items) {
    LinkedStack<T> result = this;
    for (T item : items) {
      result = result.push(item);
    }
    return result;
  }

  @Override
  public Iterator<T> iterator() {
    return new Iterator<T>() {
      LinkedStack<T> _next = LinkedStack.this;

      @Override
      public boolean hasNext() {
        return _next._previous != null;
      }

      @Override
      public T next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }

        T result = _next._item;
        _next = _next._previous; // guaranteed not to be null
        return result;
      }
    };
  }

  /**
   * @return a stream that visits the items in the stack in its iteration order (the reverse of the order they were
   *         added).
   */
  public Stream<T> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  @Override
  public Spliterator<T> spliterator() {
    return Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED + Spliterator.IMMUTABLE);
  }

  @Override
  public int hashCode() {
    return Iterables.hashCodeOfOrderedElements(this) + LinkedStack.class.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof LinkedStack && Iterables.elementsAreEqual(this, (LinkedStack<?>) obj);
  }

  /**
   * Calculates the size of the linked stack.  <strong>The computational complexity of this operation is linear in the
   * size of the list.</strong>
   *
   * @return the size of the list
   */
  @Override
  public long size64() {
    LinkedStack<T> current = this;
    long count = 0;

    while (current._previous != null) {
      count++;
      current = current._previous;
    }

    return count;
  }

  /**
   * Creates an {@link ArrayList} of the items contained in this linked list, in the order they were added (note that
   * {@link #iterator()} and {@link #stream()} have a different order, providing items in the <strong>reverse</strong>
   * of the order they were added).
   *
   * An exception will be thrown if this list contains more items than can be stored in an {@link ArrayList}.
   *
   * @return an {@link ArrayList} of the items contained in this stack in the order in which they were added to
   *         the list
   */
  public ArrayList<T> toList() {
    ArrayList<T> result = new ArrayList<>();
    iterator().forEachRemaining(result::add);
    Collections.reverse(result);

    return result;
  }

  @Override
  public String toString() {
    return toList().toString();
  }
}
