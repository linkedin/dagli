package com.linkedin.dagli.math.distribution;

import java.util.Arrays;
import java.util.Objects;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class DiscreteDistributionTest {
  @Test
  public void testFiltering() {
    // basic tests of the filtering methods
    DiscreteDistribution<Integer> dist = Arrays.asList(
        new LabelProbability<>(0, 0.2),
        new LabelProbability<Integer>(null, 0.5)
    ).stream().collect(ArrayDiscreteDistribution.collector());

    assertEquals(dist.filter(lp -> Objects.equals(lp.getLabel(), 0), Renormalization.NONE).get(0), 0.2, 0.0001);
    assertEquals(dist.filter(lp -> Objects.equals(lp.getLabel(), 0), Renormalization.NONE).get(null), 0, 0.0001);
    assertEquals(dist.filter(lp -> Objects.equals(lp.getLabel(), 0), Renormalization.CONSTANT_SUM).get(0), 0.7, 0.0001);

    assertEquals(dist.filter(lp -> Objects.equals(lp.getLabel(), 99), Renormalization.NONE).get(null), 0, 0.0001);
    assertEquals(dist.filter(lp -> Objects.equals(lp.getLabel(), 99), Renormalization.CONSTANT_SUM).get(null), 0, 0.0001);

    assertEquals(dist.filterLabels(label -> Objects.equals(label, 0), Renormalization.CONSTANT_SUM).get(0), 0.7, 0.0001);
    assertEquals(dist.filterLabels(label -> Objects.equals(label, 99), Renormalization.CONSTANT_SUM).get(null), 0, 0.0001);
  }

  @Test
  public void testMapping() {
    // basic tests of the mapLabels & map methods
    DiscreteDistribution<Integer> dist = Arrays.asList(
        new LabelProbability<>(0, 0.2),
        new LabelProbability<Integer>(null, 0.5)
    ).stream().collect(ArrayDiscreteDistribution.collector());

    assertEquals(dist.mapLabels(l -> 2, Deduplication.MERGE, Renormalization.CONSTANT_SUM).get(2), 0.7, 0.0001);
    assertEquals(dist.mapLabels(l -> 2, Deduplication.MAX, Renormalization.NONE).get(2), 0.5, 0.0001);
    assertEquals(dist.mapLabels(l -> null, Deduplication.MERGE, Renormalization.CONSTANT_SUM).get(null), 0.7, 0.0001);
    assertEquals(dist.mapLabels(l -> null, Deduplication.MAX, Renormalization.NONE).get(null), 0.5, 0.0001);
    assertEquals(dist.mapLabels(l -> l == null ? 2 : null, Deduplication.NONE, Renormalization.NONE).size64(), 2);

    assertEquals(dist.map(lp -> new LabelProbability<>(3, lp.getProbability() * 2), Deduplication.MERGE,
        Renormalization.CONSTANT_SUM).get(3), 0.7, 0.0001);
    assertEquals(dist.map(lp -> new LabelProbability<>(null, lp.getProbability() * 2), Deduplication.MERGE,
        Renormalization.CONSTANT_SUM).get(null), 0.7, 0.0001);
    assertEquals(
        dist.map(lp -> new LabelProbability<>(2, lp.getProbability() * 2), Deduplication.MAX, Renormalization.NONE)
            .get(2), 1.0, 0.0001);
    assertEquals(dist.map(lp -> null, Deduplication.NONE, Renormalization.NONE).size64(), 0);

    // map all probabilities to their square roots and check the sum of probabilities in resulting distribution
    assertEquals(dist.map(lp -> lp.mapProbability(Math::sqrt), Deduplication.NONE, Renormalization.NONE).probabilitySum(),
        1.154320376686505, 0.00001);
    // now do the same thing, but use CONSTANT_SUM and make sure that the sum of probabilities remains at ~0.7
    assertEquals(dist.map(lp -> lp.mapProbability(Math::sqrt), Deduplication.NONE, Renormalization.CONSTANT_SUM).probabilitySum(),
        0.7, 0.00001);
  }

  @Test
  public void testObject2DoubleFixedArrayMap() {
    // very basic tests of core map functionality; we add these tests as insurance in case, e.g. the backing
    // Object2DoubleArrayMap changed in a breaking way (the most likely would be for its constructor to make copies
    // of passed key/value arrays rather than simply copying the references as it does now)
    String[] keys = { "A", "B"};
    double[] values = {1.0, 2.0};
    Object2DoubleFixedArrayMap<String> map = new Object2DoubleFixedArrayMap<>(keys, values);

    // modify the backing array and verify that this is reflected in the map
    values[0] = 3.0;
    assertEquals(map.getDouble("A"), 3.0);
    assertEquals(map.getDouble("B"), 2.0);

    // check that size-increasing modifications are throwing exceptions
    assertThrows(UnsupportedOperationException.class, () -> map.put("C", 4.0));
  }

  @Test
  public void testEqualsAndHashCode() {
    // create multiple sets of distributions (each set contains equivalent distributions):
    ArrayDiscreteDistribution<String>[][] distributionSets = new ArrayDiscreteDistribution[][]{
        { new ArrayDiscreteDistribution<>(new String[]{"A", "B", null}, new double[]{0.2, 0.2, 0.2}),
          new ArrayDiscreteDistribution<>(new String[]{"B", "A", null}, new double[]{0.2, 0.2, 0.2}),
          new ArrayDiscreteDistribution<>(new String[]{null, "A", "B"}, new double[]{0.2, 0.2, 0.2})},
        { new ArrayDiscreteDistribution<>(new String[]{"A", "B", null}, new double[]{0.2, 0.4, 0.2}),
          new ArrayDiscreteDistribution<>(new String[]{"B", "A", null}, new double[]{0.4, 0.2, 0.2}),
          new ArrayDiscreteDistribution<>(new String[]{null, "A", "B"}, new double[]{0.2, 0.2, 0.4})},
        { new ArrayDiscreteDistribution<>(new String[]{"A", "B"}, new double[]{0.2, 0.2}),
          new ArrayDiscreteDistribution<>(new String[]{"B", "A"}, new double[]{0.2, 0.2})},
        { new ArrayDiscreteDistribution<>(new String[]{"A", "B"}, new double[]{0.4, 0.4}),
          new ArrayDiscreteDistribution<>(new String[]{"B", "A"}, new double[]{0.4, 0.4})},
        { new ArrayDiscreteDistribution<>(new String[]{}, new double[]{}),
          new ArrayDiscreteDistribution<>(new String[]{"A"}, new double[]{0}),
          new ArrayDiscreteDistribution<>(new String[]{"B", "A"}, new double[]{0, 0})}};

    for (ArrayDiscreteDistribution<String>[] set : distributionSets) {
      for (ArrayDiscreteDistribution<String> dist1 : set) {
        for (ArrayDiscreteDistribution<String> dist2 : set) {
          assertEquals(dist1, dist2);
          assertEquals(dist1.hashCode(), dist2.hashCode());
        }
      }
    }

    for (ArrayDiscreteDistribution<String>[] set1 : distributionSets) {
      for (ArrayDiscreteDistribution<String>[] set2 : distributionSets) {
        if (set1 != set2) {
          for (ArrayDiscreteDistribution<String> dist1 : set1) {
            for (ArrayDiscreteDistribution<String> dist2 : set2) {
              assertNotEquals(dist1, dist2);

              // this condition does not *necessarily* hold (there's a small chance it won't) but because the hash
              // function for ArrayDiscreteDistribution is deterministic we know it will with the extant implementation:
              assertNotEquals(dist1.hashCode(), dist2.hashCode());
            }
          }
        }
      }
    }
  }

  @Test
  public void testMultiplication() {
    ArrayDiscreteDistribution<String>[] distributions = new ArrayDiscreteDistribution[]{
        new ArrayDiscreteDistribution<>(new String[]{"A", "B", null}, new double[]{0.2, 0.2, 0.2}),
        new ArrayDiscreteDistribution<>(new String[]{"A", "B", null}, new double[]{0.2, 0.2, 0.2}),
        new ArrayDiscreteDistribution<>(new String[]{"A", "B"}, new double[]{0.2, 0.5})};

    // multiplying by an empty distribution should yield an empty distribution
    assertEquals(distributions[0].multiply(DiscreteDistributions.empty()).size64(), 0);

    // ditto the other way around
    assertEquals(DiscreteDistributions.empty().multiply(distributions).size64(), 0);

    // and empty * empty
    assertEquals(DiscreteDistributions.empty().multiply(DiscreteDistributions.empty()).size64(), 0);

    DiscreteDistribution<String> product = distributions[0].multiply(distributions);
    assertEquals(product.size64(), 2); // only two non-zero entries should be present
    assertEquals(product.get("A"), 0.2 * 0.2 * 0.2 * 0.2);
    assertEquals(product.get("B"), 0.2 * 0.2 * 0.2 * 0.5);
  }

  @Test
  public void testMultiplicationWithMinimumProbability() {
    ArrayDiscreteDistribution<String>[] distributions = new ArrayDiscreteDistribution[]{
        new ArrayDiscreteDistribution<>(new String[]{"A", "B", null}, new double[]{0.4, 0.3, 0.2}),
        new ArrayDiscreteDistribution<>(new String[]{"A", "B", null}, new double[]{0.2, 0.2, 0.5}),
        new ArrayDiscreteDistribution<>(new String[]{"A", "B"}, new double[]{0.2, 0.5})};

    // multiplying by an empty distribution should use the minimumEventProbability across all items
    assertEquals(distributions[0].multiply(0.1, DiscreteDistributions.empty()),
        distributions[0].mapValues(v -> v * 0.1));

    // ditto the other way around
    assertEquals(DiscreteDistributions.empty().multiply(0.1, distributions[0]),
        distributions[0].mapValues(v -> v * 0.1));

    // and empty * empty
    assertEquals(DiscreteDistributions.empty().multiply(0.1, DiscreteDistributions.empty()).size64(), 0);

    DiscreteDistribution<String> product = distributions[0].multiply(0.3, distributions);
    assertEquals(product.size64(), 3); // all entries should be present
    assertEquals(product.get("A"), 0.4 * 0.4 * 0.3 * 0.3);
    assertEquals(product.get("B"), 0.3 * 0.3 * 0.3 * 0.5);
    assertEquals(product.get(null), 0.3 * 0.3 * 0.5 * 0.3);
  }

  @Test
  public void testNormalization() {
    // normalizing an empty distribution
    assertEquals(DiscreteDistributions.empty().normalize().size64(), 0);

    ArrayDiscreteDistribution<String> dist =
        new ArrayDiscreteDistribution<>(new String[]{"A", "B", null}, new double[]{0.1, 0.2, 0.2});

    assertEquals(dist.normalize().probabilitySum(), 1.0);
    assertEquals(dist.normalize(0).size64(), 0);

    DiscreteDistribution<String> normalized = dist.normalize(2.0);
    assertEquals(normalized.get("A"), 0.4);
    assertEquals(normalized.get(null), 0.8);
  }
}
