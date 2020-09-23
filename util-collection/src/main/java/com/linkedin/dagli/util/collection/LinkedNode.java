package com.linkedin.dagli.util.collection;

import it.unimi.dsi.fastutil.Size64;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * Lightweight data structure capable of holding an arbitrary number of objects as a singly-linked list.
 *
 * Each {@link LinkedNode} is immutable, but the emergent linked list is not as items can be added to the end of any
 * existing list within an additional linked node pointing to the previous list.  {@link LinkedNode} are especially
 * useful when you wish to efficiently many lists that share the same first elements.
 *
 * Iteration order is the reverse of the order that items were added to the linked list (the last item added will be
 * the first in the iteration order).
 *
 * @param <T> the type of item stored by this linked node
 */
public final class LinkedNode<T> implements Serializable, Iterable<T>, Size64 {
  private static final long serialVersionUID = 1;

  private final LinkedNode<T> _previous; // null for the first node in the list
  private final T _item; // the item stored by this node; may be null

  /**
   * Creates a new instance with no previous node and the provided item (a linked list of size 1).
   *
   * @param initialItem the initial item
   */
  public LinkedNode(T initialItem) {
    this(null, initialItem);
  }

  /**
   * Creates a new instance with the provided previous node and item to add to the linked list.
   *
   * @param previous the previous node
   * @param item the item to add
   */
  private LinkedNode(LinkedNode<T> previous, T item) {
    _previous = previous;
    _item = item;
  }

  /**
   * @return the previous node in the linked list, or null if this is the first node in the list
   */
  public LinkedNode<T> getPreviousNode() {
    return _previous;
  }

  /**
   * @return the item stored by this node
   */
  public T getItem() {
    return _item;
  }

  /**
   * Adds the given item to the linked list that ends with this node, and returns a new node corresponding to the
   * new, expanded list.  This is a constant-time operation.
   *
   * @param item the item to add to the end of the linked list corresponding to this node
   * @return a new node corresponding to the new, expanded list
   */
  public LinkedNode<T> add(T item) {
    return new LinkedNode<>(this, item);
  }

  @Override
  public Iterator<T> iterator() {
    return new Iterator<T>() {
      LinkedNode<T> _next = LinkedNode.this;

      @Override
      public boolean hasNext() {
        return _next != null;
      }

      @Override
      public T next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }

        T result = _next._item;
        _next = _next._previous;
        return result;
      }
    };
  }

  /**
   * @return a stream that visited the items in the linked list ending at this node in its iteration order (the reverse
   *         of the order they were added).
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
    return Iterables.hashCodeOfOrderedElements(this) + LinkedNode.class.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof LinkedNode && Iterables.elementsAreEqual(this, (LinkedNode<?>) obj);
  }

  /**
   * Calculates the size of the linked list that terminates at this node.  <strong>The computational complexity of this
   * operation is linear in the size of the list.</strong>
   *
   * @return the size of the list
   */
  @Override
  public long size64() {
    LinkedNode<T> current = this;
    long count = 0;

    do {
      count++;
      current = current._previous;
    } while (current != null);

    return count;
  }

  /**
   * Creates an {@link ArrayList} of the items contained in this linked list, in the order they were added (note that
   * {@link #iterator()} and {@link #stream()} have a different order, providing items in the <strong>reverse</strong>
   * of the order they were added.
   *
   * An exception will be thrown if this list contains more items than can be stored in an {@link ArrayList}.
   *
   * @return an {@link ArrayList} of the items contained in this linked list in the order in which they were added to
   *         the list
   */
  public ArrayList<T> toArrayList() {
    ArrayList<T> result = new ArrayList<>();
    iterator().forEachRemaining(result::add);
    Collections.reverse(result);

    return result;
  }

  /**
   * Filters a sequence of a {@link LinkedNode}s by the type of items they contain.
   *
   * @param stream the stream to filter
   * @param clazz the type of item for which to filter
   * @param <T> the type of item for which to filter
   * @return a {@link Stream} over all {@link LinkedNode}s containing items of the provided {@code clazz}
   */
  @SuppressWarnings("unchecked")
  public static <T> Stream<LinkedNode<T>> filterByClass(Stream<? extends LinkedNode<?>> stream,
      Class<? extends T> clazz) {
    return (Stream) stream.filter(node -> clazz.isInstance(node.getItem()));
  }
}
