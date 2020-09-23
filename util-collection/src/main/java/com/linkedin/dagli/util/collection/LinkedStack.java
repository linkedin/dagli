package com.linkedin.dagli.util.collection;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * Collection that stores items in a unidirectional linked list.  Items may only be added and removed from the end of
 * the list, allowing it to serve as a last-in first-out stack as well as a general collection.
 *
 * Internally, the collection maintains a pointer to the <strong>last</strong> node in the list, which maintains a link
 * to its predecessor, and so on; this means that the collection can be very cheaply "forked" (cloned) and subsequent
 * items added without affecting the original collection, but the <strong>iteration order is the reverse of the
 * insertion order</strong>.  This also affects the order of elements produced by other methods, such as
 * {@link #toArray()} or {@link #stream()}.
 *
 * @param <T> the type of element stored in the collection
 */
public class LinkedStack<T> extends AbstractCollection<T> implements Serializable, Cloneable {
  private static final long serialVersionUID = 1;

  private LinkedNode<T> _lastNode = null;
  private int _size = 0;

  @Override
  @SuppressWarnings("unchecked") // clone will return same type of item
  public LinkedStack<T> clone() {
    try {
      return (LinkedStack<T>) super.clone();
    } catch (CloneNotSupportedException e) {
      // this should never happen
      throw new RuntimeException(e);
    }
  }

  @Override
  public Iterator<T> iterator() {
    return _lastNode == null ? Collections.emptyIterator() : _lastNode.iterator();
  }

  @Override
  public int size() {
    return _size;
  }

  @Override
  public boolean add(T t) {
    _lastNode = _lastNode == null ? new LinkedNode<>(t) : _lastNode.add(t);
    _size++;
    return true;
  }

  /**
   * Removes and returns the most recently-added item (this is also the item that is <strong>first</strong> in the
   * iteration order.)
   *
   * @return the most recently-added item; a {@link NoSuchElementException} will be thrown if the collection
   *         is empty
   */
  public T pop() {
    if (_lastNode == null) {
      throw new NoSuchElementException();
    }

    T result = _lastNode.getItem();
    _lastNode = _lastNode.getPreviousNode();
    _size--;
    return result;
  }

  /**
   * Returns the most recently-added item (this is also the item that is <strong>first</strong> in the iteration order.)
   *
   * @return the most recently-added item; a {@link NoSuchElementException} will be thrown if the collection is empty
   */
  public T peek() {
    if (_lastNode == null) {
      throw new NoSuchElementException();
    }

    return _lastNode.getItem();
  }

  @Override
  public int hashCode() {
    return Iterables.hashCodeOfOrderedElements(this) + LinkedStack.class.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof LinkedStack && Iterables.elementsAreEqual(this, (LinkedStack<?>) obj);
  }
}
