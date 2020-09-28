package com.linkedin.dagli.dag;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.function.FunctionResult2;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.generator.ExampleIndex;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.objectio.biglist.BigListWriter;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.preparer.AbstractStreamPreparer1;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.AbstractPreparableTransformer1;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer2;
import com.linkedin.dagli.tuple.Tuple2;
import com.linkedin.dagli.tuple.Tuple3;
import com.linkedin.dagli.tuple.Tuple4;
import com.linkedin.dagli.util.array.ArraysEx;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

public class DAGTest {
  private static final boolean RUN_ALL_TESTS = Boolean.parseBoolean(System.getProperty("com.linkedin.dagli.alltests"));
  @BeforeAll
  public static void setup() {
    // turn on tracing for the SimpleDAGExecutor so we can also make sure it's logging routines don't throw
    Configurator.setLevel(SimpleDAGExecutor.class.getName(), Level.TRACE);
  }

  public static DAGExecutor[] preparableExecutors() {
    return ArraysEx.concat(new DAGExecutor[]{new SimpleDAGExecutor(), new LocalDAGExecutor()},
        getMultithreadedExecutors());
  }

  public static PreparedDAGExecutor[] preparedExecutors() {
    return ArraysEx.concat(preparableExecutors(), getFastPreparedExecutors());
  }

  private static PreparedDAGExecutor[] getFastPreparedExecutors() {
    int[] minInputsPerThreads = RUN_ALL_TESTS ? new int[]{1, 2, 3, 4, 8, 16} : new int[]{4};
    int[] maxThreads = RUN_ALL_TESTS ? new int[]{1, 2, 3, 4, 5, 8, 16} : new int[]{5};

    PreparedDAGExecutor[] res = new PreparedDAGExecutor[minInputsPerThreads.length * maxThreads.length];
    int nextIndex = 0;
    for (int minPerThread : minInputsPerThreads) {
      for (int maxThread : maxThreads) {
        res[nextIndex++] = new FastPreparedDAGExecutor().withMinInputsPerThread(minPerThread).withMaxThreads(maxThread);
      }
    }

    return res;
  }

  private static DAGExecutor[] getMultithreadedExecutors() {
    int[] batchSizes = RUN_ALL_TESTS ? new int[]{1, 2, 3, 5, 7, 10} : new int[]{6};
    int[] maxConcurrentBatches = RUN_ALL_TESTS ? new int[]{1, 2, 3, 4, 8, 17} : new int[]{7};
    int[] maxThreadCounts = RUN_ALL_TESTS ? new int[]{1, 2, 3, 8, 15} : new int[]{5};
    LocalStorage[] cachings = LocalStorage.values(); // try them all!

    DAGExecutor[] executors =
        new DAGExecutor[batchSizes.length * maxConcurrentBatches.length * maxThreadCounts.length * cachings.length];

    int nextIndex = 0;
    for (int batchSize : batchSizes) {
      for (int maxConcurrentBatch : maxConcurrentBatches) {
        for (int maxThreadCount : maxThreadCounts) {
          for (LocalStorage caching : cachings) {
            executors[nextIndex++] = new MultithreadedDAGExecutor()
                .withBatchSize(batchSize)
                .withConcurrentBatches(maxConcurrentBatch)
                .withMaxThreads(maxThreadCount)
                .withStorage(caching);
          }
        }
      }
    }

    return executors;
  }

  @ValueEquality
  static class TestAddAsDoublesTransformer extends AbstractPreparedTransformer2<Number, Number, Double, TestAddAsDoublesTransformer> {
    private static final long serialVersionUID = 1;

    public TestAddAsDoublesTransformer(Producer<? extends Number> input1, Producer<? extends Number> input2) {
      super(input1, input2);
    }

    @Override
    public Double apply(Number val1, Number val2) {
      if (val1 == null || val2 == null) {
        return null;
      }
      return val1.doubleValue() + val2.doubleValue();
    }
  }

