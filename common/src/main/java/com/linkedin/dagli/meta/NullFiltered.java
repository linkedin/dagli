package com.linkedin.dagli.meta;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.preparer.AbstractPreparerDynamic;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerDynamic;
import com.linkedin.dagli.preparer.PreparerMode;
import com.linkedin.dagli.preparer.PreparerResultMixed;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.reducer.Reducer;
import com.linkedin.dagli.transformer.AbstractPreparableTransformerDynamic;
import com.linkedin.dagli.transformer.AbstractPreparedStatefulTransformerDynamic;
import com.linkedin.dagli.transformer.PreparableTransformer;
import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.util.invariant.Arguments;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;



/**
 * Filters out inputs containing nulls provided to a wrapped preparable transformer.  This can be used to run a
 * transformer on only a subset of examples.  Use NullFilter.Prepared directly if you want to wrap a prepared
 * transformer.
 *
 * By default, an example is filtered if any of the inputs is null, and such examples will not be used to prepare the
 * wrapped transformer nor will the wrapped transformer be applied to them to obtain results.  Instead, the result will
 * be null.
 *
 * For instance, let's say we were using the VectorSum transformer to add pairs of vector inputs, but some of the
 * inputs were null:
 *    [4], [2]
 *    null, [1]
 *    [5], [6]
 *    null, null
 *    [7], null
 *
 * The NullFiltered.Prepared transformer would not apply the VectorSum transformer to any example with a null
 * input (avoiding a NullPointerException) and instead return null in these cases; for the other two examples, it would
 * apply the wrapped transformer and return the result as normal.  The results would be:
 *    [6]
 *    null
 *    [11]
 *    null
 *    null
 */
@ValueEquality
public class NullFiltered<R> extends AbstractPreparableTransformerDynamic<R, PreparedTransformer<R>, NullFiltered<R>> {
  private static final long serialVersionUID = 1;

  private PreparableTransformer<? extends R, ?> _wrapped;
  private boolean _filteredPreparation = true;
  private boolean _filteredApplication = true;
  private R _filteredApplicationResult = null;

  // sets the transformer wrapped by NullFiltered
  private void setWrapped(PreparableTransformer<? extends R, ?> wrapped) {
    _inputs = new ArrayList<>(wrapped.internalAPI().getInputList());
    _wrapped = wrapped;
  }

  public NullFiltered() {
    super();
  }

  public NullFiltered(PreparableTransformer<? extends R, ?> wrapped) {
    super();
    setWrapped(wrapped);
  }

  /**
   * Returns a copy of this instance that will wrap and filter input values passed to the specified transformer.
   *
   * Note that {@link NullFiltered} adopts the wrapped transformer's input nodes as its own, so this will also set
   * {@link NullFiltered}'s inputs.  You don't need to specify inputs on NullFiltered directly.
   *
   * @param wrapped the transformer to be wrapped and whose input values will be null-filtered
   * @return a copy of this instance that will filter input values for the specified transformer
   */
  public NullFiltered<R> withTransformer(PreparableTransformer<? extends R, ?> wrapped) {
    return clone(c -> c.setWrapped(wrapped));
  }

  /**
   * If true (default), if any of the input values are null, the example will not be used to prepare the wrapped
   * transformer and will simply be ignored.
   *
   * If false, all examples will be used to prepare the wrapped transformer, even if they have null input values.
   *
   * @param filteredPreparation whether or not to filter out examples with null input values during preparation
   * @return a copy of this instance with filtering enabled (or not)
   */
  public NullFiltered<R> withFilteredPreparation(boolean filteredPreparation) {
    return clone(c -> c._filteredPreparation = filteredPreparation);
  }

  /**
   * If true (default), if any of the input values are null, the example will not be transformed by the wrapped
   * transformer (after the wrapped transformer is prepared).  Instead, the result of the transformation will be
   * the value specified by withFilteredApplicationResult(...) (null by default).
   *
   * If false, all examples will be processed by the wrapped transformer, even if they have null input values.
   *
   * @param filteredApplication whether or not to use the wrapped transformer to process examples with null input values
   * @return a copy of this instance with filtering enabled (or not)
   */
  public NullFiltered<R> withFilteredApplication(boolean filteredApplication) {
    return clone(c -> c._filteredApplication = filteredApplication);
  }

