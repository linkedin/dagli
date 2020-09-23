package com.linkedin.dagli.object;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.function.FunctionResult2;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.generator.ExampleIndex;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.reducer.Reducer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformerDynamic;
import com.linkedin.dagli.util.invariant.Arguments;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Transformer that logs its inputs to assist with the debugging of a DAG.
 *
 * The result of this transformer is always its first input, unmodified.  Like any transformer, {@link Logged} must be
 * the ancestor of an output for it to be included in the DAG (and thus output anything during execution).
 *
 * By default, the logger logs the values for the first 5 examples only.
 *
 * @param <R> the type of result of the DAG, which must also be the type of the first input.
 */
@ValueEquality
public class Logged<R> extends AbstractPreparedTransformerDynamic<R, Logged<R>> {
  private static final long serialVersionUID = 1;
  private static final Logger LOGGER = LogManager.getLogger();
  private static final int LOGGING_CONDITION_INPUT = 0;
  private static final int EXAMPLE_INDEX_INPUT = 1;
  private static final int FIRST_LOGGED_INPUT = 2;

  private Level _logLevel = Level.INFO;
  private boolean _removeFromGraph = false;
  private boolean _removeFromPreparedGraph = false;

  @Override
  @SuppressWarnings("unchecked")
  protected Collection<? extends Reducer<? super Logged<R>>> getGraphReducers() {
    return _removeFromGraph || _removeFromPreparedGraph ? Collections.singleton(
        Reducer.Level.ESSENTIAL.with((target, context) -> {
          if (_removeFromGraph || context.isPreparedDAG()) {
            context.replace(target, (Producer<R>) context.getParents(target).get(FIRST_LOGGED_INPUT));
          }
        })) : Collections.emptyList();
  }

  public Logged() {
    super(Compare.is(new ExampleIndex()).lessThan(new Constant<>(5L)));
  }

  /**
   * Creates a new instance that will log the provided inputs.  The result of the transformer will be the first input
   * value, unmodified.
   *
   * @param firstInput the first input to log
   * @param remainingInputs zero or more additional inputs to log
   */
  public Logged(Producer<? extends R> firstInput, Producer<?>... remainingInputs) {
    this();
    setInputs(firstInput, remainingInputs);
  }

  private void setInputs(Producer<? extends R> firstInput, Producer<?>... remainingInputs) {
    Producer<?> loggingConditionInput = _inputs.get(0);
    _inputs = new ArrayList<>(remainingInputs.length + 3);
    _inputs.add(loggingConditionInput);
    _inputs.add(new ExampleIndex());
    _inputs.add(firstInput);
    _inputs.addAll(Arrays.asList(remainingInputs));
  }

  /**
   * Returns a copy that, if {@code remove} is true, will <strong>always</strong> be removed from its containing DAG
   * when the DAG is reduced.  This means that, unless reduction is disabled for the DAG, <strong>no</strong> logging
   * will be performed.
   *
   * The purpose of this method is to provide a way to easily disable logging without having to change the structure
   * of the defined DAG (simply removing the {@link Logged} instance from the graph will also disable logging, of
   * course, but may be less convenient.)
   *
   * @param remove whether or not this instance should <strong>always</strong> be removed from its containing DAG when
   *               the DAG is reduced (DAGs are reduced by default unless explicitly disabled)
   * @return a copy that, if {@code remove} is true, will <strong>always</strong> be removed from its containing DAG
   *         when the DAG is reduced
   */
  public Logged<R> withRemovalFromGraph(boolean remove) {
    return clone(c -> c._removeFromGraph = remove);
  }

  /**
   * Returns a copy that, if {@code remove} is true, will always be removed from its containing DAG when the DAG is
   * reduced, <strong>if</strong> the DAG is prepared.  This allows the use of logging during the preparation (training)
   * of a DAG, while the resulting prepared (trained) DAG will perform no logging (and, because the {@link Logged}
   * instance is entirely removed from the prepared graph, incur zero computational cost.)
   *
   * @param remove whether or not this instance should be removed from a containing <strong>prepared</strong> DAG when
   *               the DAG is reduced (DAGs are reduced by default unless explicitly disabled)
   * @return a copy that, if {@code remove} is true, will always be removed from its containing DAG when the DAG is
   *         reduced, <strong>if</strong> the DAG is prepared
   */
  public Logged<R> withRemovalFromPreparedGraph(boolean remove) {
    return clone(c -> c._removeFromPreparedGraph = remove);
  }