  @ValueEquality
  static class MemorizedFilterTransformer<T>
      extends AbstractPreparableTransformer1<T, T, MemorizedFilterTransformer<T>.FilteringTransformer, MemorizedFilterTransformer<T>> {
    private static final long serialVersionUID = 1;

    public HashSet<T> _seen = new HashSet<>();

    MemorizedFilterTransformer(Producer<? extends T> input1) {
      super(input1);
    }

    @ValueEquality
    class FilteringTransformer extends AbstractPreparedTransformer1<T, T, FilteringTransformer> {
      private static final long serialVersionUID = 1;

      public FilteringTransformer(Producer<? extends T> input1) {
        super(input1);
      }

      @Override
      public T apply(T valA) {
        if (_seen.contains(valA)) {
          return valA;
        } else {
          return null;
        }
      }
    }

    public class Preparer extends AbstractStreamPreparer1<T, T, FilteringTransformer> {
      @Override
      public PreparerResult<FilteringTransformer> finish() {
        return new PreparerResult<>(new FilteringTransformer(MissingInput.get()));
      }

      @Override
      public void process(T value) {
        _seen.add(value);
      }
    }

    @Override
    public Preparer getPreparer(PreparerContext context) {
      return new Preparer();
    }
  }

  @ParameterizedTest
  @MethodSource("preparedExecutors")
  public void testBasicPrepared(PreparedDAGExecutor executor) {
    Placeholder<Integer> intPlaceholder = new Placeholder<>("Integer Placeholder");
    Placeholder<Double> doublePlaceholder = new Placeholder<>("Double Placeholder");

    TestAddAsDoublesTransformer intDoubleAdder = new TestAddAsDoublesTransformer(intPlaceholder, doublePlaceholder);

    // 2*(A + B)
    TestAddAsDoublesTransformer twiceIntDoubleAdder = new TestAddAsDoublesTransformer(intDoubleAdder, intDoubleAdder);

    // 3*(A + B)
    TestAddAsDoublesTransformer tripleIntDoubleAdder =
        new TestAddAsDoublesTransformer(intDoubleAdder, twiceIntDoubleAdder);

    // 3 *(A + B) + B
    TestAddAsDoublesTransformer tripleIntDoublePlusDoubleAdder =
        new TestAddAsDoublesTransformer(tripleIntDoubleAdder, doublePlaceholder);

    DAG2x2.Prepared<Integer, Double, Double, Double> dag = DAG.Prepared.withPlaceholders(intPlaceholder, doublePlaceholder)
        .withOutputs(tripleIntDoubleAdder, tripleIntDoublePlusDoubleAdder)
        .withExecutor(executor);

    assertEquals(dag.apply(1, 1.5), Tuple2.of(7.5, 9.0));
    assertEquals(dag.apply(-1, 1.5), Tuple2.of(1.5, 3.0));
    assertEquals(dag.apply(-1, 1.0), Tuple2.of(0.0, 1.0));

    DynamicDAG.Prepared<Tuple2<Double, Double>> dynamicDAG =
        new DynamicDAG.Prepared<>().withOutputs(tripleIntDoubleAdder, tripleIntDoublePlusDoubleAdder)
            .withPlaceholders(intPlaceholder, doublePlaceholder).withExecutor(executor);
    DynamicDAGResult.Prepared<Tuple2<Double, Double>> res = dynamicDAG.applyAll()
        .input(intPlaceholder, ObjectReader.of(1, -1, -1))
        .input(doublePlaceholder, ObjectReader.of(1.5, 1.5, 1.0))
        .execute();
    assertTrue(
        ObjectReader.equals(res, ObjectReader.of(Tuple2.of(7.5, 9.0), Tuple2.of(1.5, 3.0), Tuple2.of(0.0, 1.0))));
  }

  @ParameterizedTest
  @MethodSource("preparableExecutors")
  public void testTrivialPreparableDAG(DAGExecutor executor) {
    Placeholder<Integer> intPlaceholder = new Placeholder<>("Integer Placeholder");
    MemorizedFilterTransformer<Integer> filter = new MemorizedFilterTransformer<>(intPlaceholder);

    DAG1x1<Integer, Integer> dag = DAG.withPlaceholder(intPlaceholder).withOutput(filter).withExecutor(executor);

    List<Integer> preparationInput = Arrays.asList(1, 2, 3, 4, 5, 6);

    DAG1x1.Result<Integer, Integer> res = dag.prepareAndApply(preparationInput);

    assertEquals(filter._seen, new HashSet<>(preparationInput));
    assertEquals(res.toList(), preparationInput);

    assertEquals(res.getPreparedDAG().applyAll(Arrays.asList(-1000, -100, 3, 4, 7, 100)).toList(),
        Arrays.asList(null, null, 3, 4, null, null));
  }

