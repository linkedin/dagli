package com.linkedin.dagli.vector;

import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.tester.Tester;
import org.junit.jupiter.api.Test;


public class LazyFilteredVectorTest {
  @Test
  public void test() {
    Tester.of(new LazyClippedVector().withMinimumValue(0).withMaximumValue(0))
        .input(DenseFloatArrayVector.wrap(-1, 0, 1, 2))
        .output(Vector.empty())
        .test();

    Tester.of(new LazyClippedVector().withMinimumValue(-1).withMaximumValue(1))
        .input(DenseFloatArrayVector.wrap(-2, -1, 0, 1, 2))
        .output(DenseFloatArrayVector.wrap(-1, -1, 0, 1, 1))
        .test();
  }
}
