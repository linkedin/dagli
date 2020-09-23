package com.linkedin.dagli.object;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.map.DictionaryValue;
import com.linkedin.dagli.preparer.AbstractStreamPreparer1;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.transformer.AbstractPreparableTransformer1WithInput;
import com.linkedin.dagli.transformer.PreparedTransformer1;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.HashMap;


/**
 * Determines the number of times each inputted object occurs in the preparation data.  E.g. if the example inputs are:
 * a
 * a
 * a
 * b
 * b
 * c
 *
 * The results will be:
 * 3
 * 3
 * 3
 * 2
 * 2
 * 1
 *
 * The name of this transformer comes from the concept of multiplicity in multisets (in a multiset, a unique item's
 * /multiplicity/ is the number of times that item occurs in the multiset).
 *
 * If the prepared transformer is applied to an input not seen during preparation, the multiplicity will, of course, be
 * 0.
 */
@ValueEquality
public class Multiplicity
    extends AbstractPreparableTransformer1WithInput<Object, Long, PreparedTransformer1<Object, Long>, Multiplicity> {
  private static final long serialVersionUID = 1;

  @Override
  protected Preparer getPreparer(PreparerContext context) {
    return new Preparer();
  }

  /**
   * Preparer for {@link Multiplicity} instances.
   */
  private static class Preparer extends AbstractStreamPreparer1<Object, Long, PreparedTransformer1<Object, Long>> {
    private final Object2LongOpenHashMap<Object> _counts = new Object2LongOpenHashMap<>();

    @Override
    public PreparerResult<PreparedTransformer1<Object, Long>> finish() {
      // in case you're wondering why we convert the open hash map with primitive values (lower memory consumption)
      // to a HashMap, it's because the HashMap's values may be returned many times; it's better to box each long
      // once to return an indefinite number of clones of that Long by recreating it on each input.
      return new PreparerResult<PreparedTransformer1<Object, Long>>(
          new DictionaryValue<Long>().withDictionary(new HashMap<>(_counts)).withDefaultValue(0L));
    }

    @Override
    public void process(Object value0) {
      _counts.addTo(value0, 1);
    }
  }
}
