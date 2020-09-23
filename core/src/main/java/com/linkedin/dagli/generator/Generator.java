package com.linkedin.dagli.generator;

import com.linkedin.dagli.producer.ProducerType;
import com.linkedin.dagli.producer.RootProducer;


/**
 * Generators do not have inputs but instead create new values which can then be fed as inputs to transformers.
 *
 * @param <R> The type of thing generated
 */
public interface Generator<R> extends RootProducer<R>, ProducerType<R, Generator<R>> {
  /**
   * Generate a new value.  This is called for every input of the DAG.
   *
   * @param index 0-based index of the input for which the value is being generated.
   * @return a generated value
   */
  R generate(long index);

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
   * @param generator the generator to cast
   * @param <R> the type of result of the returned generator
   * @return the passed generator, typed to a new "supertype" of the original
   */
  @SuppressWarnings("unchecked")
  static <R> Generator<R> cast(Generator<? extends R> generator) {
    return (Generator<R>) generator;
  }
}