  @ParameterizedTest
  @MethodSource("preparableExecutors")
  public void testPreparableDAG(DAGExecutor executor) {
    Placeholder<Integer> intPlaceholder = new Placeholder<>("Integer Placeholder");
    TestAddAsDoublesTransformer indexGenerator =
        new TestAddAsDoublesTransformer(new ExampleIndex(), new Constant<>(1));

    TestAddAsDoublesTransformer adder1 = new TestAddAsDoublesTransformer(intPlaceholder, indexGenerator);
    MemorizedFilterTransformer<Double> filter = new MemorizedFilterTransformer<>(adder1);
    TestAddAsDoublesTransformer adder2 = new TestAddAsDoublesTransformer(adder1, filter);

    DAG1x1<Integer, Double> dag = DAG.withPlaceholder(intPlaceholder).withOutput(adder2).withExecutor(executor);

    List<Integer> preparationInput = Arrays.asList(1, 2, 3, 4, 5, 6);
    // when added with their index, these double to: 2, 4, 6, 8, 10, 12, which go into the filter
    Set<Double> inFilter = new HashSet<>(Arrays.asList(2.0, 4.0, 6.0, 8.0, 10.0, 12.0));

    DAG1x1.Result<Integer, Double> res = dag.prepareAndApply(preparationInput);

    assertEquals(filter._seen, inFilter);
    assertTrue(
        ObjectReader.equals(res, ObjectReader.wrap(Arrays.asList(4.0, 8.0, 12.0, 16.0, 20.0, 24.0))));

    assertEquals(res.getPreparedDAG().applyAll(Arrays.asList(-1000, -100, 3, 4, 7, 100)).toList(),
        Arrays.asList(null, null, 12.0, 16.0, 24.0, null));
  }

  @ParameterizedTest
  @MethodSource("preparableExecutors")
  public void testPreparableDAG2(DAGExecutor executor) {
    Placeholder<Integer> intPlaceholder = new Placeholder<>("Integer Placeholder");
    TestAddAsDoublesTransformer indexGenerator =
        new TestAddAsDoublesTransformer(new ExampleIndex(), new Constant<>(1));

    MemorizedFilterTransformer<Integer> filter = new MemorizedFilterTransformer<>(intPlaceholder);
    TestAddAsDoublesTransformer adder = new TestAddAsDoublesTransformer(filter, indexGenerator);

    DAG1x1<Integer, Double> dag = DAG.withPlaceholder(intPlaceholder).withOutput(adder).withExecutor(executor);

    List<Integer> preparationInput = Arrays.asList(0, 1, 2, 3, 4, 5);
    Set<Integer> inFilter = new HashSet<>(Arrays.asList(0, 1, 2, 3, 4, 5));

    DAG1x1.Result<Integer, Double> res = dag.prepareAndApply(preparationInput);

    assertEquals(filter._seen, inFilter);
    assertTrue(ObjectReader.equals(res,
        ObjectReader.wrap(Arrays.asList(1.0, 3.0, 5.0, 7.0, 9.0, 11.0))));

    assertEquals(res.getPreparedDAG().applyAll(Arrays.asList(3, -1000, -100)).toList(), Arrays.asList(4.0, null, null));
  }

  @ParameterizedTest
  @MethodSource("preparedExecutors")
  public void testSubgraphs(PreparedDAGExecutor executor) {
    Placeholder<Integer> a = new Placeholder<>();
    Placeholder<Integer> b = new Placeholder<>();
    Placeholder<String> c = new Placeholder<>();

    FunctionResult2<Integer, String, Integer> identity =
        new FunctionResult2<Integer, String, Integer>().withFunctionUnsafe((i, s) -> i).withInputs(b, c);
    FunctionResult2<Integer, Integer, Integer> sum =
        new FunctionResult2<Integer, Integer, Integer>().withFunctionUnsafe((x, y) -> x + y).withInputs(a, identity);
    FunctionResult2<Integer, Integer, Integer> sum2 =
        new FunctionResult2<Integer, Integer, Integer>().withFunctionUnsafe((x, y) -> x + y).withInputs(sum, identity);

    DAG4x2.Prepared<Integer, Integer, String, Integer, Integer, Integer> bisum =
        DAG.Prepared.withInputs(a, b, c, identity).withOutputs(sum, sum2).withExecutor(executor);

    assertEquals(bisum.apply(5, 10000, "blah", 3), Tuple2.of(8, 11));

    DAG4x3.Prepared<Integer, Integer, String, Integer, Integer, Integer, String> bisumAndString =
        DAG.Prepared.withInputs(a, b, c, identity).withOutputs(sum, sum2, c).withExecutor(executor);

    assertEquals(bisumAndString.apply(5, 10000, "blah", 3), Tuple3.of(8, 11, "blah"));

    DAG4x1.Prepared<Integer, Integer, Integer, String, Integer> simpleSum =
        DAG.Prepared.withInputs(sum, a, b, c).withOutput(sum2).withExecutor(executor);

    assertEquals((int) simpleSum.apply(11, -34234324, 16, "bleh"), 27);

    DAG1x1.Prepared<Integer, Integer> identityDAG = DAG.Prepared.withInput(sum2).withOutput(sum2).withExecutor(executor);

    assertEquals((int) identityDAG.apply(2), 2);

    DAG1x1.Prepared<Integer, Integer> placeholderDAG = DAG.Prepared.withInput(a).withOutput(a).withExecutor(executor);

    assertEquals((int) identityDAG.apply(2), 2);
  }

