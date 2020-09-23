package com.linkedin.dagli.transformer;

import com.linkedin.dagli.tester.Tester;
import com.linkedin.dagli.vector.CategoricalFeatureVector;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;


public class CategoricalObjectsAsSparseVectorTest {
  @Test
  public void resultArityTest() {
    Tester.of(new CategoricalFeatureVector())
        .input(Arrays.asList(1, 3, 3, 7))
        .outputTest(v -> v.size64() == 4)
        .input(Collections.singletonList(1))
        .outputTest(v -> v.size64() == 1)
        .input(Collections.singletonList(2))
        .outputTest(v -> v.size64() == 1)
        .input(Collections.emptyList())
        .outputTest(v -> v.size64() == 0)
        .distinctOutputs()
        .test();
  }
}
