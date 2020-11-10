package com.linkedin.dagli.producer.internal;

import com.linkedin.dagli.dag.Graph;
import com.linkedin.dagli.handle.ProducerHandle;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.reducer.ClassReducerTable;
import com.linkedin.dagli.reducer.Reducer;
import java.lang.reflect.Type;
import java.util.Collection;


/**
 * The root of the internal API hierarchy for Producers of all types.
 *
 * This interface, and all interfaces extending it and classes implementing it, are intended for use only within the
 * Dagli library and should not be directly used by library clients.
 *
 * The internal API objects are one way for Dagli components to communicate across package boundaries without adding
 * additional public methods on the relevant classes.  Although it is (of course) possible for a client of the Dagli
 * library to obtain an internal API object, such use is explicitly discouraged since even minor revisions to the
 * library may involve substantial changes to the internal API.
 *
 * @param <R> the type of object produced by this producer
 * @param <S> the derived type of this producer (this is used in a curiously-recurring template pattern sort of way)
 */
public interface ProducerInternalAPI<R, S extends Producer<R>> {
  /**
   * @return a collection of {@link Reducer}s that should be applied to the DAG containing this node; this
   *         collection will not be modified
   */
  Collection<? extends Reducer<? super S>> getGraphReducers();

  /**
   * @return a {@link ClassReducerTable} that provides {@link Reducer}s for other producers within the DAG
   *         containing this producer that belong to certain classes.
   */
  ClassReducerTable getClassReducerTable();

  /**
   * Returns true if this producer will produce the same value for all examples <strong>in a given execution of the DAG
   * </strong>.  Unlike {@link Producer#hasConstantResult()}, this must be true regardless of the producer's ancestors
   * in the DAG (i.e. this producer should still have a constant result even if its parents are arbitrarily changed).
   *
   * For example, if we create a "Cardinality" PreparableTransformer that takes a single input and yields a
   * PreparedTransformer} that outputs the total number of distinct inputted values seen during preparation (for all
   * examples), the PreparableTransformer has a constant result.
   *
   * If this method returns true, the producer must have a constant result <strong>regardless of the examples in a
   * particular run</strong>; a node which produces the same value for all examples in some DAG executions but not
   * others is not considered to have a constant result.
   *
   * PreparableTransformers that are always-constant-result must also prepare to PreparedTransformers (for "preparation"
   * data and "new" data) that are themselves always-constant-result, also returning true for
   * {@link #hasAlwaysConstantResult()}.
   *
   * An always-constant-value {@link com.linkedin.dagli.transformer.PreparedTransformer} will be assumed by Dagli to
   * always return the same value regardless of its input values, even if those values are nulls.  Dagli may
   * consequently apply the transformer by passing null inputs to pre-compute its constant result value.
   *
   * Core types of Producers that have constant results include Constant, all TransformerViews and all
   * ConstantResultTransformations.
   *
   * Note that, even if this method returns false, Dagli can infer the producer will have a constant result when its
   * parents all have constant results; this is because all child nodes in Dagli are assumed to be deterministic, always
   * producing the same output given the same inputs).
   *
   * Because constant-resultness can be determined for any producer instance <strong>before</strong> executing its DAG,
   * it is very useful for certain correctness checks and optimizations.
   *
   * @return whether this producer will always have a constant result throughout any given DAG execution, regardless of
   *         its parents
   */
  boolean hasAlwaysConstantResult();

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
   * Conversely, two indistinguishable transformers will not <strong>necessarily</strong> share the same handle.
   *
   * @implNote usually, new transformers are implemented by deriving from an abstract class, such as
   * AbstractPreparedTransformer1; these classes already provide a suitable implementation of getHandle().
   *
   * @return the handle for this producer
   */
  ProducerHandle<S> getHandle();

  /**
   * Gets a "subgraph" that describes this producer as a {@link Graph} of arbitrary nodes.  This subgraph should provide
   * a human-interpretable graph (e.g. to be rendered by a visualizer) representing this producer's architecture.
   *
   * Although some complex producers can benefit greatly by providing a subgraph (e.g. neural networks), it is not
   * required and, moreover, inappropriate for most producer implementations; these will simply return {@code null}.
   *
   * Guidelines:
   * (1) The <i>parents</i> of subgraph nodes can be either be within the subgraph or among the ancestors of this
   *     producer in the encapsulating DAG.  Subgraph nodes must not have children outside the subgraph.
   * (2) A producer's subgraph nodes should contain that producer, with edges pointing to other nodes in the subgraph
   *     indicating how the final result output by that producer is derived.
   * (3) The types of the vertices in the subgraph may be any arbitrary objects, but it should not include any producer
   *     that is also present in the encapsulating DAG (except for the {@link Producer} which this subgraph represents.)
   * (4) Subgraphs may contain loops.
   *
   * Subgraphs are <strong>not</strong> guaranteed to be consistent, even across producers that compare as
   * {@link Object#equals(Object)}.  In particular, a deserialized object need not have the same subgraph as its
   * progenitor.
   *
   * @return a human-interpretable description of this producer as a (sub)graph, or {@code null} when no graph is
   *         available
   */
  Graph<Object> subgraph();

  /**
   * Gets a type that is a supertype of all results produced by this producer.
   *
   * Dagli provides a default implementation of this method that will attempt to use reflection to find the result type,
   * but because generics are not (at present) reified in Java, the returned type is only guaranteed to be a supertype
   * of the result type, quite possibly an {@link Object}, a {@link java.lang.reflect.WildcardType}, or a
   * {@link java.lang.reflect.TypeVariable} if no more concrete, specific type can be ascertained.
   *
   * Overriding the default implementation is generally not necessary, but may be helpful in edge cases where the result
   * type is generic but the producer has some way of ascertaining that type at run-time (e.g. if it depends on the
   * input producers' result types).
   *
   * A naked type is considered to be a "supertype" of the corresponding parameterized type (e.g. {@code List}
   * is considered a valid supertype of {@code List<String>}).  Additionally, the supertype of all results may be an
   * interface, and is not strictly required to be a supertype of R (the generic type parameter that is [also] a
   * supertype of all result instances): for example, a producer that is declared to have a {@code R = Number} result
   * but knows that it will always produce {@code Double} instances can return {@code Double} from this method.
   *
   * @return a supertype of all result objects that may be produced by this producer
   */
  Type getResultSupertype();
}
