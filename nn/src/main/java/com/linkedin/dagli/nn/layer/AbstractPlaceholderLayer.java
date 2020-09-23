package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.producer.Producer;
import java.util.Objects;


/**
 * Base class for input layers in the neural network, providing the values of producers in the encapsulating DAG as
 * input values to the neural network.
 *
 * Duplicate placeholder layers are de-duplicated when constructing the neural network.  Two placeholder layers compare
 * as {@link Object#equals(Object)} iff they are the same type and have their input producers are also
 * {@link Object#equals(Object)}.
 *
 * @param <A> the type of value accepted by this placeholder
 * @param <S> the ultimate derived type of the concrete descendent class
 */
abstract class AbstractPlaceholderLayer<A, R, S extends AbstractPlaceholderLayer<A, R, S>> extends NNRootLayer<R, S> {
  private static final long serialVersionUID = 1;

  Producer<? extends A> _input = MissingInput.get();

  @Override
  public InternalAPI internalAPI() {
    return new InternalAPI();
  }

  /**
   * Methods provided exclusively for use by Dagli.
   *
   * Client code should not use these methods as they are subject to change at any time.
   */
  public class InternalAPI extends NNLayer<R, S>.InternalAPI {
    /**
     * @return the (sole) input to this layer from the encapsulating DAG.
     */
    public Producer<? extends A> getInputProducer() {
      return AbstractPlaceholderLayer.this.getInputProducer();
    }
  }

  /**
   * @return the (sole) input to this layer from the encapsulating DAG.
   */
  Producer<? extends A> getInputProducer() {
    return _input;
  }

  @Override
  public int hashCode() {
    return getClass().hashCode() + _input.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }

    return Objects.equals(this._input, ((AbstractPlaceholderLayer<?, ?, ?>) obj)._input);
  }

  /**
   * Returns a copy of this layer that will use the specified input.
   *
   * @param input the producer providing the input to this layer
   * @return a copy of this layer that will use the specified input
   */
  public S withInput(Producer<? extends A> input) {
    return clone(c -> c._input = input);
  }
}
