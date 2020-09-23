package com.linkedin.dagli.producer.internal;

import com.linkedin.dagli.handle.ProducerHandle;
import com.linkedin.dagli.reducer.ClassReducerTable;
import com.linkedin.dagli.reducer.Reducer;
import com.linkedin.dagli.producer.Producer;
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
}
