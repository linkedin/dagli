package com.linkedin.dagli.util.collection;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IterablesTest {
  @Test
  public void testSize64() {
    Assertions.assertEquals(3, Iterables.size64(Arrays.asList(1, 2, 3)));
    Assertions.assertEquals(3, Iterables.size64(() -> Arrays.<Object>asList(1, 2, 3).iterator()));
    Assertions.assertEquals(0, Iterables.size64(Collections::emptyIterator));
  }

  @Test
  public void testPrepend() {
    Assertions.assertEquals(Arrays.asList(1, 2, 3), Iterables.prepend(Arrays.asList(2, 3), 1));
    Assertions.assertEquals(Collections.singletonList(1), Iterables.prepend(Collections::emptyIterator, 1));
  }

  @Test
  @SuppressWarnings("unchecked") // pretending the type of elements "in" the empty iterator are Comparable is safe
  public void testArgMax() {
    Assertions.assertEquals(-1, Iterables.argMax(Collections::<Comparable>emptyIterator));
    Assertions.assertEquals(1, Iterables.argMax(Arrays.asList(1, 3, 2)));
  }
}
