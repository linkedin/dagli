package com.linkedin.dagli.transformer;

import com.linkedin.dagli.distribution.SampledWithReplacement;
import com.linkedin.dagli.math.distribution.ArrayDiscreteDistribution;
import com.linkedin.dagli.math.hashing.DoubleXorShift;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class SampledWithReplacementTest {
  @Test
  public void basicTest() {
    ArrayDiscreteDistribution<Boolean> distribution =
        new ArrayDiscreteDistribution<>(new Boolean[]{true, false}, new double[]{2, 2});
    DoubleXorShift rng = new DoubleXorShift(0xc0de);

    SampledWithReplacement<Boolean> sampler = new SampledWithReplacement<Boolean>().withDistribution(distribution);

    final int samples = 1000000;
    int trueCount = 0;

    for (int i = 0; i < samples; i++) {
      if (sampler.apply(distribution, rng.hashToDouble(i))) {
        trueCount++;
      }
    }

    final double allowableError = 0.001;
    Assertions.assertTrue(((double) trueCount) / samples > (0.5 - allowableError));
    Assertions.assertTrue(((double) trueCount) / samples < (0.5 + allowableError));
  }
}
