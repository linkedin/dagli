package com.linkedin.dagli.producer;

import com.linkedin.dagli.handle.ProducerHandle;
import com.linkedin.dagli.producer.internal.AncestorSpliterator;
import com.linkedin.dagli.producer.internal.ProducerInternalAPI;
import com.linkedin.dagli.util.collection.LinkedNode;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * The base interface of all producer interfaces, including placeholders, transformers, generators, etc.
 *
 * All nodes in a Dagli DAG derive from this interface, and all {@link Producer}s must be <strong>immutable</strong>;
 * that is, calling a (public) method on a particular producer instance with exactly the same parameter values should
 * always yield the same result.
 *
 * ProducerUtil, are the name suggest, produce some value.  {@link RootProducer}s have no inputs:
 * {@link com.linkedin.dagli.placeholder.Placeholder}s produce values that are provided to the containing DAG as data,
 * while {@link com.linkedin.dagli.generator.Generator}s "generate" a sequence of values de novo.
 * {@link ChildProducer}s, such as {@link com.linkedin.dagli.transformer.Transformer}s, require input from at least one
 * other node to generate their values.
 *
 * @param <R> the type of result produced by the producer
 */
public interface Producer<R> extends Serializable {
  /**
   * Gets a name for this producer.  This name is intended for human consumption and should not be relied upon as a
   * unique or consistent identifier.  In particular, names may differ even for instances for which
   * {@link Object#equals(Object)} returns true.
   *
   * Generally the name of a producer should not include the names of its input producers' {@link #getName()}s as this
   * may create arbitrarily long names; instead, use the inputs' {@link #getShortName()}s.
   *
   * The default implementation returns a name which incorporates the class name and the "identityHashCode" that is
   * specific to this instance (and would not be consistent if the instance were serialized and a deserialized copy
   * examined).
   *
   * @return a human-readable(ish) name for this producer.
   */
  default String getName() {
    return this.getClass().getSimpleName() + " " + Integer.toHexString(System.identityHashCode(this));
  }

  /**
   * Gets a short name for this producer.  This name is intended for human consumption and should not be relied upon as
   * a unique or consistent identifier.  The default implementation simply returns {@link #getName()}.
   *
   * This method should be overridden when {@link #getName()} returns a name that includes this producer's inputs; such
   * recursively nested names can become quite long.  A short name should not depend on the names of the inputs (those
   * returned by either {@link #getName()} or {@link #getShortName()}).
   *
   * @return a human-readable(ish) name for this producer.
   */
  default String getShortName() {
    return getName();
  }

  /**
   * Validates the node, throwing an exception if it is misconfigured.  Only this node (and not any inputs it may have)
   * should be validated.  The callee is not responsible for "chaining" this call to its inputs, nor is it responsible
   * for ensuring that its inputs are present (and not, e.g. a {@link MissingInput} stand-in).  However, the callee
   * <em>is</em> responsible for chaining this method to its superclass (if the superclass implements this
   * interface).
   *
   * Transformers that cannot have an invalid state/configuration do not need to override this method.
   *
   * Validation is performed when constructing a DAG containing this node.
   *
   * @implNote validate() finds configuration errors at runtime when a DAG is being constructed and before it
   * begins processing data.  This is good, but not as good as finding errors at compile-time, e.g. via static typing.
   * Nonetheless, it is still <strong>much</strong> preferable to only reporting the error once processing has
   * commenced.
   */
  default void validate() { }

  /**
   * Checks if a {@link Producer} will always produce the same value for all examples during any particular DAG
   * execution.
   *
   * Knowing that {@link Producer} has a constant result allows for a number of useful optimizations; for example, a
   * constant-result transformer can be invoked just once and its result cached and used as the result for all further
   * examples in that DAG execution or, better yet, its value can be precomputed and it can be replaced by a
   * {@link com.linkedin.dagli.generator.Constant}.  It can also allow for correctness checks: for instance, a neural
   * network's DenseLayer can accept its unit count (number of neurons in the layer) hyperparameter from a
   * {@link Producer}, but this value must be constant throughout the execution of the DAG (it wouldn't be possible to
   * change the neural network architecture on an example-by-example basis!)
   *
   * Common types of Producers that are always constant-result include
   * {@link com.linkedin.dagli.generator.Constant}, all {@link com.linkedin.dagli.view.TransformerView}s and all
   * ConstantResultTransformations, e.g. {@link com.linkedin.dagli.transformer.ConstantResultTransformation2}.
   *
   * A slightly more sophisticated example would be a "Cardinality"
   * {@link com.linkedin.dagli.transformer.PreparableTransformer} that calculated the number of distinct input values it
   * saw during DAG preparation and created a {@link com.linkedin.dagli.transformer.PreparedTransformer} that simply
   * produced this cardinality value while ignoring its input.
   *
   * This method will return true if a {@link Producer} can be inferred to be constant-result because either:
   * (1) The {@link Producer} explicitly identifies itself as constant result, e.g. by implementing
   *     {@link AbstractProducer#hasAlwaysConstantResult()}.
   * (2) The {@link Producer} is a child node (i.e. a {@link ChildProducer}, such as a transformer or transformer view)
   *     and all its parents are constant-result (this is because all child nodes in Dagli are assumed to have a
   *     deterministic result given their inputs.)
   *
   * See {@link AbstractProducer#hasAlwaysConstantResult()} for additional information.
   *
   * @return true if this {@link Producer} will always produce the same result in any given DAG execution, false
   *         otherwise
   */
  boolean hasConstantResult();

