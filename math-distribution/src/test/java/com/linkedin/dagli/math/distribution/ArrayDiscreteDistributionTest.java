package com.linkedin.dagli.math.distribution;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class ArrayDiscreteDistributionTest {
  @Test
  public void basicTest() {
    ArrayDiscreteDistribution<Integer> dist = new ArrayDiscreteDistribution<>(Arrays.asList(
        new LabelProbability<>(3, 0.3), new LabelProbability<>(1, 0.1),
        new LabelProbability<>(5, 0.5)
    ));

    List<LabelProbability<Integer>> entries = dist.stream().collect(Collectors.toList());
    assertEquals(entries.get(0).getLabel().intValue(), 5);
    assertEquals(entries.get(1).getLabel().intValue(), 3);
    assertEquals(entries.get(2).getLabel().intValue(), 1);

    ArrayDiscreteDistribution<Integer> transformed = dist.stream()
        .map(lp -> lp.mapLabel(l -> l == 3 ? 6 : (l == 1 ? 2 : 10)))
        .collect(ArrayDiscreteDistribution.collector());

    assertEquals(transformed.getLabelByIndex(0).intValue(), 10);
    assertEquals(transformed.getLabelByIndex(1).intValue(), 6);
    assertEquals(transformed.getLabelByIndex(2).intValue(), 2);

    assertEquals(transformed.getProbabilityByIndex(0), 0.5, 0.001);
    assertEquals(transformed.getProbabilityByIndex(1), 0.3, 0.001);
    assertEquals(transformed.getProbabilityByIndex(2), 0.1, 0.001);

    assertEquals(transformed.size64(), 3);
  }
}
