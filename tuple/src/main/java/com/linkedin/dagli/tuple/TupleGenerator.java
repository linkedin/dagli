package com.linkedin.dagli.tuple;

import java.util.Iterator;


/**
 * Tuple generators create tuples of a fixed dimension from a variety of inputs.  Note that these methods are always
 * equivalent to the static methods available on the TupleX interfaces; the benefit of TupleGenerators is that they
 * provide an efficient way to generate many tuples of a given size when the tuple size is only known at runtime.
 *
 * You can get the generator for a specific size by calling Tuple.generator(size).
 */
public interface TupleGenerator {
  /**
   * @return the size of tuples created by this generator
   */
  int size();

  /**
   * Creates a new tuple, taking ownership of the array (which should not be subsequently modified.)
   *
   * @param elements an array of elements that will back the returned tuple; must contain at least size() items.
   * @return a new tuple with the elements provided in the given array
   */
  Tuple fromArray(Object[] elements);

  /**
   * Creates a new tuple from a given iterable.
   *
   * @param elements an iterable of elements that will back the returned tuple; must contain at least size() items.
   * @return a new tuple with the elements provided
   */
   Tuple fromIterable(Iterable<?> elements);

  /**
   * Creates a new tuple from a given iterator.
   *
   * @param elements an iterator of elements that will back the returned tuple; must contain at least size() items.
   * @return a new tuple with the elements provided
   */
   Tuple fromIterator(Iterator<?> elements);
}
