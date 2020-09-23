package com.linkedin.dagli.producer;

import com.linkedin.dagli.annotation.equality.IgnoredByValueEquality;
import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class ChildProducerEqualityTest {
  @ValueEquality
  private static class A extends AbstractPreparedTransformer2<Integer, Integer, Integer, A> {
    private static final long serialVersionUID = 1;

    private int _v1;
    private int _v2;

    public A withInputs(Producer<? extends Integer> input0, Producer<? extends Integer> input1) {
      return super.withAllInputs(input0, input1);
    }

    @IgnoredByValueEquality
    private int _ignored;

    public A(int v1, int v2, int ignored) {
      _v1 = v1;
      _v2 = v2;
      _ignored = ignored;
    }

    @Override
    public Integer apply(Integer value0, Integer value1) {
      return null;
    }
  }

  @ValueEquality(commutativeInputs = true)
  private static class B extends A {
    private static final long serialVersionUID = 1;

    private int _v1;

    @IgnoredByValueEquality
    private int _ignored;

    public B(int v1, int v2, int ignored1, int v11, int ignored2) {
      super(v1, v2, ignored1);
      _v1 = v11;
      _ignored = ignored2;
    }
  }

  @ValueEquality
  private static class C extends B {
    private static final long serialVersionUID = 1;

    public C(int v1, int v2, int ignored1, int v11, int ignored2) {
      super(v1, v2, ignored1, v11, ignored2);
    }
  }

  @Test
  public void test() {
    Placeholder<Integer> p1 = new Placeholder<>();
    Placeholder<Integer> p2 = new Placeholder<>();
    Placeholder<Integer> p3 = new Placeholder<>();

    // test with same inputs throughout (all MissingInput)
    Assertions.assertEquals(new A(1, 2, 3).withInputs(p1, p2), new A(1, 2, 4).withInputs(p1, p2));
    Assertions.assertNotEquals(new A(1, 2, 3).withInputs(p1, p2), new A(1, 2, 4).withInputs(p2, p1));

    Assertions.assertEquals(new B(1, 2, 3, 4, 5).withInputs(p1, p2), new B(1, 2, -3, 4, -5).withInputs(p2, p1));
  }
}
