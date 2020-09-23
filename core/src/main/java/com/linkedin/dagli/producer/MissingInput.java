package com.linkedin.dagli.producer;

import java.util.Arrays;
import java.util.List;


/**
 * The MissingInput "producer" singleton is used to mark references to input producers that have not yet been assigned.
 *
 * When you create a transformer, you need to specify its inputs before it's used, e.g.
 * {@code new LowerCased().withInput(someStringProducer);}
 *
 * However, if you fail to do so, that (unassigned) input will have a reference to the MissingInput instead,
 * and this will be detected and used to generate a (hopefully informative) exception to this effect.
 *
 * @param <R> the type that the input is expected to have
 */
public interface MissingInput<R> extends Producer<R> {
  /**
   * Gets a reference to the MissingInput singleton.
   *
   * @param <R> the type that the input is expected to have
   * @return the MissingInput singleton
   */
  @SuppressWarnings("unchecked")
  static <R> MissingInput<R> get() {
    return (MissingInput) MissingInputInstance.INSTANCE;
  }

  /**
   * Creates a list of MissingInputs (these will all be the same object instance)
   *
   * @param arity the desired number of MissingInputs
   * @param <I> the expected type of the input
   * @return a list of MissingInputs
   */
  static <I> List<MissingInput<I>> list(int arity) {
    return Arrays.asList(array(arity));
  }

  /**
   * Creates a list of MissingInputs (these will all be the same object instance)
   *
   * @param arity the desired number of MissingInputs
   * @return a list of MissingInputs
   */
  static List<Producer<?>> producerList(int arity) {
    return Arrays.asList(array(arity));
  }

  /**
   * Creates an array of MissingInputs (these will all be the same object instance)
   *
   * @param arity the desired number of MissingInputs
   * @param <I> the expected type of the input
   * @return an array of MissingInputs
   */
  static <I> MissingInput<I>[] array(int arity) {
    MissingInput[] list = new MissingInput[arity];
    Arrays.fill(list, MissingInput.get());
    return list;
  }
}
