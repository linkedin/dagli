package com.linkedin.dagli.meta;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG1x1;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.list.VariadicList;
import com.linkedin.dagli.object.Rank;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.tester.Tester;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class NullFilteredTest {
  @Test
  public void testReduction() {
    Placeholder<Integer> input1 = new Placeholder<>();
    Constant<Integer> input2 = Constant.nullValue();

    DAG1x1.Prepared<Integer, java.util.List<Integer>> dag = DAG.Prepared.withPlaceholder(input1)
        .withOutput(new NullFiltered.Prepared<>(new VariadicList<Integer>().withInputs(input1, input2)));

    Assertions.assertEquals(2, dag.graph().getParentToChildrenMap().size());
    Assertions.assertNull(dag.apply(42));
  }

  @Test
  public void testPreparationAndApplication() {
    Placeholder<Integer> intPlaceholder = new Placeholder<>();
    NullFiltered<Integer> nullFiltered = new NullFiltered<Integer>().withTransformer(new Rank().withInput(intPlaceholder))
        .withFilteredPreparation(true)
        .withFilteredApplication(true);
    DAG1x1<Integer, Integer> dag = DAG.withPlaceholder(intPlaceholder).withOutput(nullFiltered);

    Tester.of(dag)
        .allParallelInputs(Arrays.asList(null, 3, 2, null, 1, 3, null))
        .allOutputs(Arrays.asList(null, 2, 1, null, 0, 2, null))
        .test();
  }
}
