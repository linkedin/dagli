package com.linkedin.dagli.input;

import com.linkedin.dagli.util.cloneable.AbstractCloneable;
import com.linkedin.dagli.util.collection.LinkedStack;
import java.util.List;


/**
 * Base class for input configurators, optionally supporting the aggregation of one or more input values.
 *
 * @param <V> the type of inputs that may be aggregated (if applicable; otherwise Void)
 * @param <T> the type of the configured result object (e.g. a transformer)
 * @param <S> the type of the derived-most class extended this abstract class
 */
public abstract class AbstractInput<V, T, S extends AbstractInput<V, T, S>>
    extends AbstractCloneable<S> {
  private LinkedStack<V> _inputStack = LinkedStack.empty();

  /**
   * @return the list of inputs to be aggregated
   */
  protected List<V> getInputs() {
    return _inputStack.toList();
  }

  /**
   * Adds an input to the list of inputs to be aggregated
   * @param input the input to add
   * @return a copy of this instance with the given input added
   */
  protected S withAddedInput(V input) {
    return clone(c -> ((AbstractInput<V, ?, ?>) c)._inputStack = _inputStack.push(input));
  }

  /**
   * Called when all inputs have been added.
   *
   * @return the resulting object that will accept the configured aggregated input
   */
}