  @ParameterizedTest
  @MethodSource("preparedExecutors")
  public void testTrivialPlaceholderOutput(PreparedDAGExecutor executor) {
    Placeholder<Integer> intPlaceholder = new Placeholder<>("Integer");
    TestAddAsDoublesTransformer indexGenerator =
        new TestAddAsDoublesTransformer(new ExampleIndex(), new Constant<>(1));

    BigListWriter<Integer> inputs = new BigListWriter<>(10000);
    BigListWriter<Double> expectedOutputs2 = new BigListWriter<>(10000);
    for (int i = 0; i < 10000; i++) {
      inputs.write(i);
      expectedOutputs2.write((double) (i + 1));
    }

    DAG1x2.Prepared<Integer, Integer, Double> dag =
        DAG.Prepared.withPlaceholder(intPlaceholder).withOutputs(intPlaceholder, indexGenerator).withExecutor(executor);

    DAG1x2.Prepared.Result<Integer, Double> res = dag.applyAll(inputs.createReader());

    assertTrue(ObjectReader.equals(inputs.createReader(), res.getResult1()));
    assertTrue(ObjectReader.equals(expectedOutputs2.createReader(), res.getResult2()));
  }

  @ParameterizedTest
  @MethodSource("preparableExecutors")
  public void testDeepGraph(DAGExecutor executor) {
    Random r = new Random(0);

    Placeholder<Long> p0s1 = new Placeholder<>();
    Placeholder<Long> p0s2 = new Placeholder<>();
    ExampleIndex p0g1 = new ExampleIndex();

    DelayedIdentity.Prepared<Long> p0d1a = new DelayedIdentity.Prepared<Long>(r.nextInt(5)).withInput(p0s1);
    DelayedIdentity.Prepared<Long> p0d2g = new DelayedIdentity.Prepared<Long>(r.nextInt(5)).withInput(p0g1);
    DelayedIdentity.Prepared<Long> p0d3g = new DelayedIdentity.Prepared<Long>(r.nextInt(5)).withInput(p0g1);
    DelayedIdentity.Prepared<Long> p0d4 = new DelayedIdentity.Prepared<Long>(r.nextInt(5)).withInput(p0s2);
    DelayedIdentity.Prepared<Long> p0d5 = new DelayedIdentity.Prepared<Long>(r.nextInt(5)).withInput(p0s2);
    DelayedIdentity.Prepared<Long> p0d1 = new DelayedIdentity.Prepared<Long>(r.nextInt(5)).withInput(p0d1a);

    DelayedIdentity<Long> p1d1 = new DelayedIdentity<Long>(r.nextInt(5)).withInput(p0s1);
    DelayedIdentity<Long> p1d2g = new DelayedIdentity<Long>(r.nextInt(5)).withInput(p0d2g);

    DelayedIdentity.ProcessedCountView p1d1View = new DelayedIdentity.ProcessedCountView(p1d1);

    IsMatch<Long> p1gm1 = new IsMatch<Long>().withInputs(p1d2g, p0d1);
    IsMatch<Long> p1gm2 = new IsMatch<Long>().withInputs(p0d3g, p1d1);

    DelayedIdentity<Long> p2gm = new DelayedIdentity<Long>(r.nextInt(5)).withInput(p1gm1);
    DelayedIdentity.ProcessedCountView p2gmView = new DelayedIdentity.ProcessedCountView(p1d1);

    IsMatch<Long> p3gm = new IsMatch<Long>().withInputs(p1gm2, p0d5);
    IsMatch<Long> p3gm2 = new IsMatch<Long>().withInputs(p3gm, p3gm);

    IsMatch<Long> p3gm3 = new IsMatch<Long>().withInputs(p2gm, p3gm2);

    DelayedIdentity<Long> p4gm = new DelayedIdentity<Long>(r.nextInt(5)).withInput(p3gm3);
    DelayedIdentity<Long> p5gm = new DelayedIdentity<Long>(r.nextInt(5)).withInput(p4gm);

    IsMatch<Long> p5gm2 = new IsMatch<Long>().withInputs(p5gm, p2gm);

    DelayedIdentity<Long> p6gm = new DelayedIdentity<Long>(r.nextInt(5)).withInput(p0d4);

    IsMatch<Long> p6gm2 = new IsMatch<Long>().withInputs(p5gm2, p6gm);
    IsMatch<Long> t1 = new IsMatch<Long>().withInputs(p6gm2, p6gm2);

    DelayedIdentity<Long> t2 = new DelayedIdentity<Long>(r.nextInt(5)).withInput(p0g1);

    Sum sum = new Sum().withInputs(t2, p2gmView);

    BigListWriter<Long> placeholder = new BigListWriter<>(10000);
    BigListWriter<Long> t2r = new BigListWriter<>(10000);
    BigListWriter<Long> t1r = new BigListWriter<>(10000);

    for (long i = 0; i < 10000; i++) {
      placeholder.write(i % 2 == 0 ? i : -1);
      t1r.write(i % 2 == 0 ? i : null);
      t2r.write(i);
    }

    DAG2x4<Long, Long, Long, Long, Long, Long> dag = DAG.withPlaceholders(p0s1, p0s2).withOutputs(t1, t2, p1d1View, sum).withExecutor(executor);
    DAG2x4.Result<Long, Long, Long, Long, Long, Long> res = dag.prepareAndApply(placeholder.createReader(), placeholder.createReader());
    try (ObjectReader<Long> reader = t1r.createReader()) {
      assertTrue(ObjectReader.equals(res.getResult1(), reader));
    }
    try (ObjectReader<Long> reader = t2r.createReader()) {
      assertTrue(ObjectReader.equals(res.getResult2(), reader));
    }

    assertEquals(res.getPreparedDAG().apply(2L, 2L), Tuple4.of(null, 0L, placeholder.size64(), placeholder.size64()));

    DynamicDAG<Tuple4<Long, Long, Long, Long>> dynamicDAG =
        new DynamicDAG<>().withPlaceholders(p0s1, p0s2).withOutputs(t1, t2, p1d1View, sum).withExecutor(executor);
    DynamicDAGResult<Tuple4<Long, Long, Long, Long>> dynamicDAGResult = dynamicDAG.prepareAndApply()
        .input(p0s1, placeholder.createReader())
        .input(p0s2, placeholder.createReader())
        .execute();
    try (ObjectReader<Long> reader = t1r.createReader()) {
      assertTrue(ObjectReader.equals(dynamicDAGResult.getResult(t1), reader));
    }
    try (ObjectReader<Long> reader = t2r.createReader()) {
      assertTrue(ObjectReader.equals(dynamicDAGResult.getResult(t2), reader));
    }
    assertEquals(dynamicDAGResult.getPreparedDAG().apply().input(p0s1, 2L).input(p0s2, 2L).execute(),
        Tuple4.of(null, 0L, placeholder.size64(), placeholder.size64()));
  }

