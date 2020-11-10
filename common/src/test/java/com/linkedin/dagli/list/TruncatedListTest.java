package com.linkedin.dagli.list;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG1x1;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.tester.Tester;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class TruncatedListTest {
  @Test
  public void reductionTest() {
    Placeholder<List<Integer>> list = new Placeholder<>();
    DAG1x1.Prepared<List<Integer>, List<? extends Integer>> dag = DAG.Prepared.withPlaceholder(list).withOutput(
        new TruncatedList<Integer>().withMaxSize(8).withListInput(
        new TruncatedList<Integer>().withMaxSize(4).withListInput(
        new TruncatedList<Integer>().withMaxSize(6).withListInput(list))));


    Assertions.assertEquals(1, dag.producers(TruncatedList.class).count());
    Assertions.assertEquals(4, Constant.tryGetValue((Producer<Integer>)
        dag.producers(TruncatedList.class).findFirst().get().peek().internalAPI().getInputList().get(1)));
  }

  @Test
  public void test() {
    List<String> list2 = Arrays.asList("1", "2");

    Tester.of(new TruncatedList<String>())
        .input(Collections.emptyList(), 2)
        .output(Collections.emptyList())
        .input(list2, 2)
        .outputTest(o -> o == list2)
        .input(Arrays.asList("1", "2", "3"), 2)
        .output(list2)
        .test();
  }
}
