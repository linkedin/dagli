package com.linkedin.dagli.vector;

import com.linkedin.dagli.math.vector.DenseFloatArrayVector;
import com.linkedin.dagli.math.vector.SparseFloatMapVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.math.vector.VectorElementIterator;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.tester.Tester;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class SparseCombinedVectorTest {
  @Test
  public void basicTest() {
    CompositeSparseVector combined = new CompositeSparseVector().withInputs(MissingInput.get(), MissingInput.get());

    Tester.of(combined).test(); // basic tests

    Vector denseVec = DenseFloatArrayVector.wrap(1, 2, 3, 4, 5);
    assertEquivalent(combined.apply(Arrays.asList(denseVec)), denseVec);

    SparseFloatMapVector shv = new SparseFloatMapVector();
    shv.put(10, 10);
    shv.put(100, 100);
    shv.put(1000, 1000);
    assertEquivalent(combined.apply(Arrays.asList(shv)), shv);

    SparseFloatMapVector shv2 = new SparseFloatMapVector();
    shv2.put(1, 1);
    shv2.put(2, 2);
    shv2.put(3, 3);
    shv2.put(4, 4);
    shv2.put(5, 5);
    shv2.put(10, 10);
    shv2.put(100, 100);
    shv2.put(1000, 1000);
    assertEquivalent(combined.apply(Arrays.asList(shv, denseVec)), shv2);
  }

  public void assertEquivalent(Vector v1, Vector v2) {
    assertEquals(v1.size64(), v2.size64());
    HashSet<Double> values1 = new HashSet<>();
    HashSet<Double> values2 = new HashSet<>();
    HashSet<Long> indices1 = new HashSet<>();
    HashSet<Long> indices2 = new HashSet<>();

    VectorElementIterator iter1 = v1.iterator();
    VectorElementIterator iter2 = v2.iterator();

    while (iter1.hasNext()) {
      iter1.next((index, value) -> {
        indices1.add(index);
        values1.add(value);
      });
    }

    while (iter2.hasNext()) {
      iter2.next((index, value) -> {
        indices2.add(index);
        values2.add(value);
      });
    }

    assertEquals(values1, values2);
    assertEquals(indices1.size(), indices2.size());
  }
}
