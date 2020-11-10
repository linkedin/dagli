package com.linkedin.dagli.tester;

import com.linkedin.dagli.dag.DAGTransformer;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.reducer.Reducer;
import com.linkedin.dagli.util.cloneable.AbstractCloneable;
import com.linkedin.dagli.util.collection.LinkedStack;
import com.linkedin.dagli.util.exception.Exceptions;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;


/**
 * Base class for testers of Dagli nodes
 *
 * @param <T> type of the node being tested
 * @param <S> type of the ultimate derived subclass of this class
 */
abstract class AbstractTestBuilder<R, T extends Producer<R>, S extends AbstractTestBuilder<R, T, S>>
    extends AbstractCloneable<S> {
  /**
   * The primary test subject of this test.
   */
  T _testSubject;

  /**
   * A list of nodes that should be equivalent in both behavior and as reported by equals()
   */
  final List<T> _equivalents = new ArrayList<>();

  /**
   * A list of nodes that should not be equivalent in behavior or as reported by equals()
   */
  final List<T> _nonEquivalents = new ArrayList<>();

  /**
   * A list of outputs testers.  Each should return true if the output is satisfactory.
   */
  final List<Predicate<? super R>> _outputsTesters = new ArrayList<>();

  /**
   * A list of testers for the reduced DAG.  Each tester will receive the stream of producers retrieved by the
   * {@code producers()} method on the reduced DAG and should return true if it is as expected.
   */
  final List<Predicate<Stream<LinkedStack<Producer<?>>>>> _reductionTesters = new ArrayList<>();

  /**
   * True if all outputs tested (for generators) or the outputs corresponding to every input (for child nodes like
   * transformers) should be distinct.
   */
  boolean _distinctOutputs = false;

  /**
   * True if {@link Producer#validate()} should not be used to validate producers.
   */
  boolean _skipValidation = false;

  Type _resultSupertype = null;

  /**
   * Creates a new instance that will test the provided Dagli node.
   *
   * @param testSubject the primary test subject
   */
  AbstractTestBuilder(T testSubject) {
    _testSubject = testSubject;
  }

  /**
   * @return a clone of this tester
   */
  @Override
  public S clone() {
    return super.clone();
  }

  /**
   * Adds to this test a node that should be logically equivalent to the primary test subject.
   *
   * @param other the other, equivalent node.
   * @return this instance
   */
  @SuppressWarnings("unchecked")
  public S equalTo(T other) {
    _equivalents.add(other);
    return (S) this;
  }

  /**
   * Adds to this test a node that should be logically distinct, as reported by equals(), from the primary test subject.
   *
   * @param other the other, non-equivalent node.
   * @return this instance
   */
  @SuppressWarnings("unchecked")
  public S notEqualTo(T other) {
    _nonEquivalents.add(other);
    return (S) this;
  }

  /**
   * Adds a check to verify that the tested producer's reported result supertype <strong>exactly</strong> matches the
   * provided type.  Most implementations use Dagli's default implementation for determining the result supertype, so
   * testing is generally only required when this implementation is overridden.
   *
   * @param supertype the result supertype that should be reported by the producer
   * @return this instance
   */
  @SuppressWarnings("unchecked")
  public S resultSupertype(Type supertype) {
    _resultSupertype = supertype;
    return (S) this;
  }

  /**
   * Calls the provided <code>tester</code> on:
   * (1) The primary test subject
   * (2) The serialized and then deserialized copy of the test subject
   * (3) Each "equivalent" node
   * (4) The serialized and then deserialized copy of each "equivalent" node
   *
   * @param tester the testing method
   */
  void checkAll(Consumer<T> tester) {
    tester.accept(_testSubject);
    tester.accept(serializeAndDeserialize(_testSubject));
    _equivalents.forEach(tester);
    _equivalents.stream().map(AbstractTestBuilder::serializeAndDeserialize).forEach(tester);
  }

  /**
   * Simple wrapper for a predicate that implements {@link Object#toString()}
   *
   * @param <R> the type of result of the predicate
   */
  private static class NamedPredicate<R> implements Predicate<R> {
    private final Predicate<R> _tester;
    private final String _name;

    public NamedPredicate(Predicate<R> tester, String name) {
      _tester = tester;
      _name = name;
    }

    @Override
    public boolean test(R r) {
      return _tester.test(r);
    }

    @Override
    public String toString() {
      return _name;
    }
  }

  /**
   * Adds a client-provided test that will check that the result of the tested node, returning true if the result is
   * as expected, and false if the result is erroneous.  For child nodes (those with inputs) each output check
   * corresponds to an input sequence provided by input() such that the first input provided corresponds to the first
   * output, the second input corresponds to the second output, etc.
   *
   * For child nodes, the number of output checks cannot be greater than the number of inputs.  Generators don't have
   * inputs and can have as many output checks as desired; these will check the result of the generator at increasing
   * example indices (starting at 0).
   *
   * @param test the test to perform against the output of the node
   * @param name a name for the test; this will be included in the thrown exception if the test fails
   * @return this instance
   */
  public S outputTest(Predicate<? super R> test, String name) {
    return outputTest(new NamedPredicate<>(test, name));
  }

  /**
   * Adds a client-provided test that will check that the result of the tested node, returning true if the result is
   * as expected, and false if the result is erroneous.  For child nodes (those with inputs) each output check
   * corresponds to an input sequence provided by input() such that the first input provided corresponds to the first
   * output, the second input corresponds to the second output, etc.
   *
   * For child nodes, the number of output checks cannot be greater than the number of inputs.  Generators don't have
   * inputs and can have as many output checks as desired; these will check the result of the generator at increasing
   * example indices (starting at 0).
   *
   * @param test the test to perform against the output of the node
   * @return this instance
   */
  @SuppressWarnings("unchecked")
  public S outputTest(Predicate<? super R> test) {
    _outputsTesters.add(test);
    return (S) this;
  }

  /**
   * Adds test that will check that the result of the tested node is equal to the provided value.  For child nodes
   * (those with inputs) each output check corresponds to an input sequence provided by input() such that
   * the first input provided corresponds to the first output, the second input corresponds to the second output, etc.
   *
   * For child nodes, the number of output checks cannot be greater than the number of inputs.  Generators don't have
   * inputs and can have as many output checks as desired; these will check the result of the generator at increasing
   * example indices (starting at 0).
   *
   * @param output an expected output from the tested node
   * @return this instance
   */
  public S output(R output) {
    return outputTest(new NamedPredicate<>(value -> Objects.equals(value, output), "== " + output));
  }

  /**
   * Adds each expected output value from a collection; equivalent to calling {@link #output(Object)} for each value.
   *
   * @param outputs the output values to add
   * @return this instance
   */
  @SuppressWarnings("unchecked")
  public S allOutputs(Iterable<? extends R> outputs) {
    outputs.forEach(this::output);
    return (S) this;
  }

  /**
   * Adds each output test from a collection; equivalent to calling {@link #outputTest(Predicate)} for each test.
   *
   * @param outputTests the output tests to add
   * @return this instance
   */
  @SuppressWarnings("unchecked")
  public S allOutputTests(Iterable<Predicate<? super R>> outputTests) {
    outputTests.forEach(this::outputTest);
    return (S) this;
  }

  /**
   * Adds a tester that will examine a DAG containing created with the test subject as an output (if not a generator,
   * it must have no more than 10 placeholders as ancestors) that has been reduced to the greatest extent possible
   * ({@link com.linkedin.dagli.reducer.Reducer.Level#EXPENSIVE}).
   *
   * Each tester will receive the stream of producers retrieved by the {@code producers()} method on the reduced DAG and
   * should return true if the producers in the reduced DAG are as expected.
   */
  @SuppressWarnings("unchecked")
  public S reductionTest(Predicate<Stream<LinkedStack<Producer<?>>>> reductionTester) {
    _reductionTesters.add(reductionTester);
    return (S) this;
  }

  /**
   * If this method is called, producers will not be validated.
   *
   * This is not recommended, but may be useful if the tests can proceed on an invalid instance and for whatever reason
   * creating a valid instance is too cumbersome.
   *
   * @return this instance
   */
  @SuppressWarnings("unchecked")
  public S skipValidation() {
    return skipValidation(true);
  }

  /**
   * If {@code skip} is true, producers will not be validated.
   *
   * Skipping validation is not recommended, but may be useful if the tests can proceed on an invalid instance and for
   * whatever reason creating a valid instance is too cumbersome.  Invalid producers are also likely to fail other tests
   * which will themselves need to be skipped.
   *
   * @param skip whether to skip validation testing
   * @return this instance
   */
  @SuppressWarnings("unchecked")
  public S skipValidation(boolean skip) {
    _skipValidation = true;
    return (S) this;
  }

  /**
   * If this method is called, all outputs tested (for generators) or all outputs corresponding to every input (for
   * child nodes like transformers) must be distinct (not evaluate as {@link Object#equals(Object)}).
   *
   * @return this instance
   */
  @SuppressWarnings("unchecked")
  public S distinctOutputs() {
    _distinctOutputs = true;
    return (S) this;
  }

  /**
   * If <code>mustBeDistinct</code> is true, all outputs tested (for generators) or all outputs corresponding to every
   * input (for child nodes like transformers) must be distinct (not evaluate as {@link Object#equals(Object)}).
   *
   * @return this instance
   */
  @SuppressWarnings("unchecked")
  S distinctOutputs(boolean mustBeDistinct) {
    _distinctOutputs = mustBeDistinct;
    return (S) this;
  }

  static void assertTrue(boolean value, String message) {
    if (!value) {
      throw new AssertionError(message);
    }
  }

  static void assertEquals(Object a, Object b, String message) {
    if (!Objects.equals(a, b)) {
      throw new AssertionError(message + " (" + a + " != " + b + ")");
    }
  }

  static void assertNotEquals(Object a, Object b, String message) {
    if (Objects.equals(a, b)) {
      throw new AssertionError(message + " (" + a + " == " + b + ")");
    }
  }

  /**
   * Runs the test.  Failures will result in thrown exceptions; a successful test will return from this method without
   * an exception being thrown.
   */
  public void test() {
    if (!_skipValidation) {
      _testSubject.validate(); // instance must be valid for us to test it further
    }

    testEqualityForEach(_testSubject, _equivalents);
    testNonEqualityForEach(_testSubject, _nonEquivalents);

    testReduction();

    // check for invalid names
    assertTrue(!isNullOrEmpty(_testSubject.getShortName()), "A producer's short name must not be null or empty");
    assertTrue(!isNullOrEmpty(_testSubject.getName()), "A producer's name must not be null or empty");

    // check the reported result supertype
    Type resultSupertype = Producer.getResultSupertype(_testSubject);
    assertTrue(resultSupertype != null, "The producer erroneously reports a null result supertype");
    if (_resultSupertype != null) {
      assertEquals(_resultSupertype, resultSupertype, "The expected and reported result supertypes do not match");
    }
  }

  private static boolean isNullOrEmpty(String str) {
    return str == null || str.isEmpty();
  }

  void testReduction() {
    if (_reductionTesters.isEmpty()) {
      return;
    }

    DAGTransformer<R, ?> dag = DAGTransformer.withOutput(_testSubject).withReduction(Reducer.Level.EXPENSIVE);

    for (int i = 0; i < _reductionTesters.size(); i++) {
      assertTrue(_reductionTesters.get(i).test(dag.producers()),
          "Reduction test #" + i + " (" + _reductionTesters.get(i) + ") failed");
    }
  }

  static void testNonEqualityForEach(Object testSubject, List<?> nonEquivalents) {
    if (nonEquivalents.isEmpty()) {
      return;
    }

    Object deserializedTestSubject = serializeAndDeserialize(testSubject);

    for (Object nonEquivalent : nonEquivalents) {
      Object deserializedNonEquivalent = serializeAndDeserialize(nonEquivalent);
      testNonEquality(testSubject, nonEquivalent);
      testNonEquality(deserializedTestSubject, nonEquivalent);
      testNonEquality(testSubject, deserializedNonEquivalent);
      testNonEquality(deserializedTestSubject, deserializedNonEquivalent);
    }
  }

  static void testEqualityForEach(Object testSubject, List<?> equivalents) {
    if (equivalents.isEmpty()) {
      return;
    }

    Object deserializedTestSubject = serializeAndDeserialize(testSubject);

    for (Object equivalent : equivalents) {
      Object deserializedEquivalent = serializeAndDeserialize(equivalent);
      testEquality(testSubject, equivalent);
      testEquality(deserializedTestSubject, equivalent);
      testEquality(testSubject, deserializedEquivalent);
      testEquality(deserializedTestSubject, deserializedEquivalent);
    }
  }

  static void testNonEquality(Object object1, Object object2) {
    assertEquals(object1, object2, "Non-equality test failed");
    assertEquals(object2, object1, "Non-equality test failed; equals() method is not symmetric");
  }

  static void testEquality(Object object1, Object object2) {
    assertEquals(object1, object2, "Equality test failed");
    assertEquals(object2, object1, "Equality test failed; equals() method is not symmetric");
    assertEquals(object1.hashCode(), object2.hashCode(), "Objects evaluating as equals() do not have same hashCode()");
  }

  /**
   * Checks that the given object's class defined the serialVersionUID field.
   *
   * @param tested the object to test
   */
  static void assertHasSerialVersionUID(Object tested) {
    try {
      tested.getClass().getDeclaredField("serialVersionUID");
    } catch (NoSuchFieldException e) {
      throw new AssertionError(tested.getClass().getName() + " does not define serialVersionUID", e);
    }
  }

  /**
   * Serializes and deserializes the provided object, testing that the deserialized copy is equivalent to the original.
   * Useful for testing if serialization and deserialization break or otherwise alter an object.
   *
   * @param obj the object to serialize and then deserialize
   * @param <P> the type of object
   * @return the deserialized object
   */
  @SuppressWarnings("unchecked") // unchecked cast vetted by comparison of classes in subsequent assertEquals()
  static <P> P serializeAndDeserialize(P obj) {
    assertHasSerialVersionUID(obj);

    final P result;
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(obj);
      oos.flush();

      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      ObjectInputStream ois = new ObjectInputStream(bais);
      result = (P) ois.readObject();
    } catch (Exception e) {
      throw Exceptions.asRuntimeException(e);
    }

    assertEquals(result.getClass(),
        obj.getClass(), "Object is not the same class after serializing and deserializing!");
    assertEquals(result, obj, "Object does not compare as equals() after serializing and deserializing!");
    assertEquals(result.hashCode(), obj.hashCode(),
        "Object does not have consistent hashCode() after serializing and deserializing!");

    return result;
  }
}
