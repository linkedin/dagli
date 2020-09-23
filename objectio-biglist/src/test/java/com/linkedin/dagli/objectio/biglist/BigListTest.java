package com.linkedin.dagli.objectio.biglist;

import com.linkedin.dagli.objectio.testing.Tester;
import it.unimi.dsi.fastutil.BigList;
import it.unimi.dsi.fastutil.objects.ObjectBigArrayBigList;
import it.unimi.dsi.fastutil.objects.ObjectBigLists;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class BigListTest {
  @Test
  public void testBigArrayList() {
    Tester.testWriter(new BigListWriter<>(new ObjectBigArrayBigList<>()));
  }

  @Test
  public void testToBigList() {
    Tester.testReader(new BigListReader<>(new ObjectBigArrayBigList<>(Arrays.asList(1, 2, 3, 4).iterator())),
        Arrays.asList(1, 2, 3, 4));
    BigList<String> biglist = new BigListReader<>(ObjectBigLists.singleton("yolo")).toBigList();
    Assertions.assertEquals(biglist.size64(), 1);
    Assertions.assertEquals(biglist.get(0), "yolo");
  }
}
