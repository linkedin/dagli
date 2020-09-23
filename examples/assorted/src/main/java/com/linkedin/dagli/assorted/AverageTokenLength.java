package com.linkedin.dagli.assorted;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;


/**
 * Implements a simple transformer that computes the average string length from a list of strings.
 *
 * The average length of an empty list is defined to be 0.
 *
 * Notice the naming convention for transformer classes: the class name is also the name of the result of the
 * transformation.  This helps make DAG specifications more readable.
 *
 * The arguments to the base {@link AbstractPreparedTransformer1WithInput} class provide the type of the input, the type
 * of the result, and finally the AverageTokenLength type itself; this last parameter is used as part of a "curiously
 * recurring template pattern" (CRTP) that allows methods defined on the base class to use the descendant's type.
 */
@ValueEquality // transformers are compared by value-equality (field-by-field) by default, but you'll see a compilation
               // message suggesting that you make your intention explicit if you don't add this annotation; equality
               // checking is important because it helps de-duplicate node in the graph and avoid redundant work
public class AverageTokenLength
    extends AbstractPreparedTransformer1WithInput<Iterable<? extends CharSequence>, Double, AverageTokenLength> {
  // to ensure consistent serialization, it's a good idea to define an (arbitrary) version ID:
  private static final long serialVersionUID = 0xDEADBEEF;

  @Override
  public Double apply(Iterable<? extends CharSequence> list) {
    long sum = 0;
    long count = 0;
    for (CharSequence text : list) {
      count++;
      sum += text.length();
    }
    return ((double) sum) / count;
  }
}
