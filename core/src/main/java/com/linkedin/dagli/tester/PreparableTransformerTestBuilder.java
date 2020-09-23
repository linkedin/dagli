package com.linkedin.dagli.tester;

import com.linkedin.dagli.dag.DAGTransformer;
import com.linkedin.dagli.dag.PreparableDAGTransformer;
import com.linkedin.dagli.dag.SimpleDAGExecutor;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.preparer.Preparer;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResultMixed;
import com.linkedin.dagli.producer.internal.ChildProducerInternalAPI;
import com.linkedin.dagli.reducer.Reducer;
import com.linkedin.dagli.transformer.PreparableTransformer;
import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.util.collection.Iterables;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


/**
 * Tests {@link PreparedTransformer} nodes.
 *
 * @param <R> the type of result of the preparable transformer
 * @param <N> the type of the prepared transformer prepared from the tested preparable transformer
 * @param <T> the type of the preparable transformer
 */
public final class PreparableTransformerTestBuilder<R, N extends PreparedTransformer<R>, T extends PreparableTransformer<R, N>>
    extends AbstractTransformerTestBuilder<R, T, PreparableTransformerTestBuilder<R, N, T>> {
  Consumer<N> _preparedTransformerTester = n -> { };
  N _expectedPrepared = null;
  int _preparedTransformerInputLimit = Integer.MAX_VALUE;

  /**
   * Creates a new instance that will test the provided Dagli node.
   *
   * @param testSubject the primary test subject
   */
  public PreparableTransformerTestBuilder(T testSubject) {
    super(testSubject);
  }

  /**
   * By default, the tester will prepare using the provided inputs and then test the resulting, prepared transformer
   * against all of these inputs (even if no constraints/checks on the corresponding outputs are specified, this still
   * acts as a "sanity check" to make sure the prepared transformer doesn't, e.g. throw an exception when applied to
   * the inputs.)  However, for some transformers this can be unduly expensive (for example, a FastText model can
   * require, with common hyperparameterizations, much less time per example to train than to do inference);
   * consequently, a limit may be applied to the total number of inputs that will be used to test the prepared
   * transformer.
   *
   * @param limit a limit on the number of inputs provided to the tester for preparing the transformer that will also be
   *              used to test the resulting prepared transformer
   * @return this instance
   */
  public PreparableTransformerTestBuilder<R, N, T> preparedTransformerInputLimit(int limit) {
    _preparedTransformerInputLimit = limit;
    return this;
  }

  /**
   * Adds a method that should receive the prepared transformer resulting from this test for further evaluation.
   * Multiple testing methods may be added via repeated calls to this method.
   *
   * The <code>tester</code> method can create and execute a {@link PreparedTransformerTestBuilder} and/or do any other
   * checks it desires.  The prepared transformer will be "for new data", not "for preparation data" (i.e. it is the
   * prepared transformer intended for use on new examples in the final, prepared DAG); these are <em>almost</em> always
   * the same (see {@link PreparableTransformer} for details.)
   *
   * No transformer will be prepared (or tested) if {@link #input(Object, Object...)} has not been used to supply at least one
   * input.
   *
   * Note that the provided testing method may be called multiple times (e.g. each preparable transformer supposedly
   * {@link #equalTo(com.linkedin.dagli.producer.Producer)} to the test subject will be prepared and the result passed
   * to this method).
   *
   * @param tester the method that will examine the prepared transformer(s) resulting from this test, as prepared on the
   *               test's inputs
   * @return this instance
   */
  public PreparableTransformerTestBuilder<R, N, T> preparedTransformerTester(Consumer<N> tester) {
    _preparedTransformerTester = _preparedTransformerTester.andThen(tester);
    return this;
  }

  /**
   * If specified, the tester will make sure that the prepared transformer resulting from this test (as prepared on the
   * provided inputs) is {@link Object#equals(Object)} to this prepared transformer.  The prepared transformer being
   * compared will be "for new data", not "for preparation data" (i.e. it is the prepared transformer intended for use
   * on new examples in the final, prepared DAG); these are <em>almost</em> always the same (see
   * {@link PreparableTransformer} for details.)
   *
   * No transformer will be compared if {@link #input(Object, Object...)} has not been used to supply at least one input.
   *
   * @param expected the prepared transformer expected to result from preparing the subject of this test with the
   *                 provided inputs
   * @return this instance
   */
  public PreparableTransformerTestBuilder<R, N, T> preparedTransformerExpected(N expected) {
    _expectedPrepared = expected;
    return this;
  }

  @Override
  public void test() {
    super.test();

    if (_autogenerateParents && _expectedPrepared != null) {
      // update the parents of expectedPrepared too
      _expectedPrepared =
          ChildProducerInternalAPI.withInputsUnsafe(_expectedPrepared, _testSubject.internalAPI().getInputList());
    }

    checkAll(subject -> checkPreparable(subject, true));
    checkPreparable(withPlaceholderInputs(_testSubject), false);

    simpleReductionTest();
  }

  /**
   * @return the list of inputs that should be used to test the resulting prepared transformer
   */
  private List<Object[]> inputsForPreparedTransformer() {
    return _preparedTransformerInputLimit < _inputs.size() ? _inputs.subList(0, _preparedTransformerInputLimit)
        : _inputs;
  }

  @SuppressWarnings("unchecked")
  private void simpleReductionTest() {
    if (_skipSimpleReductionTest) {
      return;
    }

    // test a reduced graph
    PreparableDAGTransformer<R, ?, ?> dag =
        (PreparableDAGTransformer<R, ?, ?>) DAGTransformer.withOutput(_testSubject)
            .withReduction(Reducer.Level.EXPENSIVE);

    Tester.of(dag)
        .skipNonTrivialEqualityCheck()
        .skipValidation(_skipValidation)
        .skipSimpleReductionTest() // avoids infinite recursion!
        .allInputs(_inputs)
        .allOutputTests(_outputsTesters)
        .skipNonTrivialEqualityCheck()
        .distinctOutputs(_distinctOutputs)
        .preparedTransformerInputLimit(_preparedTransformerInputLimit)
        .test();
  }

  /**
   * Checks that a preparable:
   * (1) Prepares to a prepared transformer ("for preparation data") that yields results consistent with our expected
   *     output list when applied to our inputs (which are also used to prepare the transformer)
   * (2) Prepares to a prepared transformer ("for new data") that:
   *     (a) is equals() to others prepared from preparables that are supposed to be equivalent to our test subject
   *     (b) is equals() to a client-supplied expected prepared transformer (depending on
   *         <code>testPreparedEquality</code>)
   *     (c) passes the client-supplied test, if any
   *     (d) is able to be applied to the inputs used to prepare it without throwing an exception and passes other basic
   *         tests
   * (3) Prepares to transformers that are always-constant-result if the preparable claims to be always-constant-result
   * (4) Satisfies all tests with duplicated input if the preparable claims to be idempotent
   *
   * @param preparable the preparable to be tested
   * @param testPreparedEquality whether or not the prepared transformer is tested for equality with the
   *                             client-specified expected prepared transformer
   */
  void checkPreparable(T preparable, boolean testPreparedEquality) {
    if (_inputs.isEmpty()) {
      return;
    }

    checkPreparable(preparable, testPreparedEquality, _inputs);

    if (preparable.internalAPI().hasIdempotentPreparer()) {
      checkPreparable(preparable, testPreparedEquality, Iterables.concatenate(_inputs, _inputs));
    }
  }

  private void checkPreparable(T preparable, boolean testPreparedEquality, List<Object[]> inputs) {
    Preparer<R, N> preparer = preparable.internalAPI()
        .getPreparer(PreparerContext.builder(inputs.size()).setExecutor(new SimpleDAGExecutor()).build());

    inputs.forEach(preparer::processUnsafe);
    PreparerResultMixed<? extends PreparedTransformer<? extends R>, N> result =
        preparer.finishUnsafe(ObjectReader.wrap(inputs));

    // make sure the results have parents
    result = (PreparerResultMixed) new PreparerResultMixed.Builder<>().withTransformerForPreparationData(
        result.getPreparedTransformerForPreparationData()
            .internalAPI()
            .withInputsUnsafe(new ArrayList<>(preparable.internalAPI().getInputList())))
        .withTransformerForNewData(result.getPreparedTransformerForNewData()
            .internalAPI()
            .withInputsUnsafe(new ArrayList<>(preparable.internalAPI().getInputList())))
        .build();

    // do validation of the prepared transformers
    if (!_skipValidation) {
      result.getPreparedTransformerForPreparationData().validate();
      result.getPreparedTransformerForNewData().validate();
    }

    // check that always-constant-value is honored
    if (preparable.internalAPI().hasAlwaysConstantResult()) {
      assertTrue(result.getPreparedTransformerForPreparationData().internalAPI().hasAlwaysConstantResult(),
          "Preparable is always-constant-result, but prepared transformer (for preparation data) is not; this is "
              + "required by the always-constant-result contract.");
      assertTrue(result.getPreparedTransformerForNewData().internalAPI().hasAlwaysConstantResult(),
          "Preparable is always-constant-result, but prepared transformer (for new data) is not; this is "
              + "required by the always-constant-result contract.");
    }

    // check that the resulting prepared transformer (for preparation data) is producing the expected results
    Tester.of(PreparedTransformer.<R>cast(result.getPreparedTransformerForPreparationData()))
        .addAllInputs(inputsForPreparedTransformer())
        .allOutputTests(_outputsTesters)
        .skipNonTrivialEqualityCheck()
        .skipSimpleReductionTest(_skipSimpleReductionTest)
        .skipValidation(_skipValidation)
        .distinctOutputs(_distinctOutputs)
        .test();

    if (testPreparedEquality) {
      // check if the prepared transformer (for new data) matches what we expected; if there's nothing expected, compare
      // the prepared transformer against itself
      testEquality(_expectedPrepared != null ? _expectedPrepared : result.getPreparedTransformerForNewData(),
          result.getPreparedTransformerForNewData());
    }

    // also make sure that it satisfies all the basic tests and doesn't throw on our test inputs
    Tester.of(result.getPreparedTransformerForNewData())
        .skipNonTrivialEqualityCheck()
        .skipSimpleReductionTest(_skipSimpleReductionTest)
        .skipValidation(_skipValidation)
        .addAllInputs(inputsForPreparedTransformer())
        .test();

    // run the client-supplied test (if any)
    _preparedTransformerTester.accept(result.getPreparedTransformerForNewData());
  }
}
