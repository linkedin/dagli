package com.linkedin.dagli.transformer;

import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.dag.DAG2x3;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.tuple.Tuple3;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class ValueFromTupleTest {
  @Test
  public void test() {
    Placeholder<Integer> placeholder1 = new Placeholder<>();
    Placeholder<String> placeholder2 = new Placeholder<>();
    Tupled3<Integer, String, Integer> tupled = new Tupled3<>(placeholder1, placeholder2, placeholder1);
    Value0FromTuple<Integer> val1 = new Value0FromTuple<>(tupled);
    Value1FromTuple<String> val2 = new Value1FromTuple<>(tupled);
    Value2FromTuple<Integer> val3 = new Value2FromTuple<>(tupled);

    DAG2x3.Prepared<Integer, String, Integer, String, Integer> dag =
        DAG.Prepared.withPlaceholders(placeholder1, placeholder2).withOutputs(val1, val2, val3);

    Assertions.assertEquals(2, dag.graph().getParentToChildrenMap().size());
    Assertions.assertEquals(Tuple3.of(42, "a", 42), dag.apply(42, "a"));
  }
}
