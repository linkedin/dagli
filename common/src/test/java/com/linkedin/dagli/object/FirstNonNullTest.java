package com.linkedin.dagli.object;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG1x1;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.placeholder.Placeholder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class FirstNonNullTest {
  @Test
  public void testReduction() {
    Constant<Integer> input1 = Constant.nullValue();
    Constant<Integer> input2 = new Constant<>(42);
    Placeholder<Integer> input3 = new Placeholder<>();

    DAG1x1.Prepared<Integer, Integer> dag =
        DAG.Prepared.withPlaceholder(input3).withOutput(new FirstNonNull<Integer>().withInputs(input1, input2, input3));

    Assertions.assertEquals(2, dag.graph().getParentToChildrenMap().size());
    Assertions.assertEquals(42, dag.apply(123));

    DAG1x1.Prepared<Integer, Integer> dag2 =
        DAG.Prepared.withPlaceholder(input3).withOutput(new FirstNonNull<Integer>().withInputs(input1, input3, input2));

    Assertions.assertEquals(4, dag2.graph().getParentToChildrenMap().size());
  }
}
