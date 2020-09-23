package com.linkedin.dagli.handle;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG1x5;
import com.linkedin.dagli.dag.DAG2x3;
import com.linkedin.dagli.dag.DAGExecutor;
import com.linkedin.dagli.dag.DAGTest;
import com.linkedin.dagli.dag.DelayedIdentity;
import com.linkedin.dagli.dag.PreparedDAGExecutor;
import com.linkedin.dagli.dag.Sum;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.tuple.Tuple3;
import com.linkedin.dagli.tuple.Tuple5;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * These tests have been greatly reduced from their original purpose as most of handles' former functionality has been
 * removed, but are retained for the tests that remain.
 */
public class HandleTest {
  // used for testing
  private static class SumSubclass extends Sum {
    private static final long serialVersionUID = 1;
  }

  @Test
  public void unorderedInputsTest() {
    Placeholder<Long> input1 = new Placeholder<>();
    Placeholder<Long> input2 = new Placeholder<>();

    Sum sum1 = new Sum().withInputs(input1, input2);
    Sum sum2 = new Sum().withInputs(input2, input1);
    Sum sum3 = new SumSubclass().withInputs(input2, input1);

    Assertions.assertEquals(sum1, sum2);
    Assertions.assertEquals(sum1, sum3);

    DAG2x3.Prepared<Long, Long, Long, Long, Long> dag =
        DAG.Prepared.withPlaceholders(input1, input2).withOutputs(sum1, sum2, sum3);

    for (PreparedDAGExecutor executor : DAGTest.PREPARED_EXECUTORS) {
      Assertions.assertEquals(dag.withExecutor(executor).apply(5L, 6L), Tuple3.of(11L, 11L, 11L));
    }
  }

  @Test
  public void preparedHandleTest() {
    Placeholder<Long> input = new Placeholder<>();

    DelayedIdentity<Long> delayedIdentityA1 = new DelayedIdentity<Long>(0).withInput(input);
    DelayedIdentity<Long> delayedIdentityA2 = new DelayedIdentity<Long>(0).withInput(input);
    DelayedIdentity<Long> delayedIdentityA3 = new DelayedIdentity<Long>(0).withInput(input);

    DelayedIdentity<Long> delayedIdentityB1 = new DelayedIdentity<Long>(0).withInput(delayedIdentityA1);
    DelayedIdentity<Long> delayedIdentityB2 = new DelayedIdentity<Long>(0).withInput(delayedIdentityA1);
    DelayedIdentity<Long> delayedIdentityB3 = new DelayedIdentity<Long>(0).withInput(delayedIdentityA2);
    DelayedIdentity<Long> delayedIdentityB4 = new DelayedIdentity<Long>(0).withInput(delayedIdentityA3);

    DelayedIdentity<Long> delayedIdentityC1 = new DelayedIdentity<Long>(0).withInput(delayedIdentityB1);

    DAG1x5<Long, Long, Long, Long, Long, Long> preparableDAG = DAG.withPlaceholder(input)
        .withOutputs(delayedIdentityC1, delayedIdentityA1, delayedIdentityB2, delayedIdentityB3, delayedIdentityB4);


    for (DAGExecutor executor : DAGTest.PREPARABLE_EXECUTORS) {
      DAG1x5.Prepared<Long, Long, Long, Long, Long, Long> preparedDAG =
          preparableDAG.withExecutor(executor).prepare(Arrays.asList(1L, 2L, 3L));

      Assertions.assertEquals(preparedDAG.apply(3L), Tuple5.of(3L, 3L, 3L, 3L, 3L));
    }
  }
}