  @ParameterizedTest
  @MethodSource("preparedExecutors")
  public void testEmbeddedDAG(PreparedDAGExecutor executor) {
    Placeholder<Long> summand1Placeholder = new Placeholder<>();
    Placeholder<Long> summand2Placeholder = new Placeholder<>();
    FunctionResult2<Long, Long, Long> sum =
        new FunctionResult2<Long, Long, Long>().withFunctionUnsafe((Long a, Long b) -> a + b)
            .withInputs(summand1Placeholder, summand2Placeholder);

    DAG2x1.Prepared<Long, Long, Long> sumDAG =
        DAG.Prepared.withPlaceholders(summand1Placeholder, summand2Placeholder).withOutput(sum).withExecutor(executor);

    Placeholder<Long> operandPlaceholder = new Placeholder<>();
    sumDAG = sumDAG.withInputs(new ExampleIndex(), operandPlaceholder);
    DAG1x1.Prepared<Long, Long> sequentialDAG =
        DAG.Prepared.withPlaceholder(operandPlaceholder).withNoReduction().withOutput(sumDAG).withExecutor(executor);

    assertEquals((long) sequentialDAG.apply(3L), 3);
  }

  @ParameterizedTest
  @MethodSource("preparableExecutors")
  public void basicViewTest(DAGExecutor executor) {
    Placeholder<String> input = new Placeholder<>();
    DelayedIdentity<String> delayedIdentity = new DelayedIdentity<String>(0).withInput(input);

    DelayedIdentity.ProcessedCountView pcView = new DelayedIdentity.ProcessedCountView(delayedIdentity);

    DAG1x2<String, String, Long> dag =
        DAG.withPlaceholder(input).withOutputs(delayedIdentity, pcView).withExecutor(executor);
    DAG1x2.Result<String, String, Long> prepared = dag.prepareAndApply(
        java.util.Arrays.asList("One", "Two", "Three", "Four"));
    assertEquals((long) prepared.getResult2().toList().get(0), 4);
    assertEquals((long) prepared.getResult2().toList().get(3), 4);
    assertEquals(prepared.getResult1().toList().get(0), "One");
    assertEquals(prepared.getResult1().toList().get(2), "Three");
  }