  /**
   * Retrieves an object that exposes the private API used by Dagli internally.
   * Client code outside should generally not use this.
   *
   * @implNote Dagli provides abstract classes for you to derive your producer from (e.g.
   * AbstractPreparableTransformer1); these classes already implement the internal API, so no implementation
   * on your part is required.  Only very advanced use cases will implement the Generator or Transformer interfaces
   * directly, and only in these (very rare) instances you'll need to create and return the API object yourself.
   *
   * @return an object that exposes the internal API corresponding to this producer.
   */
  ProducerInternalAPI<R, ? extends Producer<R>> internalAPI();

  /**
   * Casts a producer to an effective "supertype" interface.  The semantics of the producer guarantee that the returned
   * type is valid for the instance.
   *
   * Note that although this and other {@code cast(...)} methods are safe, this safety extends only to the
   * interfaces for which they are implemented.  The covariance and contravariance relationships existing for these interfaces
   * do not necessarily hold for their derived classes.  For example, a {@code PreparedTransformer<String>} is also a
   * {@code PreparedTransformer<Object>}, but a {@code MyTransformer<String>} cannot necessarily be safely treated as a
   * {@code MyTransformer<Object>}.
   *
   * @param producer the producer to cast
   * @param <R> the type of result of the returned producer
   * @return the passed producer, typed to a new "supertype" of the original
   */
  @SuppressWarnings("unchecked")
  static <R> Producer<R> cast(Producer<? extends R> producer) {
    return (Producer<R>) producer;
  }

  /**
   * Casts a {@code Producer<? extends Comparable<? extends T>>} to a {@code Producer<T>}.
   *
   * This cast is safe so long as {@code T} (or the relevant subclass) <i>correctly</i> implements {@link Comparable} in
   * accordance with its specification.
   *
   * Consider a type A implementing {@code Comparable<? extends T>}; we must show that A must also necessarily implement
   * or extend T.
   *
   * The specification for {@link Comparable} has two relevant requirements:
   * (1) transitivity: "{@code (x.compareTo(y) > 0 && y.compareTo(z) > 0)} implies {@code x.compareTo(z) > 0}"
   * (2) bidirectionality: "{@code sgn(x.compareTo(y)) == -sgn(y.compareTo(x))}"
   *
   * (1) Implies that if A is comparable to B, and B is comparable to C, then A is comparable to C.
   * (2) Implies that if A is comparable to B, then B must also be comparable to A.
   * Since B must be comparable to A (2), A is also comparable to A (1) (let C = A), which implies A extends T.
   *
   * The need for this cast frequently arises because the Java type system will not allow explicit type constraints such
   * as {@code A extends T & T extends Comparable<? super T>} (if A is constrained to extend T, it cannot be further
   * constrained).
   *
   * @param producer the producer to cast
   * @param <R> the type of result of the returned producer
   * @return  the passed producer, typed to an implicit supertype of the original
   */
  @SuppressWarnings("unchecked") // safe as shown above
  static <R> Producer<R> castComparable(Producer<? extends Comparable<? extends R>> producer) {
    return (Producer<R>) producer;
  }

  /**
   * Returns a stream of the producers in a subgraph defined as the provided targets and all their ancestors.  Producers
   * are enumerated in the order they are discovered via a breadth-first search starting from the targets; more
   * proximate ancestors to the targets will be returned first.
   *
   * The producers are provided as {@link LinkedNode}s, each representing a shortest-path from that producer to one of
   * the targets.  Each producer will be enumerated only once (even if there are multiple unique paths to it).
   *
   * @param targets the target producers that define the subgraph to enumerate
   * @return a stream of {@link LinkedNode}s representing paths to each target and their ancestors
   */
  static Stream<LinkedNode<Producer<?>>> subgraphProducers(Producer<?>... targets) {
    return subgraphProducers(Arrays.asList(targets));
  }

  /**
   * Returns a stream of the producers in a subgraph defined as the provided targets and all their ancestors.  Producers
   * are enumerated in the order they are discovered via a breadth-first search starting from the targets; more
   * proximate ancestors to the targets will be returned first.
   *
   * The producers are provided as {@link LinkedNode}s, each representing a shortest-path from that producer to one of
   * the targets.  Each producer will be enumerated only once (even if there are multiple unique paths to it).
   *
   * @param targets the target producers that define the subgraph to enumerate
   * @return a stream of {@link LinkedNode}s representing paths to each target and their ancestors
   */
  static Stream<LinkedNode<Producer<?>>> subgraphProducers(List<Producer<?>> targets) {
    return StreamSupport.stream(new AncestorSpliterator(targets, Integer.MAX_VALUE, ChildProducer::getParents), false);
  }

  /**
   * Gets the handle for this transformer.  Handles are useful for referring to a {@link Producer} without requiring a
   * reference to the {@link Producer} instance itself.
   *
   * Handles can be serialized and deserialized, and a serialized and deserialized {@link Producer} instance will always
   * maintain the same handle.
   *
   * If two {@link Producer} instances share the same handle (this can happen via deserialization, for example) they
   * will be functionally identical: any fields visible outside the class will have equal values, and any methods
   * visible outside the class will have identical behavior.
   *
   * Conversely, two indistinguishable transformers will not <i>necessarily</i> share the same handle, and
   * handle equality is not required for two instances to evaluate as {@link Object#equals(Object)}.
   *
   * @param producer the producer whose handle is sought
   * @return the handle for the provided producer
   */
  @SuppressWarnings("unchecked")
  static <T extends Producer<?>> ProducerHandle<T> handle(T producer) {
    return (ProducerHandle<T>) producer.internalAPI().getHandle();
  }
}

