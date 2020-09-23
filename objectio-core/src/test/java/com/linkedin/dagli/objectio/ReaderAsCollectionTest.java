package com.linkedin.dagli.objectio;

import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class ReaderAsCollectionTest {
  private static final Collection<Integer> COLLECTION = new ObjectReaderCentury().toCollection();

  @Test
  public void basicTests() {
    Assertions.assertFalse(COLLECTION.isEmpty());
    Assertions.assertTrue(COLLECTION.contains(4));
    Assertions.assertEquals(100, COLLECTION.toArray(new Integer[0]).length);
    Assertions.assertTrue(COLLECTION.stream().anyMatch(i -> i == 4));
    Assertions.assertThrows(IndexOutOfBoundsException.class, () -> new ObjectReaderCentury() {
      @Override
      public long size64() {
        return Long.MAX_VALUE;
      }
    }.toCollection());

    // create a collection wrapping a reader that claims a size of 0 on construction and a Long.MAX_VALUE size
    // on subsequent size64() calls
    Assertions.assertThrows(IndexOutOfBoundsException.class, () -> new ObjectReaderCentury() {
      private boolean _firstCall = true;

      @Override
      public long size64() {
        if (_firstCall) {
          _firstCall = false;
          return 0;
        }
        return Long.MAX_VALUE;
      }
    }.toCollection().size());

    // check that this doesn't throw
    ((ReaderAsCollection) new ObjectReaderCentury().toCollection()).close();
  }

  @Test
  public void containsAllTest() {
    Assertions.assertEquals(100, COLLECTION.size());
    Assertions.assertTrue(COLLECTION.containsAll(COLLECTION));
    Assertions.assertFalse(COLLECTION.containsAll(Arrays.asList(5, 1337)));
  }

  @Test
  public void testUnsupported() {
    Assertions.assertThrows(UnsupportedOperationException.class, () -> COLLECTION.add(6));
    Assertions.assertThrows(UnsupportedOperationException.class, () -> COLLECTION.addAll(COLLECTION));
    Assertions.assertThrows(UnsupportedOperationException.class, () -> COLLECTION.removeAll(COLLECTION));
    Assertions.assertThrows(UnsupportedOperationException.class, () -> COLLECTION.retainAll(COLLECTION));
    Assertions.assertThrows(UnsupportedOperationException.class, () -> COLLECTION.remove(6));
    Assertions.assertThrows(UnsupportedOperationException.class, () -> COLLECTION.clear());
  }

}
