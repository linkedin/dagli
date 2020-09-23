package com.linkedin.dagli.math.mdarray;

import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link MDSubarray}.
 */
public class MDSubarrayTest {
  @Test
  public void basicTests() {
    DenseFloatArrayVector wrapped = new DenseFloatArrayVector(1, 0, 2, 0, 3, 0);
    VectorAsMDArray mdArray = new VectorAsMDArray(wrapped, 3, 3);

    MDArray scalarSubarray = mdArray.subarrayAt(1, 1);
    Assertions.assertEquals(0, scalarSubarray.shape().length);
    Assertions.assertEquals(3, scalarSubarray.getAsDouble());
    Assertions.assertEquals(3, scalarSubarray.getAsDouble(0));

    MDArray vectorSubarray = mdArray.subarrayAt(1);
    Assertions.assertArrayEquals(new long[] { 3 }, vectorSubarray.shape());
    Assertions.assertEquals(3, vectorSubarray.getAsDouble(1));
    Assertions.assertEquals(3, vectorSubarray.getAsDouble(new long[] { 1 }));
  }
}
