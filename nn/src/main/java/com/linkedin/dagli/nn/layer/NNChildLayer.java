package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.Versioned;
import com.linkedin.dagli.nn.result.NNResult;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.util.invariant.Arguments;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Base class for all child layers (nodes) in the neural network (those with one or more parent layers).
 *
 * This class does not follow the usual "Abstract____" naming convention for abstract classes to allow for more readable
 * public APIs and to reflect the fact that it does not allow for clients to derive new implementations.
 *
 * @param <S> the ultimate (most derived) type deriving from this class
 */
@Versioned
public abstract class NNChildLayer<R, S extends NNChildLayer<R, S>> extends NNLayer<R, S> {
  private static final long serialVersionUID = 1;

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
    InternalAPI() { }

    /**
     * Non-root layers will accept one or more neural network layers as their inputs, which can be obtained by this
     * method.
     *
     * @return a list of {@link NNLayer}
     */
    public List<? extends NNLayer<?, ? extends NonTerminalLayer>> getInputLayers() {
      return NNChildLayer.this.getInputLayers();
    }

    /**
     * A non-root layer may have multiple equivalent (as determined by {@link Object#equals(Object)}) inputs.  This
     * method returns a (de-duplicated) set (with unspecified order) of the layers returned by
     * {@link #getInputLayers()}.
     *
     * @return a set of the {@link NNLayer}s whose outputs are the inputs of this layer
     */
    public Set<? extends NNLayer<?, ? extends NonTerminalLayer>> getInputLayerSet() {
      return new HashSet<>(getInputLayers());
    }

    /**
     * Gets a producer that yields this layer's output in the form of instances of its canonical output type, given a
     * producer providing the NNResult for the containing neural network and the output index of this layer in that
     * network.
     *
     * @param nnResultProducer the {@link Producer} that will provide the {@link NNResult}s of inference in the neural
     *                         network
     * @param outputIndex the index of the output layer in the {@link NNResult} instances that will be produced by
     *                    {@code nnResultProducer}
     * @return a {@link Producer} (which will necessarily include {@code nnResultProducer} among its ancestors) providing
     *         the output of this layer in the form of its canonical output type
     */
    public Producer<R> outputFromNNResult(Producer<? extends NNResult> nnResultProducer, int outputIndex) {
      return NNChildLayer.this.outputFromNNResult(nnResultProducer, outputIndex);
    }
  }

  /**
   * Non-root layers will accept one or more neural network layers as their inputs, which can be obtained by this
   * method.
   *
   * @return a list of {@link NNLayer}
   */
  abstract List<? extends NNLayer<?, ? extends NonTerminalLayer>> getInputLayers();

  @Override
  void validate() {
    List<? extends NNLayer<?, ? extends NonTerminalLayer>> inputLayers = getInputLayers();
    Arguments.check(!getInputLayers().isEmpty(), "A child layer must have a parent layer");
    Arguments.check(!inputLayers.contains(null), "Parent layers must not be null");

    super.validate();
  }

  /**
   * Gets a producer that yields this layer's output in the form of instances of its canonical output type, given a
   * producer providing the NNResult for the containing neural network and the output index of this layer in that
   * network.
   *
   * @param nnResultProducer the {@link Producer} that will provide the {@link NNResult}s of inference in the neural
   *                         network
   * @param outputIndex the index of the output layer in the {@link NNResult} instances that will be produced by
   *                    {@code nnResultProducer}
   * @return a {@link Producer} (which will necessarily include {@code nnResultProducer} among its ancestors) providing
   *         the output of this layer in the form of its canonical output type
   */
  abstract Producer<R> outputFromNNResult(Producer<? extends NNResult> nnResultProducer, int outputIndex);
}
