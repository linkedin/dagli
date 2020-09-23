package com.linkedin.dagli.transformer;

import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.producer.ChildProducer;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.producer.internal.ChildProducerInternalAPI;
import com.linkedin.dagli.transformer.internal.TransformerInternalAPI;
import com.linkedin.dagli.util.collection.Iterables;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Transformers take at least one {@link com.linkedin.dagli.producer.Producer} input(s) and produce a single output
 * (though this output may itself be a tuple, list, etc.)  They are the core conceit of Dagli, comprising all the nodes
 * of the computational directed acyclic graph other than the roots.
 *
 * Transformers are either "preparable" or "prepared", and the corresponding interfaces {@link PreparableTransformer}
 * and {@link PreparedTransformer} derive from Transformer.  There are also analogous hierarchies (all descendent from
 * {@link Transformer}) for the arity-specific interfaces (e.g. {@link PreparedTransformer4} extends both
 * {@link Transformer4} and {@link PreparedTransformer}, and {@link Transformer4} extends {@link Transformer}) and for
 * the abstract implementations (e.g. {@link com.linkedin.dagli.transformer.AbstractPreparedTransformer4} extends
 * {@link com.linkedin.dagli.transformer.AbstractTransformer4}.
 *
 * Preparable transformers are "prepared" with inputted data to produce a prepared transformer, which may
 * then be applied to new data (this parallels training a model, which may then be used for inference).
 *
 * Prepared transformers are, essentially, functions: they take one or more inputs and produce a result.  They could be
 * trained models being applied to an example to infer the label, feature extractors, etc.
 *
 * Note: implementations of transformers should generally extend either the AbstractPreparableTransformerX
 * or AbstractPreparedTransformerX classes (where X is the "arity", the number of inputs), not implement any of the
 * interfaces directly!
 *
 * @param <R> the type of output
 */
public interface Transformer<R> extends ChildProducer<R> {
  @Override
  TransformerInternalAPI<R, ? extends Transformer<R>> internalAPI();

  /**
   * Checks if two transformers share the same inputs (and in the same positions, e.g. input #3 must be the same for
   * both).
   *
   * @param transformer1 the first transformer
   * @param transformer2 the second transformer
   * @return true if both transformers have the same inputs in the same positions
   */
  static boolean sameInputs(ChildProducer<?> transformer1, ChildProducer<?> transformer2) {
    return transformer1.internalAPI().getInputList().equals(transformer2.internalAPI().getInputList());
  }

  /**
   * Checks if two transformers share the same inputs, regardless of the position of each input.  E.g. if transformer's
   * first input is A, its second input is B, and its third input is also A, it will match another transformer whose
   * three inputs are B, A and A.
   *
   * Since the same {@link Producer} can act as an input to a given transformer multiple times, this method makes sure
   * that the number of inputs provided by each producer is the same across both transformers.  The transformers
   * from our previous example would not match one whose inputs were B, B and A, for example, because the previous
   * transformers had two A's and just one B.
   *
   * This method can be useful when the ordering of the inputs is immaterial; for example, in transformers that sum
   * their input values.
   *
   * @param transformer1 the first transformer
   * @param transformer2 the second transformer
   * @return true if both transformers have the same inputs (regardless of position)
   */
  static boolean sameUnorderedInputs(ChildProducer<?> transformer1, ChildProducer<?> transformer2) {
    return Iterables.sameMultisetOfElements(transformer1.internalAPI().getInputList(),
        transformer2.internalAPI().getInputList());
  }

  /**
   * Calculates a hash code corresponding to a transformer's inputs (and not any other property of the transformer).
   * This hash code is sensitive to the positions of the inputs.
   *
   * If two transformers have the same inputs in the same positions, the value returned by this method for both will be
   * the same.
   *
   * @param transformer the transformer whose inputs will be examined
   * @return a hash code for the transformer's inputs
   */
  static int hashCodeOfInputs(ChildProducer<?> transformer) {
    return transformer.internalAPI().getInputList().hashCode(); // use List's hashCode()
  }

  /**
   * Calculates a hash code corresponding to a transformer's inputs (and not any other property of the transformer).
   * This hash code is <strong>not</strong> sensitive to the positions of the inputs.
   *
   * If two transformers have the same inputs (and, in the case of duplicate inputs, the same number of duplicates for
   * each input), the value returned by this method for both will be the same regardless of the positions of those
   * inputs.  This is analogous to {@link #sameUnorderedInputs(ChildProducer, ChildProducer)}.
   *
   * @param transformer the transformer whose inputs will be examined
   * @return a hash code for the transformer's inputs
   */
  static int hashCodeOfUnorderedInputs(ChildProducer<?> transformer) {
    int hashSum = 0x6a3fd14d; // random initial value for sum

    for (Producer<?> producer : transformer.internalAPI().getInputList()) {
      hashSum += producer.hashCode();
    }

    return hashSum;
  }

  /**
   * Returns a copy of the provided transformer with all of its inputs replaced by new {@link Placeholder} instances.
   * This may be used to get a copy of a transformer that is disconnected from its previous DAG.
   *
   * @param transformer the transformer to copy
   * @param <T> the type of the transformer
   * @return a copy of {@code transformer} with all its inputs replaced by new {@link Placeholder}s.
   */
  static <T extends Transformer<?>> T withPlaceholderInputs(T transformer) {
    return ChildProducerInternalAPI.withInputsUnsafe(transformer,
        IntStream.range(0, transformer.internalAPI().getInputList().size())
            .mapToObj(i -> new Placeholder<>("Autogenerated Placeholder for Input #" + (i + 1)))
            .collect(Collectors.toList()));
  }
}
