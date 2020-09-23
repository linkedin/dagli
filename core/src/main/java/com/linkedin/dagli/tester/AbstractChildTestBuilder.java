package com.linkedin.dagli.tester;

import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.producer.ChildProducer;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.util.invariant.Arguments;
import com.linkedin.dagli.util.array.ArraysEx;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;


class AbstractChildTestBuilder<I, R, T extends ChildProducer<R>, S extends AbstractChildTestBuilder<I, R, T, S>>
    extends AbstractTestBuilder<R, T, S> {
  final ArrayList<I> _inputs = new ArrayList<>();
  int _inputArity = -1; // -1 => not yet set
  boolean _checkEqualWithSameParents = true;

  /**
   * Creates a new instance that will test the provided Dagli node.
   *
   * @param testSubject the primary test subject
   */
  AbstractChildTestBuilder(T testSubject) {
    super(testSubject);
  }

  /**
   * By default, the tester checks that two copies of the test subject that each have the same list of parents are
   * equal.  This will be true for the standard value-equality comparison, but a producer using handle-equality will
   * fail this check.
   *
   * Calling this method will disable the check.  Although nodes that fail aren't <i>necessarily</i> "wrong", more
   * robust equality comparison allows for better "deduplication" of redundant nodes in a DAG and is thus desirable.
   *
   * @return this instance
   */
  @SuppressWarnings("unchecked")
  public S skipNonTrivialEqualityCheck() {
    _checkEqualWithSameParents = false;
    return (S) this;
  }

  /**
   * Adds an input.  For a {@link com.linkedin.dagli.view.TransformerView}, this will be a prepared transformer to be
   * viewed by the tested; for a {@link com.linkedin.dagli.transformer.Transformer} this will be an array of inputs.
   *
   * @param input an input to be provided to the tested node
   * @return this instance
   */
  @SuppressWarnings("unchecked")
  S addInput(I input) {
    _inputs.add(input);
    return (S) this;
  }

  /**
   * Adds all of a collection of inputs.
   *
   * @param inputs the collection of inputs to add
   * @return this intance
   */
  @SuppressWarnings("unchecked")
  S addAllInputs(Collection<? extends I> inputs) {
    _inputs.addAll(inputs);
    return (S) this;
  }

  void checkMinibatchedInputsAndOutputsFor(T subject,
      BiFunction<T, List<I>, List<R>> producerAndInputToResultFunction) {

    List<R> results = producerAndInputToResultFunction.apply(subject, _inputs);

    for (int i = 0; i < _outputsTesters.size(); i++) {
      R result = results.get(i);
      if (!_outputsTesters.get(i).test(result)) {
        throw new AssertionError(
            "Output from " + subject + " on input " + ArraysEx.deepToString(_inputs.get(i)) + " was "
                + ArraysEx.deepToString(result) + ", which does not satisfy the test " + _outputsTesters.get(i));
      }
    }
  }

  void checkInputsAndOutputsFor(T subject, BiFunction<T, I, R> producerAndInputToResultFunction) {
    checkMinibatchedInputsAndOutputsFor(subject, (subj, inputList) -> inputList.stream()
        .map(input -> producerAndInputToResultFunction.apply(subj, input))
        .collect(Collectors.toList()));
  }

  void checkInputsAndOutputsForAll(BiFunction<T, I, R> producerAndInputToResultFunction) {
    checkAll(subject -> checkInputsAndOutputsFor(subject, producerAndInputToResultFunction));
  }

  void checkMinibatchedInputsAndOutputsForAll(BiFunction<T, List<I>, List<R>> producerAndInputToResultFunction) {
    checkAll(subject -> checkMinibatchedInputsAndOutputsFor(subject, producerAndInputToResultFunction));
  }

  @Override
  public void test() {
    super.test();
    Arguments.check(_inputs.size() >= _outputsTesters.size(),
        "The number of inputs to be tested must be equal or greater to the number of outputs to be tested");
    testWithInputsResult(_testSubject);
  }

  /**
   * Gets a list of placeholder inputs of the correct arity for a given child node.
   *
   * @param node the node for whom a placeholder list should be generated
   * @return the placeholder list for the node
   */
  private static List<Producer<?>> placeholderInputsFor(ChildProducer<?> node) {
    return node.internalAPI().getInputList().stream().map(o -> new Placeholder<>()).collect(Collectors.toList());
  }

  /**
   * Gets a node like the one provided but with new placeholders replacing its original inputs.
   *
   * @param node the node to be copied and returned with new placeholder inputs
   * @param <T> the type of the node
   * @return a copy of the node, with new placeholders as inputs
   */
  @SuppressWarnings("unchecked") // safe due to semantics of withInputsUnsafe(), which will return something of type T
  static <T extends ChildProducer<?>> T withPlaceholderInputs(ChildProducer<?> node) {
    return (T) node.internalAPI().withInputsUnsafe(placeholderInputsFor(node));
  }

  void testWithInputsResult(ChildProducer<?> testSubject) {
    List<Producer<?>> newInputs = placeholderInputsFor(testSubject);
    ChildProducer<?> withInputs = testSubject.internalAPI().withInputsUnsafe(newInputs);

    assertEquals(withInputs.internalAPI().getInputList().size(), newInputs.size(),
        "Copy of producer created using withInputsUnsafe() has the wrong number of inputs");

    for (int i = 0; i < newInputs.size(); i++) {
      assertEquals(newInputs.get(i), withInputs.internalAPI().getInputList().get(i),
          "Inputs on new transformer created with withInputsUnsafe() do not match the list of inputs passed "
          + "to that method.  A common mistake that might cause this is overriding getInputList() without overriding "
          + "withInputsUnsafe().");
    }

    if (_checkEqualWithSameParents) {
      // make sure that two copies of the test subject with the same list of inputs are considered equal
      assertEquals(withInputs, testSubject.internalAPI().withInputsUnsafe(new ArrayList<>(newInputs)),
          "Copies of the test subject made with a new list of parents (with the copies sharing the same new "
              + "list of parents) did not evaluate as equals().  This usually means that it is using the default "
              + "implementation of equals(), which is not a bug, but a more robust equality check would be better.  "
              + "You may either call the skipNonTrivialEqualityCheck() on this tester to disable this test or add "
              + "equality checking to your Dagli node.  This can usually be accomplished with trivial ease by adding "
              + "the @ValueEquality annotation to your node's class.");
    }
  }
}
