package com.linkedin.dagli.assorted;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import java.util.Collections;
import java.util.List;


/**
 * Implements a trivial transformer that creates a single-element list from the input element.
 *
 * Notice the naming convention for transformer classes: the class name is also the name of the result of the
 * transformation.  This helps make DAG specifications more readable.
 *
 * The arguments to the base {@link AbstractPreparedTransformer1WithInput} class provide the type of the input, the type
 * of the result, and finally the SingleElementList type itself; this last parameter is used as part of a "curiously
 * recurring template pattern" (CRTP) that allows methods defined on the base class to use the descendant's type.
 *
 * In "real" code, rather than define a new transformer to invoke a publicly available, concrete method, we could use a
 * {@link com.linkedin.dagli.function.FunctionResult1} to wrap the method instead.
 *
 * @param <T> the type of element to be wrapped in a list
 */
@ValueEquality // transformers are compared by value-equality (field-by-field) by default, but you'll see a compilation
               // message suggesting that you make your intention explicit if you don't add this annotation; equality
               // checking is important because it helps de-duplicate node in the graph and avoid redundant work
public class SingleElementList<T> extends AbstractPreparedTransformer1WithInput<T, List<T>, SingleElementList<T>> {
  // to ensure consistent serialization, it's a good idea to define an (arbitrary) version ID:
  private static final long serialVersionUID = 0xDEADBEEF;

  @Override
  public List<T> apply(T value0) {
    return Collections.singletonList(value0);
  }
}
