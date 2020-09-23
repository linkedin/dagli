package com.linkedin.dagli.producer;

import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.generator.Constant;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer1;
import com.linkedin.dagli.transformer.AbstractPreparedTransformer2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class ProducersTest {

  @ValueEquality
  private static class TestConstantValueTransformerA
      extends AbstractPreparedTransformer1<Integer, Integer, TestConstantValueTransformerA> {
    private static final long serialVersionUID = 1;

    @Override
    public Integer apply(Integer value0) {
      return null;
    }

    @Override
    protected boolean hasAlwaysConstantResult() {
      return true;
    }
  }

  // note: it is an error to extend TestConstantValueTransformerA like this (because the generic parameter identifying
  // what concrete class we are will be wrong) but this doesn't matter for our present testing purposes
  @ValueEquality
  private static class TestConstantValueTransformerB extends TestConstantValueTransformerA {
    private static final long serialVersionUID = 1;
  }

  private interface IF5 { }
  private interface IF6 extends IF5 { }
  @ValueEquality
  private static class TestNonConstantValueTransformerA
      extends AbstractPreparedTransformer2<Integer, Integer, Integer, TestNonConstantValueTransformerA> implements IF6 {
    private static final long serialVersionUID = 1;

    @Override
    public Integer apply(Integer value0, Integer value1) {
      return null;
    }

    @Override
    public TestNonConstantValueTransformerA withInput1(Producer<? extends Integer> input0) {
      return super.withInput1(input0);
    }

    @Override
    public TestNonConstantValueTransformerA withInput2(Producer<? extends Integer> input1) {
      return super.withInput2(input1);
    }
  }

  @Test
  public void testExplicitConstantValue() {
    Assertions.assertTrue(new TestConstantValueTransformerA().internalAPI().hasAlwaysConstantResult());
    Assertions.assertTrue(new TestConstantValueTransformerB().internalAPI().hasAlwaysConstantResult());

    Assertions.assertFalse(new TestNonConstantValueTransformerA().internalAPI().hasAlwaysConstantResult());
  }

  @Test
  public void testConstantValueInference() {
    TestNonConstantValueTransformerA transCV1 =
        new TestNonConstantValueTransformerA().withInput1(new Constant<>(0)).withInput2(new Constant<>(0));
    Assertions.assertTrue(ProducerUtil.hasConstantResult(transCV1));

    TestNonConstantValueTransformerA transCV2 =
        new TestNonConstantValueTransformerA().withInput1(transCV1).withInput2(new Constant<>(0));
    Assertions.assertTrue(ProducerUtil.hasConstantResult(transCV2));

    TestNonConstantValueTransformerA transCV3 =
        new TestNonConstantValueTransformerA().withInput1(transCV1).withInput2(transCV2);
    Assertions.assertTrue(ProducerUtil.hasConstantResult(transCV3));

    Assertions.assertFalse(ProducerUtil.hasConstantResult(new TestNonConstantValueTransformerA()));
    Assertions.assertFalse(ProducerUtil.hasConstantResult(
        new TestNonConstantValueTransformerA().withInput1(new TestNonConstantValueTransformerA())
            .withInput2(transCV3)));
  }
}
