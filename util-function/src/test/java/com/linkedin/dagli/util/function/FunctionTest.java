package com.linkedin.dagli.util.function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class FunctionTest {
  @Test
  public void testVariadic() {
    int[] sum = new int[1]; // not used for anything beyond making sure Java doesn't elide our call below
    consumerV((Integer... args) -> sum[0] += args.length);
    Assertions.assertEquals(3, consumerI(args -> args.length));
    Assertions.assertEquals(3, (int) this.<Integer>consumerG(args -> args.length));
  }

  private void consumerV(VoidFunctionVariadic<Integer> vvc) {
    vvc.apply(1, 2, 3);
  }

  private int consumerI(IntFunctionVariadic<Integer> vvc) {
    return vvc.apply(1, 2, 3);
  }

  private <R> R consumerG(FunctionVariadic<Integer, R> vvc) {
    return vvc.apply(1, 2, 3);
  }

  @Test
  public void testUnaryAndThen() {
    UnaryFunction<Double> adder = z -> z + 1;
    UnaryFunction<Double> divider = z -> z / 2;

    Assertions.assertEquals(3, (double) adder.andThen(divider).apply(5.0), 0);
    Assertions.assertEquals(4, (double) divider.andThen(adder).apply(6.0), 0);
  }

  @Test
  public void testDefaultOnNullArgument() {
    IntFunction1<Integer> adder = z -> z + 1;
    Assertions.assertEquals(0, adder.returnZeroOnNullArgument().apply(null));

    int[] capturedArray = new int[1];
    VoidFunction2<Integer, Integer> increment = (a, b) -> capturedArray[0]++;
    increment.skipOnNullArgument().accept(1, 1);
    increment.skipOnNullArgument().accept(null, 1);
    increment.skipOnNullArgument().accept(1, null);
    increment.skipOnNullArgument().accept(null, null);
    increment.skipOnNullArgument().accept(1, 1);
    Assertions.assertEquals(2, capturedArray[0]);
  }
}