  /**
   * Returns a copy that will always be removed from its containing DAG when the DAG is reduced, <strong>if</strong> the
   * DAG is prepared.  This allows the use of logging during the preparation (training) of a DAG, while the resulting
   * prepared (trained) DAG will perform no logging (and, because the {@link Logged} instance is entirely removed from
   * the prepared graph, incur zero computational cost.)
   *
   * @return a copy that will always be removed from its containing DAG when the DAG is reduced, <strong>if</strong> the
   *         DAG is prepared
   */
  public Logged<R> withRemovalFromPreparedGraph() {
    return withRemovalFromPreparedGraph(true);
  }

  /**
   * Returns a copy of this instance that will log at the specified logging level.
   *
   * The default logging level is {@link Level#INFO}.
   *
   * @param logLevel the logging level to use
   * @return a copy of this instance that will log at the specified logging level
   */
  public Logged<R> withLogLevel(Level logLevel) {
    return clone(c -> c._logLevel = Objects.requireNonNull(logLevel));
  }

  /**
   * Returns a copy of this instance that will log an example if and only if the condition value provided by the given
   * producer is true for that example.
   *
   * The default logging condition is to log the first 5 examples only.
   *
   * @param condition the condition that determines whether or not the (other) inputs should be logged for this example
   * @return a copy of this instance that will log an example if and only if the condition value provided by the given
   *         producer is true for that example
   */
  public Logged<R> withLoggingCondition(Producer<Boolean> condition) {
    return clone(c -> {
      c._inputs = new ArrayList<>(this._inputs);
      c._inputs.set(LOGGING_CONDITION_INPUT, condition);
    });
  }

  /**
   * @param k the number of examples to log
   * @return a copy of this instance that will log its inputs for the first {@code k} examples (only)
   */
  public Logged<R> withLoggingFirst(long k) {
    return withLoggingCondition(Compare.is(new ExampleIndex()).lessThan(new Constant<>(k)));
  }

  /**
   * @param k the inputs for an example will be logged iff the example index is a multiple of this value
   * @return a copy of this instance that will log its inputs every {@code k} examples; the first example will always
   *         be logged
   */
  public Logged<R> withLoggingEvery(long k) {
    Arguments.check(k >= 1, "k must be >= 1");

    return withLoggingCondition(Compare.is(
        new FunctionResult2<Long, Long, Long>(Math::floorMod).withInputs(new ExampleIndex(), new Constant<>(k)))
        .equalTo(new Constant<>(0)));
  }

  /**
   * Returns a copy of this instance that will log the values of the provided inputs (and return the first input value,
   * unmodified, as the result of this {@link Logged} instance.)
   *
   * @param firstInput the first input
   * @param remainingInputs zero or more additional inputs to log
   * @return a copy of this instance that will log the values of the provided inputs
   */
  public Logged<R> withInputs(Producer<? extends R> firstInput, Producer<?>... remainingInputs) {
    return clone(c -> c.setInputs(firstInput, remainingInputs));
  }

  @Override
  @SuppressWarnings("unchecked")
  protected R apply(List<?> values) {
    if (Boolean.TRUE.equals(values.get(LOGGING_CONDITION_INPUT))) {
      long exampleIndex = (Long) values.get(EXAMPLE_INDEX_INPUT);
      List<? extends Producer<?>> parents = getInputList();
      for (int i = FIRST_LOGGED_INPUT; i < values.size(); i++) {
        final int inputIndex = i;
        LOGGER.log(_logLevel,
            () -> "Example #" + exampleIndex + " / Input #" + (inputIndex - FIRST_LOGGED_INPUT) + " (" + parents.get(
                inputIndex).getName() + ") = " + values.get(inputIndex));
      }
    }

    return (R) values.get(FIRST_LOGGED_INPUT);
  }

  @Override
  public String getName() {
    return "Logged(" + getInputList().stream().map(Producer::getShortName).collect(Collectors.joining(", ")) + ")";
  }

  @Override
  public String getShortName() {
    return "Logged(...)";
  }
}
