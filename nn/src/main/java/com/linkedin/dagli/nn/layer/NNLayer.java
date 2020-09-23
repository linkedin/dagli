package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.Versioned;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.DynamicInputs;
import com.linkedin.dagli.util.cloneable.AbstractCloneable;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Base class for all layers (nodes) in the neural network.
 *
 * This class does not follow the usual "Abstract____" naming convention for abstract classes to allow for more readable
 * public APIs and to reflect the fact that it does not allow for clients to derive new implementations.
 *
 * The {@link NNLayerVisitor} inferface provides a convenient mechanism for processing layers: different neural network
 * frameworks can provide different implementations of this interface to analyze or transform Dagli layers in a
 * framework-proprietary method.  A major benefit of this pattern is that it requires the frameworks to explicitly
 * handle every layer type ({@link UnsupportedOperationException} can still be thrown for those that are unsupported).
 *
 * @param <R> the type of the output produced by this layer
 * @param <S> the ultimate (most derived) type deriving from this class
 */
@Versioned
public abstract class NNLayer<R, S extends NNLayer<R, S>> extends AbstractCloneable<S> implements Serializable {
  private static final long serialVersionUID = 1;

  private String _name = null;

  /**
   * The handle that identifies this specific layer; clones of this instance will be assigned a different handle.
   *
   * Generally handles are unique, although it is possible to create multiple instances with the same handle through
   * serialization and deserialization; any such duplicates will also share all other fields and thus be
   * indistinguishable (except for their location in memory, of course).
   */
  @SuppressWarnings("unchecked") // we know our class is S
  private LayerHandle<S> _handle = new LayerHandle<>((Class<S>) getClass());



  /**
   * Package-private constructor to prevent new immediate subclasses from being defined outside our package
   */
  NNLayer() { }

  /**
   * @return an {@link InternalAPI} instance that provides methods that are exclusively intended for internal use by
   *         Dagli; these methods should not be used by client code and are subject to change at any time.
   */
  public InternalAPI internalAPI() {
    return new InternalAPI();
  }

  /**
   * Methods provided exclusively for use by Dagli.
   *
   * Client code should not use these methods as they are subject to change at any time.
   */
  public class InternalAPI {
    InternalAPI() { }

    /**
     * Asserts that this layer has a logically complete and correct configuration, throwing an exception if this is not
     * the case.  This allows prima facie errors to be caught early, before the DAG containing the neural network is
     * executed and before the neural network is materialized in the "native format" of the underlying implementation.
     */
    public void validate() {
      NNLayer.this.validate();
    }

    /**
     * Gets the constant-result (see {@link Producer#hasConstantResult()} for details) {@link Producer}s from the DAG
     * encapsulating the neural network that are required by this layer to calculate its dynamic configuration.  These
     * {@link Producer}s will always be included in the set of inputs to the neural network as a whole.
     *
     * @return a list of {@link Producer}s required for this layer to calculate its {@link DynamicLayerConfig}
     */
    public List<? extends Producer<?>> getDynamicConfigurationInputProducers() {
      return NNLayer.this.getDynamicConfigurationInputProducers();
    }

    /**
     * Gets the {@link DynamicLayerConfig} corresponding to a given layer given the dynamic configuration of its
     * ancestors, the {@link DynamicInputs}, and the available {@link DynamicInputs.ConstantInputs}.
     *
     * @param ancestorConfigs a map from all the ancestor layers of this layer (and possibly others) to their dynamic
     *                        configurations
     * @param dynamicInputs the {@link DynamicInputs} for the current DAG execution of the neural network
     * @param constantInputs the {@link DynamicInputs.ConstantInputs} for the current DAG execution of the neural network
     * @return the dynamic configuration for this layer
     */
    public DynamicLayerConfig getDynamicConfig(Map<NNLayer<?, ?>, DynamicLayerConfig> ancestorConfigs,
        DynamicInputs dynamicInputs, DynamicInputs.ConstantInputs constantInputs) {
      return NNLayer.this.getDynamicConfig(ancestorConfigs, dynamicInputs, constantInputs);
    }
  }

  // override clone() so new clones are given different UUIDs than the cloned instance
  @Override
  @SuppressWarnings("unchecked") // we know our class is S
  protected S clone() {
    S res = super.clone();
    ((NNLayer<R, S>) res)._handle = new LayerHandle<>((Class<S>) getClass());
    return res;
  }

