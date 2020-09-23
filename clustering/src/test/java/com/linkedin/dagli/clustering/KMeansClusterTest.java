package com.linkedin.dagli.clustering;

import com.linkedin.dagli.tester.Tester;
import org.junit.jupiter.api.Test;


public class KMeansClusterTest {
  @Test
  public void testBasic() {
    int[] alpha = new int[1];
    Tester.of(new KMeansCluster(2))
        .input(new double[]{0, 1})
        .input(new double[]{0, 1.1})
        .input(new double[]{0, 0.9})
        .input(new double[]{1, 0})
        .input(new double[]{1.1, 0})
        .input(new double[]{0.9, 0})
        .input(new double[]{0, 1})
        .outputTest(res -> {
          alpha[0] = res.getIndex(); // can't be sure what cluster will be assigned, so capture it here
          return true;
        }).outputTest(res -> res.getIndex() == alpha[0])
        .outputTest(res -> res.getIndex() == alpha[0])
        .outputTest(res -> res.getIndex() == 1 - alpha[0])
        .outputTest(res -> res.getIndex() == 1 - alpha[0])
        .test();
  }
}
