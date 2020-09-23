package com.linkedin.dagli.object;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.preparer.AbstractStreamPreparer1;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.Preparer1;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparableTransformer1;
import com.linkedin.dagli.transformer.ConstantResultTransformation1;


/**
 * Base class for transformers that produce a single value determined by comparing its inputs.
 *
 * @param <T> the type of the objects being compared (and the type of object that is produced) should be found
 */
@ValueEquality
abstract class AbstractMinMaxTransformer<T, S extends AbstractMinMaxTransformer<T, S>>
    extends AbstractPreparableTransformer1<T, T, ConstantResultTransformation1.Prepared<T, T>, S> {
  private static final long serialVersionUID = 1;

  boolean _nullIfAnyInputNull = false;

  @Override
  protected boolean hasIdempotentPreparer() {
    return true;
  }

  @Override
  protected boolean hasAlwaysConstantResult() {
    return true;
  }

  /**
   * Returns a copy of this transformer that will accept input values from the provided producer.
   *
   * @param input the producer providing the input values
   * @return a copy of this transformer that will accept input values from the provided producer
   */
  public S withInput(Producer<? extends T> input) {
    return withInput1(input);
  }

  /**
   * Returns a copy of this instance that will produce null if <strong>any</strong> of the input values seen during
   * preparation are null.  The default behavior is to return null only if <strong>all</strong> of the input values are
   * null.
   *
   * @return a copy of this instance that will produce null if any of the input values seen during preparation are null
   */
  public S withNullResultIfAnyInputValueIsNull() {
    return clone(c -> c._nullIfAnyInputNull = true);
  }

  /**
   * Returns true if the transformer prefers to return the first value rather than the second (e.g. if the derived class
   * is finding the minimum, the transformer would return true if the first object is "less than" the second.
   *
   * If the two objects are equally "good", either true or false may be returned.
   *
   * @param first the first object being compared; this will never be null
   * @param second the second object being compared; this will never be null
   * @return whether or not the first object is preferred to the second
   */
  abstract boolean isFirstPreferred(T first, T second);

  @Override
  protected Preparer1<T, T, ConstantResultTransformation1.Prepared<T, T>> getPreparer(PreparerContext context) {
    return new Preparer();
  }

  private class Preparer extends AbstractStreamPreparer1<T, T, ConstantResultTransformation1.Prepared<T, T>> {
    private T _currentBest = null;
    private boolean _isFirstValue = false;

    @Override
    public PreparerResult<ConstantResultTransformation1.Prepared<T, T>> finish() {
      return new PreparerResult<>(new ConstantResultTransformation1.Prepared<T, T>().withResult(_currentBest));
    }

    @Override
    public void process(T other) {
      if (_isFirstValue) {
        _currentBest = other;
        _isFirstValue = false;
      } else if (_currentBest == null) {
        _currentBest = _nullIfAnyInputNull ? null : other;
      } else if (other == null) {
        _currentBest = _nullIfAnyInputNull ? null : _currentBest;
      } else {
        if (isFirstPreferred(other, _currentBest)) {
          _currentBest = other;
        }
      }
    }
  }
}
