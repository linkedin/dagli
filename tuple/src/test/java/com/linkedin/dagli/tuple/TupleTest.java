package com.linkedin.dagli.tuple;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class TupleTest {
  @Test
  public void testBasic() {
    testPair(Tuple2.fromArrayUnsafe(new Object[] {true, 1}), true, 1);
    testPair(Tuple2.of(3.14, "yo"), 3.14, "yo");
  }

  private <A extends Comparable<A>, B extends Comparable<B>> void testPair(Tuple2<A, B> pair, A valA, B valB) {
    Assertions.assertEquals(valA, pair.get(0));
    Assertions.assertEquals(valB, pair.get(1));
    Assertions.assertEquals(valA, pair.get0());
    Assertions.assertEquals(valB, pair.get1());
    Assertions.assertArrayEquals(new Object[] { valA, valB }, pair.toArray());
    Assertions.assertEquals(2, pair.size());

    if (valB != null) {
      Assertions.assertTrue(pair.compareTo(Tuple.of(valA, null)) > 0);
    }
    Assertions.assertNotEquals(pair, Tuple2.of("random nonsense", new Object()));
    Assertions.assertNotEquals(pair, Tuple3.of(pair.get0(), pair.get1(), null));

    Tuple2<A, B> iterablePair = Tuple2.fromIterableUnsafe(pair);
    Tuple2<A, B> arrayPair = Tuple2.fromArrayUnsafe(pair.toArray());
    testEquality(iterablePair, arrayPair);
    testEquality(pair, arrayPair);
    testEquality(iterablePair, pair);
  }

  private <A, B> void testEquality(Tuple2<A, B> pair1, Tuple2<A, B> pair2) {
    Assertions.assertEquals(pair1, pair2);
    Assertions.assertEquals(pair1.hashCode(), pair2.hashCode());
    Assertions.assertEquals(pair1.toString(), pair2.toString());
    Assertions.assertEquals(pair1.compareTo(pair2), 0);
  }
}
