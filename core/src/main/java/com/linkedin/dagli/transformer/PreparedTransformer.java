package com.linkedin.dagli.transformer;

import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.producer.ProducerType;
import com.linkedin.dagli.transformer.internal.PreparedTransformerInternalAPI;
import java.util.function.Function;


/**
 * Transformers take at least one {@link com.linkedin.dagli.preparer.Preparer} input(s) and produce a single output
 * (though this output may itself be a tuple, list, etc.)  They are the core conceit of Dagli, comprising all the nodes
 * of the computational directed acyclic graph other than the roots.
 *
 * PreparedTransformers can be applied to their inputs without further preparation to produce an output.  Prepared
 * transformers extend {@link PreparableTransformer}s; you can re-prepare a prepared transformer, but this is unusual
 * and the result is implementation specific (often just resulting in the original prepared transformer.)
 *
 * Note: implementations of PreparedTransformers should generally extend one of the AbstractPreparedTransformerX
 * classes, not implement this interface directly!
 *
 * @param <R> the type of output
 */
public interface PreparedTransformer<R> extends Transformer<R>, ProducerType<R, PreparedTransformer<R>> {
  @Override
  PreparedTransformerInternalAPI<R, ? extends PreparedTransformer<R>> internalAPI();

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
   * @param transformer the transformer to cast
   * @param <R> the type of result of the returned transformer
   * @return the passed transformer, typed to a new "supertype" of the original
   */
  @SuppressWarnings("unchecked")
  static <R> PreparedTransformer<R> cast(PreparedTransformer<? extends R> transformer) {
    return (PreparedTransformer<R>) transformer;
  }

  /**
   * Creates a new {@link MappedIterable.Prepared} that will obtain a prepared transformer from the given "factory
   * function", which will almost always be a {@code withInput(...)}-type method.
   *
   * For example, given a hypothetical {@code Concatenation} transformer that concatenates its two String inputs
   * provided as {@code Concatenation::withInputA(...)} and {@code Concatenation::withInputB(...)}, and wanted to
   * concatenate the String {@code "PREFIX"} to every String in lists of Strings, we could write something like:
   * <pre>{@code
   *    Placeholder<List<String>> stringList = new Placeholder<>();
   *    Concatenation prefixedString = new Concatenation().withInputA(new Constant<>("PREFIX"));
   *    MappedIterable.Prepared<String, String> prefixedStrings =
   *      PreparedTransformer.mapped(prefixedString::withInputB).withMappedInput(stringList);
   * }</pre>
   *
   * @param preparedWithMappedInputFunction the prepared transformer to wrap
   * @return a copy of this instance that will wrap the specified transformer
   */
  static <T, R, Q extends PreparedTransformer<? extends R>> MappedIterable.Prepared<T, R> mapped(
      Function<? super Placeholder<T>, Q> preparedWithMappedInputFunction) {
    return new MappedIterable.Prepared<>(preparedWithMappedInputFunction);
  }
}