  @ParameterizedTest
  @MethodSource("preparableExecutors")
  public void basicViewTest2(DAGExecutor executor) {
    Placeholder<String> input = new Placeholder<>();
    DelayedIdentity<String> delayedIdentity = new DelayedIdentity<String>(0).withInput(input);

    DelayedIdentity.ProcessedCountView pcView = new DelayedIdentity.ProcessedCountView(delayedIdentity);

    DAG1x1<String, Long> dag = DAG.withPlaceholder(input).withOutput(pcView).withExecutor(executor);
    DAG1x1.Result<String, Long> prepared = dag.prepareAndApply(java.util.Arrays.asList("One", "Two", "Three", "Four"));

    assertEquals((long) prepared.toList().get(0), 4);
    assertEquals((long) prepared.toList().get(3), 4);
  }

  @ParameterizedTest
  @MethodSource("preparableExecutors")
  public void basicViewTest3(DAGExecutor executor) {
    Placeholder<String> input = new Placeholder<>();
    DelayedIdentity<String> delayedIdentity = new DelayedIdentity<String>(0).withInput(input);

    DelayedIdentity.ProcessedCountView pcView1 = new DelayedIdentity.ProcessedCountView(delayedIdentity);
    DelayedIdentity.ProcessedCountView pcView2 = new DelayedIdentity.ProcessedCountView(delayedIdentity);

    Sum sum = new Sum().withInputs(pcView1, pcView2);
    DelayedIdentity<Long> sumIdentity = new DelayedIdentity<Long>(0).withInput(sum);

    DAG1x3<String, Long, Long, Long> dag = DAG.withPlaceholder(input).withOutputs(sum, pcView2, sumIdentity).withExecutor(executor);
    DAG1x3.Result<String, Long, Long, Long> prepared = dag.prepareAndApply(
        java.util.Arrays.asList("One", "Two", "Three", "Four"));

    assertEquals((long) prepared.toList().get(0).get0(), 8);
    assertEquals((long) prepared.toList().get(3).get0(), 8);
    assertEquals((long) prepared.toList().get(0).get1(), 4);
    assertEquals((long) prepared.toList().get(3).get1(), 4);
    assertEquals((long) prepared.toList().get(0).get2(), 8);
    assertEquals((long) prepared.toList().get(3).get2(), 8);
  }

  @ParameterizedTest
  @MethodSource("preparableExecutors")
  public void basicViewBatchTest(DAGExecutor executor) {
    Placeholder<String> input = new Placeholder<>();
    DelayedIdentityBatchPrepared<String> delayedIdentity = new DelayedIdentityBatchPrepared<String>(0).withInput(input);
    DelayedIdentityBatchPrepared.ProcessedCountView pcView = new DelayedIdentityBatchPrepared.ProcessedCountView(delayedIdentity);

    DAG1x2<String, String, Long> dag =
        DAG.withPlaceholder(input).withOutputs(delayedIdentity, pcView).withExecutor(executor);
    DAG1x2.Result<String, String, Long> prepared = dag.prepareAndApply(
        java.util.Arrays.asList("One", "Two", "Three", "Four"));
    assertEquals((long) prepared.getResult2().toList().get(0), 4);
    assertEquals((long) prepared.getResult2().toList().get(3), 4);
    assertEquals(prepared.getResult1().toList().get(0), "One");
    assertEquals(prepared.getResult1().toList().get(2), "Three");
  }

