package com.linkedin.dagli.object;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG1x1;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.tester.Tester;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class ConditionalValueTest {
  @Test
  public void basicTest() {
    Tester.of(new ConditionalValue<>())
        .input(true, 1, 2)
        .input(false, 1, 2)
        .output(1)
        .output(2)
        .test();
  }

  @Test
  public void reductionTest() {
    Placeholder<Integer> placeholder = new Placeholder<>();
    Constant<Boolean> conditionVal = new Constant<>(true);
    Constant<Integer> val1 = new Constant<>(42);
    Constant<String> val2 = new Constant<>("abc");

    DAG1x1.Prepared<Integer, Object> dag = DAG.Prepared.withPlaceholder(placeholder)
        .withOutput(new ConditionalValue<>().withConditionInput(conditionVal)
            .withValueIfConditionTrueInput(val1)
            .withValueIfConditionFalseInput(val2));

    Assertions.assertEquals(2, dag.graph().getParentToChildrenMap().size());
    Assertions.assertEquals(42, dag.apply(123));
  }
}
