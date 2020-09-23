package com.linkedin.dagli.object;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.reducer.Reducer;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer3;
import java.util.Collection;
import java.util.Collections;
import java.util.List;



/**
 * Transformer that provides a value conditional on a boolean condition, equivalent to:
 * condition ? value1 : value2
 *
 * The condition, value1, and value2 are all inputs to the transformer, although value1 and value2 are often constants
 * in practice (with the class providing convenience methods to accommodate this).
 *
 * @param <T> the type of value produced by this transformer
 */
@ValueEquality
public class ConditionalValue<T> extends AbstractPreparedTransformer3<Boolean, T, T, T, ConditionalValue<T>> {
  private static final long serialVersionUID = 1;
  // if the condition input is a constant, we can replace this producer with one of the other inputs
  @SuppressWarnings("unchecked")
  private static final List<Reducer<ConditionalValue<?>>> REDUCERS = Collections.singletonList(
      (target, context) -> {
        List<? extends Producer<?>> parents = context.getParents(target);
        if (parents.get(0) instanceof Constant) {
          context.replace(target,
              (Producer) (((Constant<Boolean>) parents.get(0)).getValue() ? parents.get(1) : parents.get(2)));
        }
      });

  public ConditionalValue() {
    super(MissingInput.get(), new Constant<>(null), new Constant<>(null));
  }

  @Override
  protected Collection<? extends Reducer<? super ConditionalValue<T>>> getGraphReducers() {
    return REDUCERS;
  }

  /**
   * @param condition the producer providing the boolean to condition upon
   * @return a copy of this instance that will condition its return value on the provided boolean input.
   */
  public ConditionalValue<T> withConditionInput(Producer<? extends Boolean> condition) {
    return super.withInput1(condition);
  }

  /**
   * Returns a copy of this instance that will yield the specified input when the condition value is true.
   *
   * By default, the yielded value is null.
   *
   * @param value the value that will be produced if the condition value is true
   * @return a copy of this instance that will yield the specified input when the condition value is true
   */
  public ConditionalValue<T> withValueIfConditionTrueInput(Producer<? extends T> value) {
    return super.withInput2(value);
  }

  /**
   * Returns a copy of this instance that will yield the specified input when the condition value is true.
   *
   * By default, the yielded value is null.
   *
   * @param value the value that will be produced if the condition value is true
   * @return a copy of this instance that will yield the specified input when the condition value is true
   */
  public ConditionalValue<T> withValueIfConditionTrue(T value) {
    return withValueIfConditionTrueInput(new Constant<>(value));
  }

  /**
   * Returns a copy of this instance that will yield the specified input when the condition value is false.
   *
   * By default, the yielded value is null.
   *
   * @param value the value that will be produced if the condition value is false
   * @return a copy of this instance that will yield the specified input when the condition value is false
   */
  public ConditionalValue<T> withValueIfConditionFalseInput(Producer<? extends T> value) {
    return super.withInput3(value);
  }

  /**
   * Returns a copy of this instance that will yield the specified input when the condition value is false.
   *
   * By default, the yielded value is null.
   *
   * @param value the value that will be produced if the condition value is false
   * @return a copy of this instance that will yield the specified input when the condition value is false
   */
  public ConditionalValue<T> withValueIfConditionFalse(T value) {
    return withValueIfConditionFalseInput(new Constant<>(value));
  }

  @Override
  public T apply(Boolean value0, T value1, T value2) {
    return value0 ? value1 : value2;
  }
}
