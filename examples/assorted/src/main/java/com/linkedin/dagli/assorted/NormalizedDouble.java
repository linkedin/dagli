package com.linkedin.dagli.assorted;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.preparer.AbstractStreamPreparer1;
import com.linkedin.dagli.preparer.Preparer1;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.transformer.AbstractPreparableTransformer1WithInput;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1WithInput;
import com.linkedin.dagli.util.invariant.Arguments;


/**
 * Implements a very common normalization to inputted {@code Double} values by scaling and shifting them to be between
 * 0 and 1.
 */
@ValueEquality // compare instances based on the equality of their fields and their inputs (only the latter applies in
               // this case, since we do not declare any additional fields).
// AbstractPreparableTransformer1WithInput extends AbstractPreparableTransformer1, conveniently adding a
// withInput(Producer<? extend Double>) method for us so we don't have to declare it ourselves.  Since the transformer
// has arity 1, four generic type arguments are required:
// (1) The type of the input (Double)
// (2) The type of the output/result (Double)
// (3) The type of the prepared transformer that this preparable transformer will prepare to (NormalizedDouble.Prepared)
// (4) The type of this class (NormalizedDouble) so that certain methods of our ancestor classes can be correctly typed
//     to return instances of this type (analogous to the "curiously recurring template pattern" in C++)
public class NormalizedDouble
    extends AbstractPreparableTransformer1WithInput<Double, Double, NormalizedDouble.Prepared, NormalizedDouble> {
  private static final long serialVersionUID = 1;

  // This is the key method of a PreparableTransformer: it returns a Preparer that will then be fed preparation data
  // to create a prepared transformer that can ultimately be used to transformer both the preparation data and any future
  // examples in the prepared DAG.
  @Override
  protected Preparer1<Double, Double, Prepared> getPreparer(PreparerContext context) {
    return new Preparer();
  }

  /**
   * Implements a stream preparer that will, when finished, yield a {@link Prepared} transformer instance.  Since we
   * only need to see the preparation data once, we don't need to use a (more computationally expensive) batch preparer.
   */
  public static class Preparer extends AbstractStreamPreparer1<Double, Double, Prepared> {
    private double _max = Double.NEGATIVE_INFINITY;
    private double _min = Double.POSITIVE_INFINITY;

    // Processes the input for each example, allowing us to find the minimum and maximum Double values across all data
    @Override
    public void process(Double value) {
      _max = Math.max(value, _max);
      _min = Math.min(value, _min);
    }

    // As is almost always the case, the prepared transformer that should be used to transform the preparation examples
    // is the same as the one that should be used to transform future examples in the prepared DAG, so we can return
    // a PreparerResult.  A PreparerResultMixed could instead be used if we wanted to return two different transformers
    // of two different types.
    @Override
    public PreparerResult<Prepared> finish() {
      if (_max < _min) {
        // special case: we saw no examples!  Ordinarily this wouldn't happen, but it's possible to wrap transformers
        // to exclude some inputs (e.g. NullFiltered) which may create this situation
        _min = 0; // use arbitrary (but valid) defaults
        _max = 1;
      }

      // We return a PreparerResult with a single prepared transformer that will be used to transformer *all* examples
      // (both preparation data and "new", future data).  Note that we are *not* responsible for configuring the
      // returned transformer's inputs (Dagli will do this for us).
      return new PreparerResult<>(new Prepared().withMin(_min).withMax(_max));
    }
  }

  /**
   * A transformer that normalizes values to be between 0 and 1, inclusive, if the expected minimum and maximum of all
   * the inputted values are provided (any values below the minimum or above the maximum will be clipped to 0 or 1,
   * respectively).
   */
  @ValueEquality
  public static class Prepared extends AbstractPreparedTransformer1WithInput<Double, Double, Prepared> {
    private static final long serialVersionUID = 1;

    // note that our initial max/min values are (intentionally) invalid
    private double _max = Double.NEGATIVE_INFINITY;
    private double _min = Double.POSITIVE_INFINITY;

    // validate() can be used to check for an invalid configuration that can't be detected until configuration of the
    // transformer is complete (and it's being included in a DAG).
    @Override
    public void validate() {
      super.validate();
      Arguments.check(_min <= _max, "Min value must be <= to Max value");
    }

    /**
     * @param max the maximum input value to expect for the purposes of calculating the correct shift/scale amounts
     * @return a copy of this transformer that will use the provided maximum value to calculate shifting/scaling
     */
    public Prepared withMax(double max) {
      Arguments.check(!Double.isNaN(max), "Max cannot be NaN");
      return clone(c -> c._max = max);
    }

    /**
     * @param min the minimum input value to expect for the purposes of calculating the correct shift/scale amounts
     * @return a copy of this transformer that will use the provided minimum value to calculate shifting/scaling
     */
    public Prepared withMin(double min) {
      Arguments.check(!Double.isNaN(min), "Min cannot be NaN");
      return clone(c -> c._min = min);
    }

    @Override
    public Double apply(Double value) {
      if (_min == _max) {
        return 0.0;
      }
      double shiftedValue = value - _min;
      double scaledValue = shiftedValue / (_max - _min);

      // value might be lower or higher than any value we saw during preparation, so we still need to constrain it to
      // make sure the result is always between 0 and 1 (inclusive):
      return Math.min(1, Math.max(0, scaledValue));
    }
  }
}