  /**
   * Gets the handle to this instance.  Handles may be used to identify this layer without requiring the actual layer.
   * It is possible for two identical layers with identical handles to exist due to serialization and deserialization,
   * but all distinct layers will always have distinct handles.
   *
   * @return the handle for this layer
   */
  public LayerHandle<S> getHandle() {
    return _handle;
  }

  @Override
  public int hashCode() {
    return _handle.hashCode() + 0x78886461; // add random value to distinguish from handle hashes
  }

  /**
   * Checks if another object is a layer with the same handle.  Note that two layers with properties that are otherwise
   * identical beyond distinct handles should not be considered semantically equal, because adding such "duplicates" is
   * not in general idempotent to the network: for example, a network with two identical loss layers taking the same
   * layer as input induces twice as much loss (relative to other loss layers elsewhere in the network) as a single
   * layer.
   *
   * @param obj an object to check for equality
   * @return whether or not the provided object is considered equal to this one
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof NNLayer)) {
      return false;
    }
    return _handle.equals(((NNLayer<?, ?>) obj)._handle);
  }

  @Override
  public String toString() {
    return _name == null ? this.getClass().getSimpleName() : _name;
  }

  /**
   * @return the name of this layer, or null if no name has been set
   */
  public final String getName() {
    return _name;
  }

  /**
   * @return the layer's name if it has one; otherwise a non-ambiguous identifier for this instance
   */
  String getIdentifier() {
    return _name == null ? super.toString() : _name;
  }

  /**
   * Returns a copy of this instance that will have the specified name.  Names may be useful for debugging or other
   * introspection into the network but do not affect its semantics.  Setting a name is optional.  By default the name
   * is <code>null</code>, denoting that no name has been assigned.
   *
   * @param name the name for this layer
   * @return a copy of this instance that will assume the specified name
   */
  public S withName(String name) {
    return clone(c -> ((NNLayer<?, ?>) c)._name = name);
  }

  /**
   * Accepts a visitor, passing this instance to the appropriate visitor method.
   *
   * @param visitor the visitor that will process this instance
   */
  public abstract <T> T accept(NNLayerVisitor<T> visitor);

  /**
   * Asserts that this layer has a logically complete and correct configuration, throwing an exception if this is not
   * the case.  This allows prima facie errors to be caught early, before the DAG containing the neural network is
   * executed and before the neural network is materialized in the "native format" of the underlying implementation.
   *
   * Implementors: validation should not recurse to parent layers (if applicable) or any {@link Producer} inputs this
   * layer may have.
   */
  void validate() {
    if (getDynamicConfigurationInputProducers().contains(null)) {
      throw new NullPointerException("One of the input producers required by " + toString() + " layer is null");
    }

    if (getDynamicConfigurationInputProducers().contains(MissingInput.get())) {
      throw new IllegalStateException("One of the input producers required by " + toString() + " layer is missing");
    }
  }

  /**
   * Gets the constant-result (see {@link Producer#hasConstantResult()} for details) {@link Producer}s from the DAG
   * encapsulating the neural network that are required by this layer to calculate its dynamic configuration.  These
   * {@link Producer}s will always be included in the set of inputs to the neural network as a whole.
   *
   * @return a list of {@link Producer}s required for this layer to calculate its {@link DynamicLayerConfig}
   */
  List<? extends Producer<?>> getDynamicConfigurationInputProducers() {
    return Collections.emptyList();
  }

  /**
   * Gets the {@link DynamicLayerConfig} corresponding to a given layer given the dynamic configuration of its
   * ancestors, the {@link DynamicInputs}, and the available {@link DynamicInputs.ConstantInputs}.
   *
   * @param ancestorConfigs a map from all the ancestor layers of this layer (and possibly others) to their dynamic
   *                        configurations
   * @param dynamicInputs the {@link DynamicInputs} for the current DAG execution of the neural network
   * @param constantInputs the {@link DynamicInputs.ConstantInputs} for the current DAG execution of the neural network
   * @return the dynamic configuration for this layer
   */
  abstract DynamicLayerConfig getDynamicConfig(Map<NNLayer<?, ?>, DynamicLayerConfig> ancestorConfigs,
      DynamicInputs dynamicInputs, DynamicInputs.ConstantInputs constantInputs);
}
