package com.linkedin.dagli.objectio.tuple;

import com.linkedin.dagli.objectio.ConstantReader;
import com.linkedin.dagli.objectio.testing.Tester;
import com.linkedin.dagli.tuple.Tuple;
import com.linkedin.dagli.tuple.Tuple2;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class TupleReaderTest {
  @Test
  public void test() {
    TupleReader<Tuple2<Long, String>> iterable =
        new TupleReader<>(new ConstantReader<>(4L, 2), new ConstantReader<>("Hello", 2));
    java.util.List<Tuple2<Long, String>> truth = Arrays.asList(Tuple2.of(4L, "Hello"), Tuple2.of(4L, "Hello"));

    Tester.testReader(iterable, truth);
  }

  @Test
  public void multiArityTest() {
    for (int tupleSize = 1; tupleSize <= 10; tupleSize++) {
      ConstantReader<Integer>[] elementReaders = new ConstantReader[tupleSize];
      for (int i = 0; i < elementReaders.length; i++) {
        elementReaders[i] = new ConstantReader<>(i, 10);
      }

      // create the reader (we don't specify a tuple type as generic parameter because it varies with tupleSize)
      TupleReader tupleReader = new TupleReader(elementReaders);

      // test tuple reader length
      Assertions.assertEquals(10, tupleReader.size64());

      // test all tuple element values
      tupleReader.forEach(tuple -> {
        for (int i = 0; i < elementReaders.length; i++) {
          Assertions.assertEquals(i, ((Tuple) tuple).get(i));
        }
      });
    }
  }
}
