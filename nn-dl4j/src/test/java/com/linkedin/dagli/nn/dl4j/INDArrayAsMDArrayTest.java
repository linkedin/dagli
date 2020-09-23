package com.linkedin.dagli.nn.dl4j;

import com.linkedin.dagli.dl4j.INDArrayAsMDArray;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;


/**
 * Tests the {@link INDArrayAsMDArray} class.
 */
public class INDArrayAsMDArrayTest {
  @Test
  public void basicTests() {
    INDArray indArray = Nd4j.zeros(3, 2, 1);
    INDArrayAsMDArray mdArray = new INDArrayAsMDArray(indArray);
    for (int row = 0; row < 3; row++) {
      for (int column = 0; column < 2; column++) {
        long[] indices = new long[] {row, column, 0};
        int value = row * 2 + column;

        indArray.putScalar(indices, value);
        Assertions.assertEquals(value, mdArray.getAsDouble(indices));
        Assertions.assertEquals(value, mdArray.getAsDouble(value));
      }
    }

  }

  @Test
  public void testScalar() {
    INDArrayAsMDArray scalar = new INDArrayAsMDArray(Nd4j.scalar(3.14));
    Assertions.assertEquals(0, scalar.shape().length);
    Assertions.assertEquals(3.14, scalar.getAsDouble());
    Assertions.assertEquals(3.14, scalar.getAsDouble(0));
    Assertions.assertEquals(3.14, scalar.getAsDoubleUnsafe(0));
  }

}
