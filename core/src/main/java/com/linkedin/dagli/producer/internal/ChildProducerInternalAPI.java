package com.linkedin.dagli.producer.internal;

import com.linkedin.dagli.producer.ChildProducer;
import com.linkedin.dagli.producer.Producer;
import java.util.List;


/**
 * The base interface for internal APIs of child (non-root) producers.
 *
 * @param <R> the type of result produced by the associated producer
 * @param <S> the type of the producer
 */
public interface ChildProducerInternalAPI<R, S extends ChildProducer<R>> extends ProducerInternalAPI<R, S> {
  /**
   * Gets a list of this child producer's "inputs" (nodes on which the child directly depends).
   *
   * How inputs are used depends on the type of the producer; for example,
   * {@link com.linkedin.dagli.transformer.Transformer}s consume the results produced by their inputs, whereas
   * {@link com.linkedin.dagli.view.TransformerView}s examine the prepared transformer generated from a preparable
   * transformer input.
   *
   * @return a list of the inputs; this list should not be modified by the caller
   */
  List<? extends Producer<?>> getInputList();

  /**
   * Creates a new {@link ChildProducer} that uses the specified inputs but is otherwise a copy of this one.
   *
   * The returned producer <strong>must</strong> be a new instance, even if the provided list of inputs matches the
   * current instance's input list, as Dagli may rely on this invariant.
   *
   * It is "unsafe" because the inputs provided are not (necessarily) type-checked, even at runtime, which may result
   * in logic bugs.  Runtime exceptions <strong>may</strong> be thrown in cases of input type or arity mismatches, but
   * this should not be relied upon.
   *
   * @param newInputs the new inputs to be used by the copy of the {@link ChildProducer} (a copy will be made such that
   *                  the returned instance is not tied to this list of inputs after the call completes)
   * @return a copy of the {@link ChildProducer} that uses the specified inputs
   */
  S withInputsUnsafe(List<? extends Producer<?>> newInputs);

  /**
   * Creates a copy of the given {@link ChildProducer} that uses the specified inputs (via
   * {@link #withInputsUnsafe(List)}).
   *
   * This is a convenience method that avoids the necessity of a (perfectly safe) "unsafe" cast in dependent code.
   *
   * @param childProducer the producer to copy, modified to use new inputs
   * @param newInputs the new inputs to use
   * @param <T> the type of the child producer
   * @return a copy of {@code childProducer} that will use (a copy of) {@code newInputs} as its inputs
   */
  @SuppressWarnings("unchecked") // safe because withInputsUnsafe(...) always returns an instance of the same class
  static <T extends ChildProducer<?>> T withInputsUnsafe(T childProducer, List<? extends Producer<?>> newInputs) {
    return (T) childProducer.internalAPI().withInputsUnsafe(newInputs);
  }
}