  @ParameterizedTest
  @MethodSource("preparableExecutors")
  public void basicViewBatchTest2(DAGExecutor executor) {
    Placeholder<String> input = new Placeholder<>();
    DelayedIdentityBatchPrepared<String> delayedIdentity = new DelayedIdentityBatchPrepared<String>(0).withInput(input);

    DelayedIdentityBatchPrepared.ProcessedCountView pcView = new DelayedIdentityBatchPrepared.ProcessedCountView(delayedIdentity);

    DAG1x1<String, Long> dag = DAG.withPlaceholder(input).withOutput(pcView).withExecutor(executor);
    DAG1x1.Result<String, Long> prepared = dag.prepareAndApply(java.util.Arrays.asList("One", "Two", "Three", "Four"));

    assertEquals((long) prepared.toList().get(0), 4);
    assertEquals((long) prepared.toList().get(3), 4);
  }

  @ParameterizedTest
  @MethodSource("preparableExecutors")
  public void basicViewBatchTest3(DAGExecutor executor) {
    Placeholder<String> input = new Placeholder<>();
    DelayedIdentity<String> delayedIdentity = new DelayedIdentity<String>(0).withInput(input);

    DelayedIdentity.ProcessedCountView pcView1 = new DelayedIdentity.ProcessedCountView(delayedIdentity);
    DelayedIdentity.ProcessedCountView pcView2 = new DelayedIdentity.ProcessedCountView(delayedIdentity);

    Sum sum = new Sum().withInputs(pcView1, pcView2);
    DelayedIdentityBatchPrepared<Long> sumIdentity = new DelayedIdentityBatchPrepared<Long>(0).withInput(sum);

    DAG1x3<String, Long, Long, Long> dag = DAG.withPlaceholder(input).withOutputs(sum, pcView2, sumIdentity).withExecutor(executor);
    DAG1x3.Result<String, Long, Long, Long> prepared = dag.prepareAndApply(
        java.util.Arrays.asList("One", "Two", "Three", "Four"));

    assertEquals((long) prepared.toList().get(0).get0(), 8);
    assertEquals((long) prepared.toList().get(3).get0(), 8);
    assertEquals((long) prepared.toList().get(0).get1(), 4);
    assertEquals((long) prepared.toList().get(3).get1(), 4);
    assertEquals((long) prepared.toList().get(0).get2(), 8);
    assertEquals((long) prepared.toList().get(3).get2(), 8);
  }

  @Test
  public void testWithInputs() {
    // make sure withInputs() doesn't replace the original placeholders unnecessarily when used instead of withPlaceholders()
    // (although withPlaceholders() remains the recommended method)
    Placeholder<Long> placeholderA = new Placeholder<>();
    Placeholder<Long> placeholderB = new Placeholder<>();
    Sum unusedSum = new Sum().withInputs(placeholderA, placeholderB);
    Sum sum = new Sum().withInputs(placeholderA, placeholderB);

    DAG3x1<Long, Long, Long, Long> dag = DAG.withInputs(placeholderB, placeholderA, unusedSum).withOutput(sum);
    Assertions.assertTrue(dag.graph().getParentToChildrenMap().containsKey(placeholderA));
    Assertions.assertTrue(dag.graph().getParentToChildrenMap().containsKey(placeholderB));
  }

