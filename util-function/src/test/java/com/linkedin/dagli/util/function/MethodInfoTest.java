package com.linkedin.dagli.util.function;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class MethodInfoTest {
  interface Z {
    void foo();

    default int bar(boolean b) {
      return 42;
    }

    static int get() {
      return 1;
    }
  }

  class A implements Z {
    public void foo() { }

    public int bar(boolean b) {
      return 256;
    }
  }

  class B implements Z {
    public void foo() { }
  }

  class C implements Function0.Serializable<String> {
    public String apply() {
      return "blah";
    }
  }

  static class D {
    public String apply() {
      return "blah";
    }
  }

  private static String concat(String a, String b) {
    return a.concat(b);
  }

  @Test
  public void test() {
    assertEquals(((Function2.Serializable<String, String, String>) MethodInfoTest::concat).safelySerializable().apply("Start", "End"), "StartEnd");
    assertEquals(new MethodReference2<>(String::concat).apply("Start", "End"), "StartEnd");
    assertEquals((long) new MethodReference2<Long, Long, Long>(Math::addExact).apply(5L, 6L), 11L);
    assertEquals(new MethodReference0<>(String::new).apply(), "");

    String s = "blah";
    assertEquals(new MethodReference2<String, String, Boolean>(String::startsWith).apply(s, "bl"), true);
    assertEquals(new MethodReference1<String, Boolean>(s::startsWith).apply("bl"), true);

    assertEquals(new BooleanMethodReference0(s::isEmpty).apply(), false);
    assertEquals(new MethodReference2<String, String, Boolean>(String::startsWith).apply(s, "bl"), true);

    A a = new A();
    new VoidMethodReference0(a::foo).apply();
    new VoidMethodReference1<Z>(Z::foo).apply(a);

    assertEquals(new IntMethodReference1<>(a::bar).apply(true), 256);

    assertEquals(new IntMethodReference2<>(Z::bar).apply(a, false), 256);
    assertEquals(new IntMethodReference2<>(Z::bar).apply(new B(), false), 42);

    assertEquals(new IntMethodReference0(Z::get).apply(), 1);

    C c = new C();
    assertEquals(new MethodReference0<>(c::apply).apply(), "blah");

    assertTrue(new MethodReference0<D>(D::new).apply() != null);
  }
}