  /**
   * If an example cannot be processsed by the transformer because it has a null input value (assuming such
   * filtering has not been disabled by calling withFilteredApplication(true)), this will be used as the result instead.
   * By default, this value is null, such that examples with null inputs result in null outputs.
   *
   * @param filteredApplicationResult the value to return as the result of transforming examples with null input values
   * @return a copy of this instance with the specified result for examples with null inputs
   */
  public NullFiltered<R> withFilteredApplicationResult(R filteredApplicationResult) {
    return clone(c -> c._filteredApplicationResult = filteredApplicationResult);
  }

  // tests whether an array of values contains a null
  private static boolean hasNull(Object[] values) {
    for (int i = 0; i < values.length; i++) {
      if (values[i] == null) {
        return true;
      }
    }
    return false;
  }

  @Override
  public PreparerDynamic<R, PreparedTransformer<R>> getPreparer(PreparerContext context) {
    com.linkedin.dagli.preparer.Preparer<? extends R, ?> wrappedPreparer =
        _wrapped.internalAPI().getPreparer(context.withExampleCountLowerBound(0));
    return new Preparer<>(this, wrappedPreparer);
  }

  /**
   * Preparer for a {@link NullFiltered} transformer.
   *
   * @param <R> the type of result produced by the transformer
   */
  private static class Preparer<R> extends AbstractPreparerDynamic<R, PreparedTransformer<R>> {
    private final NullFiltered<R> _owner;
    private final com.linkedin.dagli.preparer.Preparer<? extends R, ?> _wrapped;

    Preparer(NullFiltered<R> owner, com.linkedin.dagli.preparer.Preparer<? extends R, ?> wrapped) {
      Arguments.inSet(wrapped.getMode(),
          () -> "Preparer mode " + wrapped.getMode() + " is unknown to NullFiltered", PreparerMode.BATCH,
          PreparerMode.STREAM);
      _owner = owner;
      _wrapped = wrapped;
    }

    @Override
    public PreparerMode getMode() {
      return _wrapped.getMode();
    }

    @Override
    public void processUnsafe(Object[] values) {
      if (_owner._filteredPreparation && hasNull(values)) {
        return;
      }

      _wrapped.processUnsafe(values);
    }

    @Override
    public PreparerResultMixed<? extends PreparedTransformer<R>, PreparedTransformer<R>> finishUnsafe(
        ObjectReader<Object[]> inputs) {

      PreparerResultMixed<? extends PreparedTransformer<? extends R>, ? extends PreparedTransformer<? extends R>> res =
          _wrapped.finishUnsafe(
              inputs == null || !_owner._filteredPreparation ? inputs : inputs.lazyFilter(values -> !hasNull(values)));

      if (!_owner._filteredApplication) {
        return (PreparerResultMixed<? extends PreparedTransformer<R>, PreparedTransformer<R>>) res;
      }

      return new PreparerResultMixed.Builder<>().withTransformerForPreparationData(
          new Prepared<R>(res.getPreparedTransformerForPreparationData()).withFilteredApplicationResult(
              _owner._filteredApplicationResult))
          .withTransformerForNewData((PreparedTransformer<R>) new Prepared<R>(
              res.getPreparedTransformerForNewData()).withFilteredApplicationResult(_owner._filteredApplicationResult))
          .build();
    }
  }

  /**
   * A NullFiltered transformer for prepared transformers.  If any of the input values for an example are null, the
   * wrapped transformer will not be called and instead a pre-determined result (by default, null) will be returned.
   * Otherwise, the wrapped transformer is called as normal and its result is used instead.
   *
   * See {@link NullFiltered} for more information.
   *
   * @param <R> the type of result
   */
  @ValueEquality
  public static class Prepared<R> extends AbstractPreparedStatefulTransformerDynamic<R, Object, Prepared<R>> {
    private static final long serialVersionUID = 1;
    // if any input is a constant null, this producer can be replaced by a constant null
    private static final List<Reducer<Prepared<?>>> REDUCERS = Collections.singletonList(
        (target, context) -> {
          List<? extends Producer<?>> parents = context.getParents(target);
          for (Producer<?> parent : parents) {
            if (parent instanceof Constant && ((Constant) parent).getValue() == null) {
              context.replace(target, Constant.nullValue());
            }
          }
        });

