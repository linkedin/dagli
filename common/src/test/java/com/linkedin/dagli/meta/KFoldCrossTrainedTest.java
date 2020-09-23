package com.linkedin.dagli.meta;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.dag.LocalDAGExecutor;
import com.linkedin.dagli.function.FunctionResult2;
import com.linkedin.dagli.preparer.AbstractStreamPreparer2;
import com.linkedin.dagli.preparer.PreparerContext;
import com.linkedin.dagli.preparer.PreparerResult;
import com.linkedin.dagli.preparer.PreparerResultMixed;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.tester.Tester;
import com.linkedin.dagli.transformer.AbstractPreparableTransformer2;
import com.linkedin.dagli.transformer.PreparedTransformer;
import com.linkedin.dagli.util.function.Function2;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class KFoldCrossTrainedTest {
  @ValueEquality
  private static class TestPreparable
      extends AbstractPreparableTransformer2<Long, Long, Long, FunctionResult2<Long, Long, Long>, TestPreparable> {

    private static final long serialVersionUID = 1;

    public TestPreparable() {
      super(MissingInput.get(), MissingInput.get());
    }

    public static class Preparer extends AbstractStreamPreparer2<Long, Long, Long, FunctionResult2<Long, Long, Long>> {

      public static HashMap<Long, HashSet<Preparer>> _seen = new HashMap<>();

      private HashSet<Long> _trained = new HashSet<>();

      // simple function that just checks if the first argument matches something seen during training, returning 1 if
      // so, 0 otherwise
      private static class TrainedIndicator implements Function2.Serializable<Long, Long, Long> {
        private HashSet<Long> _trained;

        public TrainedIndicator(HashSet<Long> trained) {
          _trained = trained;
        }

        @Override
        public Long apply(Long value0, Long value1) {
          return _trained.contains(value0) ? 1L : 0L;
        }

        @Override
        public boolean equals(Object o) {
          if (this == o) {
            return true;
          }
          if (o == null || getClass() != o.getClass()) {
            return false;
          }
          TrainedIndicator that = (TrainedIndicator) o;
          return _trained.equals(that._trained);
        }

        @Override
        public int hashCode() {
          return Objects.hash(_trained);
        }
      }

      @Override
      public PreparerResult<FunctionResult2<Long, Long, Long>> finish() {
        return new PreparerResult<>(
            new FunctionResult2<Long, Long, Long>().withFunction(new TrainedIndicator(_trained)));
      }

      @Override
      public void process(Long valueA, Long valueB) {
        if (!_seen.containsKey(valueA)) {
          _seen.put(valueA, new HashSet<>());
        }

        _seen.get(valueA).add(this);
        _trained.add(valueA);
      }
    }

    @Override
    public Preparer getPreparer(PreparerContext context) {
      return new Preparer();
    }
  }

  @Test
  public void basicTest() {
    List<Long> values = LongStream.range(0, 100).boxed().collect(Collectors.toList());
    KFoldCrossTrained<Long> kfold = new KFoldCrossTrained<Long>().withSplitCount(10).withPreparable(new TestPreparable());

    // conduct some basic tests first
    Tester.of(kfold)
        .allParallelInputs(values, values, values)
        .allOutputs(values.stream().map(v -> 0L).collect(Collectors.toList())) // all outputs should be 0
        .test();

    TestPreparable.Preparer._seen.clear(); // clear static field (modified by basic testing above)

    // need to prepare with three values, because last one corresponds to the "group" input
    PreparerResultMixed<? extends PreparedTransformer<? extends Long>, PreparedTransformer<Long>> res =
        kfold.internalAPI()
            .prepareUnsafe(new LocalDAGExecutor().withMaxThreads(1), new Collection[]{values, values, values});


    // check that each example was seen the correct number of times
    for (Long v : values) {
      assertEquals(TestPreparable.Preparer._seen.get(v).size(), 10); // 9 folds + 1 final fold
    }

    PreparedTransformer<Long> forNewData =
        res.getPreparedTransformerForNewData().internalAPI().withInputsUnsafe(MissingInput.producerList(3));
    PreparedTransformer<? extends Long> forPrepData =
        res.getPreparedTransformerForPreparationData().internalAPI().withInputsUnsafe(MissingInput.producerList(3));


    Object newExecutionCache = forNewData.internalAPI().createExecutionCache(values.size());
    Object prepExecutionCache = forPrepData.internalAPI().createExecutionCache(values.size());

    // check that Preparers were prepared properly
    for (Long v : values) {
      assertEquals((long) forNewData.internalAPI().applyUnsafe(newExecutionCache, new Long[]{v, v, v}), 1L);
    }

    for (Long v : values) {
      assertEquals((long) forPrepData.internalAPI().applyUnsafe(prepExecutionCache, new Long[]{v, v, v}), 0L);
    }
  }

}
