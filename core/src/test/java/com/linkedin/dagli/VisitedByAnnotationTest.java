package com.linkedin.dagli;

import com.linkedin.dagli.annotation.visitor.VisitedBy;


/**
 * Tests the {@link VisitedBy} annotation (or, more precisely, its annotation processor).
 */
public class VisitedByAnnotationTest {
  @VisitedBy("TestVisitor1")
  static class A<R> { }
  @VisitedBy("TestVisitor1")
  static class B { }
  @VisitedBy("TestVisitor1")
  static class C<T extends String, U extends T> { }

  @VisitedBy("TestVisitor2")
  static class D { }

  // test generated interfaces by implementing them and overriding expected methods
  static class VsitorImpl1 implements TestVisitor1<Integer> {
    @Override
    public <R> Integer visit(A<R> visitee) {
      return null;
    }

    @Override
    public Integer visit(B visitee) {
      return null;
    }

    @Override
    public <T extends String, U extends T> Integer visit(C<T, U> visitee) {
      return null;
    }
  };

  static class VisitorImpl2 implements TestVisitor2<Void> {
    @Override
    public Void visit(D visitee) {
      return null;
    }
  }
}
