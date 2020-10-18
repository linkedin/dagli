package com.linkedin.dagli.vector;

import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.placeholder.Placeholder;
import com.linkedin.dagli.tester.Tester;
import com.linkedin.dagli.util.array.ArraysEx;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;


public class NumericArgumentsVectorTest {
  @Test
  @SuppressWarnings("unchecked")
  public void integerTest() {
    ArrayList<Number> values = new ArrayList<>();
    values.add(Integer.MAX_VALUE);
    values.add(Integer.MIN_VALUE);
    values.add(3);
    values.add(4.5f);
    values.add(4.5d);
    values.add(-4);

    Tester.of(new DenseVectorFromNumbers().withInputs(
        values.stream().map(v -> new Placeholder<>()).toArray(Placeholder[]::new)))
        .inputArray(values)
        .output(DenseFloatArrayVector.wrap(ArraysEx.toFloatsLossy(values.stream().mapToDouble(Number::doubleValue).toArray())))
        .test();
  }
}
