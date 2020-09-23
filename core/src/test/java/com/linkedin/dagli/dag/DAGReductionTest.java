package com.linkedin.dagli.dag;

import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.transformer.ConstantResultTransformation1;
import com.linkedin.dagli.view.PreparedTransformerView;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class DAGReductionTest {
  @Test
  void basicConstantReduction() {
    Placeholder<Integer> placeholder = new Placeholder<>();
    Constant<Long> constant = new Constant<>(42L);
    DAG1x1<Long, Long> l42 = subgraph().withInput(constant);

    PreparedTransformerView<DAG1x1.Prepared<Long, Long>> preparedDAGView = new PreparedTransformerView<>(l42);

    ConstantResultTransformation1<Integer, Long> l99 =
        new ConstantResultTransformation1<Integer, Long>().withResult(99L).withInput(placeholder);

    ConstantResultTransformation1.Prepared<Integer, Long> l101 =
        new ConstantResultTransformation1.Prepared<Integer, Long>().withResult(101L).withInput(placeholder);

    Sum l141 = new Sum().withInputs(l42, l99);
    Sum l242 = new Sum().withInputs(l141, l101);
    Sum l484 = new Sum().withInputs(l242, l242);

    DAG1x2<Integer, Long, DAG1x1.Prepared<Long, Long>> dag =
        DAG.withPlaceholder(placeholder).withOutputs(l484, preparedDAGView);

    Assertions.assertEquals(3, dag.graph().getParentToChildrenMap().size());
    Assertions.assertTrue(dag.graph()
        .getParentToChildrenMap()
        .keySet()
        .stream()
        .filter(producer -> producer instanceof Constant)
        .anyMatch(producer -> ((Constant<?>) producer).getValue().equals(484L)));

    Assertions.assertTrue(dag.graph()
        .getParentToChildrenMap()
        .keySet()
        .stream()
        .filter(producer -> producer instanceof Constant)
        .anyMatch(producer -> ((Constant<?>) producer).getValue() instanceof DAG1x1.Prepared));

    Assertions.assertEquals(484, dag.prepare(Collections.singletonList(4)).apply(2342).get0());
    Assertions.assertTrue(dag.prepare(Collections.singletonList(4)).apply(2342).get1() instanceof DAG1x1.Prepared);
  }

  private static DAG1x1<Long, Long> subgraph() {
    Placeholder<Long> placeholder = new Placeholder<>();
    DelayedIdentity<Long> result = new DelayedIdentity<Long>(0).withInput(placeholder);
    return DAG.withPlaceholder(placeholder).withOutput(result);
  }
}
