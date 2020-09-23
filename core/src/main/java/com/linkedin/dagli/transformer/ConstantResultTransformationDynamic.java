package com.linkedin.dagli.transformer;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerDynamic;
import com.linkedin.dagli.preparer.TrivialPreparerDynamic;
import java.util.List;
import java.util.Objects;


/**
 * A trivial implementation of a preparable transformer that ignores its inputs and provides a constant result.
 *
 * @param <R> the type of the constant result that will be returned by the prepared transformer.
 */
@ValueEquality(commutativeInputs = true)
public final class ConstantResultTransformationDynamic<R> extends
  AbstractPreparableTransformerDynamic<R, ConstantResultTransformationDynamic.Prepared<R>, ConstantResultTransformationDynamic<R>>
  implements ConstantResultTransformation<R, ConstantResultTransformationDynamic<R>> {
  private static final long serialVersionUID = 1;

  // the object instances that will be the constant results of this transformer
  private R _resultForNewData = null;
  private R _resultForPreparationData = null;

  /**
   * Creates a new trivial preparable transformer that will ignore its inputs and will always has a null result.
   */
  public ConstantResultTransformationDynamic() { }

  @Override
  protected boolean hasAlwaysConstantResult() {
    return true;
  }

  @Override
  protected boolean hasIdempotentPreparer() {
    return true;
  }

  /**
   * Returns a copy of this transformer that will always have the specified constant result object for both new and
   * preparation data.  Note that this exact object will be returned every time the transformer is applied (and not a
   * clone).
   *
   * @param result the result that will always be returned by this transformer
   * @return a copy of this transformer that will always have the specified constant result
   */
  public ConstantResultTransformationDynamic<R> withResult(R result) {
    return clone(c -> {
      c._resultForNewData = result;
      c._resultForPreparationData = result;
    });
  }

  /**
   * Returns a copy of this transformer that will always have the specified constant result object for preparation data.
   * Note that this exact object will be returned every time the transformer is applied (and not a clone).
   *
   * @param result the result that will always be returned by this transformer for preparation data
   * @return a copy of this transformer that will always have the specified constant result
   */
  public ConstantResultTransformationDynamic<R> withResultForPreparationData(R result) {
    return clone(c -> {
      c._resultForPreparationData = result;
    });
  }

  /**
   * Returns a copy of this transformer that will always have the specified constant result object for new data.
   * Note that this exact object will be returned every time the transformer is applied (and not a clone).
   *
   * @param result the result that will always be returned by this transformer for new data
   * @return a copy of this transformer that will always have the specified constant result
   */
  public ConstantResultTransformationDynamic<R> withResultForNewData(R result) {
    return clone(c -> {
      c._resultForNewData = result;
    });
  }

  /**
   * @return the object instance that will be produced by this transformer for all new examples.
   */
  public R getResultForNewData() {
    return _resultForNewData;
  }

  /**
   * @return the object instance that will be produced by this transformer for all preparation examples.
   */
  public R getResultForPreparationData() {
    return _resultForPreparationData;
  }

  @Override
  protected PreparerDynamic<R, Prepared<R>> getPreparer(PreparerContext context) {
    Prepared<R> resultForPrepData = new Prepared<>(_resultForPreparationData);
    return _resultForPreparationData == _resultForNewData ? new TrivialPreparerDynamic<>(resultForPrepData)
        : new TrivialPreparerDynamic<>(resultForPrepData, new Prepared<>(_resultForNewData));
  }

  @Override
  public String getName() {
    return "ConstantResultTransformationDynamic(" + (Objects.equals(_resultForNewData, _resultForPreparationData)
        ? _resultForNewData : ("prep = " + _resultForPreparationData + ", new = " + _resultForNewData)) + ")";
  }

  /**
   * A trivial implementation of a prepared transformer that ignores its inputs and provides a constant result.
   *
   * @param <R> the type of the constant result that will be returned by the transformer.
   */
  @ValueEquality
  public static final class Prepared<R> extends AbstractPreparedTransformerDynamic<R, Prepared<R>>
      implements ConstantResultTransformation.Prepared<R, Prepared<R>> {
    private static final long serialVersionUID = 1;

    // the object instance that will be the constant result of this transformer
    private R _result = null;

    @Override
    protected boolean hasAlwaysConstantResult() {
      return true;
    }

    /**
     * Creates a new trivial prepared transformer that will ignore its inputs and always have a null result.
     */
    public Prepared() { }

    /**
     * Creates a new trivial prepared transformer that will ignore its inputs and always have the given result.
     *
     * @param result the result to be "produced"
     */
    public Prepared(R result) {
      _result = result;
    }

    /**
     * Returns a copy of this transformer that will always have the specified constant result object.  Note that this
     * exact result object will be returned every time the transformer is applied (and not a clone).
     *
     * @param result the result that will always be returned when applying the transformer
     * @return a copy of this transformer that will always have the specified constant result
     */
    public Prepared<R> withResult(R result) {
      return clone(c -> c._result = result);
    }

    /**
     * Gets the object instance that will be produced by this transformer for all inputs.
     *
     * @return the object instance that will be produced by this transformer for all inputs.
     */
    public R getResult() {
      return _result;
    }

    @Override
    protected R apply(List<?> values) {
      return _result;
    }

    @Override
    public String getName() {
      return "ConstantResultTransformationDynamic.Prepared(" + _result + ")";
    }
  }
}
