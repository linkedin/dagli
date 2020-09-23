package com.linkedin.dagli.transformer;

import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.producer.ProducerType;
import com.linkedin.dagli.transformer.internal.PreparableTransformerInternalAPI;
import java.util.function.Function;


/**
 * Transformers take at least one {@link com.linkedin.dagli.preparer.Preparer} input(s) and produce a single output
 * (though this output may itself be a tuple, list, etc.)  They are the core conceit of Dagli, comprising all the nodes
 * of the computational directed acyclic graph other than the roots.
 *
 * PreparableTransformers are "prepared" (trained) when the (preparable) DAG containing them is prepared.   During
 * this process, <strong>two</strong> prepared transformers are actually created:
 * (1) A prepared transformer that is "prepared for new data" and returned as a node in the final, prepared DAG, which
 *     can then be used in the future for inference on previously-unseen, "new" data.
 * (2) A prepared transformer that is "prepared for preparation data" and used to transform its own preparation data so
 *     that the result can be supplied to downstream nodes while the DAG is being prepared).
 *
 * In <em>almost</em> all cases these two prepared transformers will actually be the same, but there are exceptions like
 * the KFoldCrossTrained transformer that need to supply different results during training and inference.
 *
 * Implementations of PreparableTransformers are strongly encouraged to extend one of the AbstractPreparableTransformerX
 * classes and not implement this interface directly.
 *
 * @param <R> the type of output
 * @param <N> the type of the prepared transformer (for new data--the type of the prepared transformer as applied to
 *            preparation/training data may be different)
 */
public interface PreparableTransformer<R, N extends PreparedTransformer<? extends R>> extends Transformer<R>, ProducerType<R, PreparableTransformer<R, N>> {
  @Override
  PreparableTransformerInternalAPI<R, N, ? extends PreparableTransformer<R, N>> internalAPI();

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
   * @param <N> the type of prepared transformer produced by the returned transformer
   * @return the passed transformer, typed to a new "supertype" of the original
   */
  @SuppressWarnings("unchecked")
  static <R, N extends PreparedTransformer<? extends R>> PreparableTransformer<R, N> cast(
      PreparableTransformer<? extends R, ? extends N> transformer) {
    return (PreparableTransformer<R, N>) transformer;
  }

  /**
   * Creates a {@link MappedIterable} that will obtain a (possibly preparable) transformer from the given "factory
   * function", which will almost always be a {@code withInput(...)}-type method corresponding to the input you wish to
   * map.
   *
   * Let's say we're training a multinomial {@code LiblinearClassifer}, but our data are packaged in such a way that
   * each String label is associated with a list of feature vectors, with each [label, feature vector] pair construing
   * a training example.  Then we can write something like this:
   * <pre>{@code
   *   Placeholder<String> label = new Placeholder<>();
   *   Placeholder<List<DenseVector>> featureVectors = new Placeholder<>();
   *   LiblinearClassification<String> liblinear =
   *     new LiblinearClassification<String>().withLabelInput(label);
   *   MappedIterable<String, DiscreteDistribution<String>> classification =
   *      PreparableTransformer.mapped(liblinear::withFeaturesInput)).withMappedInput(featureVectors);
   * }</pre>
   *
   * During preparation, {@code classification} will then provide a [String label, DenseVector features] pair for every
   * element in the {@code featureVectors} list, and, during inference, it will correspondingly produce a list of
   * predicted labels (one for each feature vector).
   *
   * @param preparableWithMappedInputFunction a function that obtains a (possibly preparable) transformer given a provided
   *                                    placeholder representing the value to be mapped
   */
  static <T, R, Q extends Transformer<? extends R>> MappedIterable<T, R> mapped(
      Function<? super Placeholder<T>, Q> preparableWithMappedInputFunction) {
    return new MappedIterable<>(preparableWithMappedInputFunction);
  }
}
