package com.linkedin.dagli.math.mdarray;

import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Tests the {@link VectorAsMDArray} class
 */
public class VectorAsMDArrayTest {
  @Test
  public void basicTests() {
    DenseFloatArrayVector wrapped = new DenseFloatArrayVector(1, 0, 2, 0, 3, 0);

    VectorAsMDArray mdArray1 = new VectorAsMDArray(wrapped);
    Assertions.assertEquals(0, mdArray1.shape().length);
    Assertions.assertEquals(1, mdArray1.getAsDouble(0));
    Assertions.assertEquals(wrapped.valueType(), mdArray1.valueType());

    VectorAsMDArray mdArray2 = new VectorAsMDArray(wrapped, 1, 3);
    Assertions.assertEquals(2, mdArray2.shape().length);
    Assertions.assertEquals(2, mdArray2.getAsDouble(2));
    Assertions.assertEquals(2, mdArray2.getAsDouble(0, 2));

    VectorAsMDArray mdArray3 = new VectorAsMDArray(wrapped, 3, 3);
    Assertions.assertEquals(2, mdArray3.shape().length);
    Assertions.assertEquals(3, mdArray3.getAsDouble(4));
    Assertions.assertEquals(3, mdArray3.getAsDouble(1, 1));
    Assertions.assertEquals(0, mdArray3.getAsDouble(2, 2));
    Assertions.assertEquals(wrapped, mdArray3.asVector());
  }
}
