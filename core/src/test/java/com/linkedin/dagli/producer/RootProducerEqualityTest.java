package com.linkedin.dagli.producer;

import com.linkedin.dagli.annotation.equality.IgnoredByValueEquality;
import com.linkedin.dagli.annotation.equality.ValueEquality;
import com.linkedin.dagli.generator.AbstractGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class RootProducerEqualityTest {
  @ValueEquality
  private static class A extends AbstractGenerator<Integer, A> {
    private static final long serialVersionUID = 1;

    private int _v1;
    private int _v2;

    private Object _alwaysNull = null;

    @IgnoredByValueEquality
    protected int _ignored;

    public A(int v1, int v2, int ignored) {
      _v1 = v1;
      _v2 = v2;
      _ignored = ignored;
    }

    @Override
    public Integer generate(long index) {
      return null;
    }
  }

  @ValueEquality
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

  private static class D extends C {
    private static final long serialVersionUID = 1;

    public D(int v1, int v2, int ignored1, int v11, int ignored2) {
      super(v1, v2, ignored1, v11, ignored2);
    }

    @Override
    protected boolean computeEqualsUnsafe(A other) {
      return ((A) this)._ignored == other._ignored;
    }

    @Override
    protected int computeHashCode() {
      return ((A) this)._ignored;
    }
  }

  @Test
  public void test() {
    Assertions.assertNotEquals(new A(0, 0, 0), new B(0, 0, 0, 0, 0));
    Assertions.assertEquals(new A(1, 2, 3), new A(1, 2, 4));
    Assertions.assertNotEquals(new A(1, 2, 3), new A(1, 3, 3));

    Assertions.assertEquals(new B(1, 2, 3, 4, 5), new B(1, 2, -3, 4, -5));
    Assertions.assertNotEquals(new B(1, 2, 3, 4, 5), new B(1, 3, 3, -4, 5));
    Assertions.assertNotEquals(new B(1, 2, 3, 4, 5), new B(-1, 3, 3, 4, 5));

    // C doesn't use value equality, so none of these should be equals
    Assertions.assertNotEquals(new C(1, 2, 3, 4, 5), new C(1, 3, -3, 4, -5));
    Assertions.assertNotEquals(new C(1, 2, 3, 4, 5), new C(1, 3, 3, -4, 5));
    Assertions.assertNotEquals(new C(1, 2, 3, 4, 5), new C(-1, 3, 3, 4, 5));

    // C should equal itself, however
    C c = new C(1, 2, 3, 4, 5);
    Assertions.assertEquals(c, c);

    Assertions.assertEquals(new D(1, 2, 3, 4, 5), new D(-1, -2, 3, -4, -5));
    Assertions.assertNotEquals(new D(1, 2, 3, 4, 5), new D(-1, -2, -3, -4, -5));
  }
}
