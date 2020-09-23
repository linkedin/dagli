package com.linkedin.dagli.tester;

import com.linkedin.dagli.dag.DAGTransformer;
import com.linkedin.dagli.dag.PreparedDAGTransformer;
import com.linkedin.dagli.reducer.Reducer;
import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.util.array.ArraysEx;
import com.linkedin.dagli.util.collection.Iterables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;


/**
 * Tests {@link PreparedTransformer} nodes.
 *
 * @param <R> the type of result of the transformer
 * @param <T> the type of the transformer
 */
public final class PreparedTransformerTestBuilder<R, T extends PreparedTransformer<R>>
    extends AbstractTransformerTestBuilder<R, T, PreparedTransformerTestBuilder<R, T>> {

  /**
   * Creates a new instance that will test the provided Dagli node.
   *
   * @param testSubject the primary test subject
   */
  public PreparedTransformerTestBuilder(T testSubject) {
    super(testSubject);
  }

  /**
   * @return a variety of ways the prepared transformer can be invoked on a minibatch of examples
   */
  private List<BiFunction<T, List<Object[]>, List<R>>> getMinibatchAppliers() {
    List<BiFunction<T, List<Object[]>, List<R>>> standard = Arrays.asList((prepared, inputs) -> {
      R[] results = (R[]) new Object[inputs.size()];
      prepared.internalAPI()
          .applyAllUnsafe(prepared.internalAPI().createExecutionCache(inputs.size()), inputs.size(),
              ArraysEx.transpose(inputs.toArray(new Object[0][])), results);
      return Arrays.asList(results);
    }, (prepared, inputs) -> {
      ArrayList<R> resultList = new ArrayList<>(inputs.size());
      Object[][] transposed = ArraysEx.transpose(inputs.toArray(new Object[0][]));
      prepared.internalAPI()
          .applyAllUnsafe(prepared.internalAPI().createExecutionCache(inputs.size()), inputs.size(),
              Arrays.stream(transposed).map(Arrays::asList).collect(Collectors.toList()), resultList);
      return resultList;
    });

    // make sure handlers correctly deal with over-sized arrays
    List<BiFunction<T, List<Object[]>, List<R>>> enlengthened = standard.stream()
        .map(function -> (BiFunction<T, List<Object[]>, List<R>>) (prepared, inputs) -> function.apply(prepared,
            inputs.stream().map(arr -> Arrays.copyOf(arr, arr.length + 1)).collect(Collectors.toList())))
        .collect(Collectors.toList());

    return Iterables.concatenate(standard, enlengthened);
  }

  private static Object[][] lift(Object[] array) {
    Object[][] res = new Object[array.length][1];
    for (int i = 0; i < array.length; i++) {
      res[i][0] = array[i];
    }
    return res;
  }

  /**
   * @return a variety of ways the prepared transformer can be invoked on an example
   */
  private List<BiFunction<T, Object[], R>> getAppliers() {
    List<BiFunction<T, Object[], R>> standard = Arrays.asList((prepared, inputs) -> prepared.internalAPI()
            .applyUnsafe(prepared.internalAPI().createExecutionCache(1), inputs),
        (prepared, inputs) -> prepared.internalAPI()
            .applyUnsafe(prepared.internalAPI().createExecutionCache(1), Arrays.asList(inputs)),
        (prepared, inputs) -> prepared.internalAPI()
            .applyUnsafe(prepared.internalAPI().createExecutionCache(1), lift(inputs), 0));

    // make sure handlers correctly deal with over-sized arrays
    List<BiFunction<T, Object[], R>> enlengthened = standard.stream()
        .map(function -> (BiFunction<T, Object[], R>) (prepared, inputs) -> function.apply(prepared,
            Arrays.copyOf(inputs, inputs.length + 1)))
        .collect(Collectors.toList());

    return Iterables.concatenate(standard, enlengthened);
  }

  @SuppressWarnings("unchecked")
  private void simpleReductionTest() {
    if (_skipSimpleReductionTest) {
      return;
    }

    // test a reduced graph
    PreparedDAGTransformer<R, ?> dag =
        (PreparedDAGTransformer<R, ?>) DAGTransformer.withOutput(_testSubject).withReduction(Reducer.Level.EXPENSIVE);

    Tester.of(dag)
        .skipNonTrivialEqualityCheck()
        .skipValidation(_skipValidation)
        .skipSimpleReductionTest() // avoids infinite recursion!
        .allInputs(_inputs)
        .allOutputTests(_outputsTesters)
        .skipNonTrivialEqualityCheck()
        .distinctOutputs(_distinctOutputs)
        .test();
  }

  @Override
  public void test() {
    super.test();
    for (BiFunction<T, Object[], R> applier : getAppliers()) {
      checkInputsAndOutputsForAll(applier);
      checkInputsAndOutputsFor(withPlaceholderInputs(_testSubject), applier);
    }
    for (BiFunction<T, List<Object[]>, List<R>> minibatchApplier : getMinibatchAppliers()) {
      checkMinibatchedInputsAndOutputsForAll(minibatchApplier);
      checkMinibatchedInputsAndOutputsFor(withPlaceholderInputs(_testSubject), minibatchApplier);
    }

    if (_distinctOutputs) {
      HashSet<R> resultSet = new HashSet<>();
      Object executionCache = _testSubject.internalAPI().createExecutionCache(_inputs.size());
      for (Object[] input : _inputs) {
        R result = _testSubject.internalAPI().applyUnsafe(executionCache, input);
        if (!resultSet.add(result)) {
          throw new AssertionError(
              "The prepared transformer " + _testSubject + " produced the result " + result + " for the input sequence "
                  + Arrays.toString(input)
                  + ", which is equals() to a result prepared for another tested input.  This is an error "
                  + "because this test was configured with distinctOutputs().");
        }
      }
    }

    simpleReductionTest();
  }
}