  @Test
  public void testDeepGraphPrepareForMDE() {
    Random r = new Random(0);

    Placeholder<Long> p0s1 = new Placeholder<>();
    Placeholder<Long> p0s2 = new Placeholder<>();
    ExampleIndex p0g1 = new ExampleIndex();

    DelayedIdentity.Prepared<Long> p0d1a = new DelayedIdentity.Prepared<Long>(r.nextInt(5)).withInput(p0s1);
    DelayedIdentity.Prepared<Long> p0d2g = new DelayedIdentity.Prepared<Long>(r.nextInt(5)).withInput(p0g1);
    DelayedIdentity.Prepared<Long> p0d3g = new DelayedIdentity.Prepared<Long>(r.nextInt(5)).withInput(p0g1);
    DelayedIdentity.Prepared<Long> p0d4 = new DelayedIdentity.Prepared<Long>(r.nextInt(5)).withInput(p0s2);
    DelayedIdentity.Prepared<Long> p0d5 = new DelayedIdentity.Prepared<Long>(r.nextInt(5)).withInput(p0s2);
    DelayedIdentity.Prepared<Long> p0d1 = new DelayedIdentity.Prepared<Long>(r.nextInt(5)).withInput(p0d1a);

    DelayedIdentity<Long> p1d1 = new DelayedIdentity<Long>(r.nextInt(5)).withInput(p0s1);
    DelayedIdentity<Long> p1d2g = new DelayedIdentity<Long>(r.nextInt(5)).withInput(p0d2g);

    DelayedIdentity.ProcessedCountView p1d1View = new DelayedIdentity.ProcessedCountView(p1d1);

    IsMatch<Long> p1gm1 = new IsMatch<Long>().withInputs(p1d2g, p0d1);
    IsMatch<Long> p1gm2 = new IsMatch<Long>().withInputs(p0d3g, p1d1);

    DelayedIdentity<Long> p2gm = new DelayedIdentity<Long>(r.nextInt(5)).withInput(p1gm1);
    DelayedIdentity.ProcessedCountView p2gmView = new DelayedIdentity.ProcessedCountView(p1d1);

    IsMatch<Long> p3gm = new IsMatch<Long>().withInputs(p1gm2, p0d5);
    IsMatch<Long> p3gm2 = new IsMatch<Long>().withInputs(p3gm, p3gm);

    IsMatch<Long> p3gm3 = new IsMatch<Long>().withInputs(p2gm, p3gm2);

    DelayedIdentity<Long> p4gm = new DelayedIdentity<Long>(r.nextInt(5)).withInput(p3gm3);
    DelayedIdentity<Long> p5gm = new DelayedIdentity<Long>(r.nextInt(5)).withInput(p4gm);

    IsMatch<Long> p5gm2 = new IsMatch<Long>().withInputs(p5gm, p2gm);

    DelayedIdentity<Long> p6gm = new DelayedIdentity<Long>(r.nextInt(5)).withInput(p0d4);

    IsMatch<Long> p6gm2 = new IsMatch<Long>().withInputs(p5gm2, p6gm);
    IsMatch<Long> t1 = new IsMatch<Long>().withInputs(p6gm2, p6gm2);

    DelayedIdentity<Long> t2 = new DelayedIdentity<Long>(r.nextInt(5)).withInput(p0g1);

    Sum sum = new Sum().withInputs(t2, p2gmView);

    BigListWriter<Long> placeholder = new BigListWriter<>(10000);
    BigListWriter<Long> t2r = new BigListWriter<>(10000);
    BigListWriter<Long> t1r = new BigListWriter<>(10000);

    for (long i = 0; i < 10000; i++) {
      placeholder.write(i % 2 == 0 ? i : -1);
      t1r.write(i % 2 == 0 ? i : null);
      t2r.write(i);
    }

    DAG2x4<Long, Long, Long, Long, Long, Long> dag =
        DAG.withPlaceholders(p0s1, p0s2).withOutputs(t1, t2, p1d1View, sum).withExecutor(new MultithreadedDAGExecutor());
    DAG2x4.Prepared<Long, Long, Long, Long, Long, Long> res =
        dag.prepare(placeholder.createReader(), placeholder.createReader());
    DAG2x4.Prepared.Result<Long, Long, Long, Long> application =
        res.applyAll(placeholder.createReader(), placeholder.createReader());
    try (ObjectReader<Long> reader = t1r.createReader()) {
      assertTrue(ObjectReader.equals(application.getResult1(), reader));
    }
    try (ObjectReader<Long> reader = t2r.createReader()) {
      assertTrue(ObjectReader.equals(application.getResult2(), reader));
    }

    assertEquals(res.apply(2L, 2L), Tuple4.of(null, 0L, placeholder.size64(), placeholder.size64()));
  }

  @Test
  public void testSimpleGraphPrepareForMDE() {
    Placeholder<Integer> intPlaceholder = new Placeholder<>();
    DeadlyPreparableTransformer deadly = new DeadlyPreparableTransformer().withInput(intPlaceholder);
    DeadlyPreparableTransformer.View deadlyView = new DeadlyPreparableTransformer.View(deadly);
    DeadlyPreparableTransformer.Prepared deadlier = new DeadlyPreparableTransformer.Prepared().withInput(deadly);
    Sum sum = new Sum().withInputs(intPlaceholder, deadlyView);
    Sum deadlySum = new Sum().withInputs(deadlier, deadlyView);

    DAG1x1.Prepared<Integer, Long> nonDeadlyDAG =
        DAG.withPlaceholder(intPlaceholder).withOutput(sum).prepare(java.util.Arrays.asList(1, 2, 3));

    Assertions.assertEquals(11, (long) nonDeadlyDAG.apply(4));

    DAG1x1.Prepared<Integer, Long> deadlyDAG =
        DAG.withPlaceholder(intPlaceholder).withOutput(deadlySum).prepare(java.util.Arrays.asList(1, 2, 3));

    try {
      deadlyDAG.apply(4);
      Assertions.fail();
    } catch (UnsupportedOperationException e) { }
  }
}