    private PreparedTransformer<? extends R> _wrapped;
    private R _filteredApplicationResult = null;

    // sets the wrapped transformer
    private void setWrapped(PreparedTransformer<? extends R> wrapped) {
      _inputs = new ArrayList<>(wrapped.internalAPI().getInputList()); // inherit the wrapped transformer's inputs
      _wrapped = wrapped; // remember the wrapped transformer
    }

    /**
     * Creates a new instance; withTransformer(...) must be called to specify the wrapped transformer prior to use.
     */
    public Prepared() {
      super();
    }

    /**
     * Creates a new instance that will wrap the specified transformer, filtering out null inputs
     *
     * @param wrapped the transformer to wrap
     */
    public Prepared(PreparedTransformer<? extends R> wrapped) {
      super();
      setWrapped(wrapped);
    }

    @Override
    protected Collection<? extends Reducer<? super Prepared<R>>> getGraphReducers() {
      return REDUCERS;
    }

    /**
     * Returns a copy of this instance that will wrap and filter input values passed to the specified transformer.
     *
     * Note that {@link NullFiltered.Prepared} adopts the wrapped transformer's input nodes as its own, so this will
     * also set {@link NullFiltered.Prepared}'s inputs.  You don't need to specify inputs directly.
     *
     * @param wrapped the transformer to be wrapped and whose input values will be null-filtered
     * @return a copy of this instance that will filter input values for the specified transformer
     */
    public Prepared<R> withTransformer(PreparedTransformer<? extends R> wrapped) {
      return clone(c -> c.setWrapped(wrapped));
    }

    /**
     * If an example cannot be processsed by the transformer because it has a null input value, this will be used as the
     * result instead.  By default, this value is null, such that examples with null inputs result in null outputs.
     *
     * @param filteredApplicationResult the value to return as the result of transforming examples with null input values
     * @return a copy of this instance with the specified result for examples with null inputs
     */
    public Prepared<R> withFilteredApplicationResult(R filteredApplicationResult) {
      return clone(c -> c._filteredApplicationResult = filteredApplicationResult);
    }

    @Override
    protected void applyAll(Object executionCache, List<? extends List<?>> values, List<? super R> results) {
      int count = values.get(0).size();
      boolean[] hasNull = new boolean[count];

      for (List<?> inputValueList : values) {
        for (int i = 0; i < count; i++) {
          hasNull[i] = hasNull[i] || (inputValueList.get(i) == null);
        }
      }

      int nonNullCount = 0;
      for (boolean val : hasNull) {
        nonNullCount += (val ? 0 : 1);
      }

      Object[][] minibatch = new Object[values.size()][nonNullCount];
      int minibatchOffset = 0;
      for (int i = 0; i < count; i++) {
        if (!hasNull[i]) {
          for (int j = 0; j < values.size(); j++) {
            minibatch[j][minibatchOffset] = values.get(j).get(i);
          }
          minibatchOffset++;
        }
      }

      Object[] nonNullResults = new Object[nonNullCount];
      _wrapped.internalAPI().applyAllUnsafe(executionCache, nonNullCount, minibatch, nonNullResults);

      int minibatchResultsOffset = 0;
      for (int i = 0; i < count; i++) {
        results.add(hasNull[i] ? _filteredApplicationResult : (R) nonNullResults[minibatchResultsOffset++]);
      }
    }

    @Override
    protected Object createExecutionCache(long exampleCountGuess) {
      return _wrapped.internalAPI().createExecutionCache(exampleCountGuess);
    }

    @Override
    protected int getPreferredMinibatchSize() {
      return _wrapped.internalAPI().getPreferredMinibatchSize();
    }

    @Override
    public R apply(Object executionCache, List<?> values) {
      if (values.contains(null)) {
        return _filteredApplicationResult;
      } else {
        return _wrapped.internalAPI().applyUnsafe(executionCache, values);
      }
    }
  }
}