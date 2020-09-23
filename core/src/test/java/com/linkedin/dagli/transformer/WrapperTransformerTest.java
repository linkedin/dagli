package com.linkedin.dagli.transformer;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.dag.DAG;
import com.linkedin.dagli.function.FunctionResult2;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.tester.Tester;
import com.linkedin.dagli.util.collection.LinkedNode;
import org.junit.jupiter.api.Test;

public class WrapperTransformerTest {
  @ValueEquality
  private static class Function2Wrapper
      extends WrapperTransformer2.Prepared<Integer, Integer, Integer, Function2Wrapper> {
    private static final long serialVersionUID = 1;

    public Function2Wrapper(Producer<Integer> in1, Producer<Integer> in2) {
      super(new FunctionResult2<>(Integer::sum).withInputs(in1, in2));
    }
  }

  @ValueEquality
  private static class PreparableFunction2Wrapper
      extends WrapperTransformer2<Integer, Integer, Integer, PreparableFunction2Wrapper> {
    private static final long serialVersionUID = 1;

    public PreparableFunction2Wrapper(Producer<Integer> in1, Producer<Integer> in2) {
      super(DAG.withInputs(in1, in2)
          .withOutput(
              new TriviallyPreparableTransformation<>(new FunctionResult2<>(Integer::sum).withInputs(in1, in2))));
    }
  }

  @Test
  public void test() {
    Placeholder<Integer> in1 = new Placeholder<>();
    Placeholder<Integer> in2 = new Placeholder<>();

    Tester.of(new Function2Wrapper(in1, in2))
        .input(3, 4)
        .output(7)
        .reductionTest(stream -> LinkedNode.filterByClass(stream, Function2Wrapper.class).count() == 0)
        .test();

    Tester.of(new PreparableFunction2Wrapper(in1, in2))
        .input(3, 4)
        .output(7)
        .reductionTest(stream -> LinkedNode.filterByClass(stream, PreparableFunction2Wrapper.class).count() == 0)
        .test();
  }
}
