package com.linkedin.dagli.data.dsv;

import com.linkedin.dagli.annotation.struct.Struct;
import com.linkedin.dagli.data.schema.RowSchema;
import com.linkedin.dagli.data.schema.RowSchemas;
import com.linkedin.dagli.objectio.ObjectIterator;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.apache.commons.csv.CSVFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.params.provider.Arguments.*;


public class DSVReaderTest {
  @Struct("TestStruct")
  static abstract class TestStructDef {
    boolean _boolVal;
    String _stringVal1;
    String _stringVal2;
    Double _doubleVal;
  }

  private static <T> DSVReader<T> reader() {
    return new DSVReader<T>()
        .withFormat(CSVFormat.DEFAULT.withEscape('\\').withHeader("A", "B", "C", "D"))
        .withResourceFile(DSVReaderTest.class.getClassLoader(), "test.csv", StandardCharsets.UTF_8);
  }

  @Test
  public void readAllStringsTest() {
    DSVReader<String[]> dsvData = reader().withSchema(RowSchemas.forStrings());
    try (ObjectIterator<String[]> iter = dsvData.iterator()) {
      String[] first = iter.next();
      Assertions.assertEquals("true", first[0]);
      Assertions.assertEquals("hello there, friend\\fiend", first[1]);
      Assertions.assertEquals("Hi!", first[2]);
      Assertions.assertEquals("6", first[3]);
    }
  }

  @Test
  public void csvTest() {
    DSVReader<TestStruct> dsvData = reader().withSchema(TestStruct.Schema.builder()
        .setBoolValColumnIndex(0)
        .setStringVal1ColumnIndex(1)
        .setStringVal2ColumnIndex(2)
        .setDoubleValColumnIndex(3)
        .build());

    Assertions.assertEquals(dsvData.size64(), 3);

    try (ObjectIterator<TestStruct> iter = dsvData.iterator()) {

      Assertions.assertEquals(iter.next(), TestStruct.Builder
          .setBoolVal(true)
          .setStringVal1("hello there, friend\\fiend")
          .setStringVal2("Hi!")
          .setDoubleVal(6d)
          .build());

      Assertions.assertEquals(iter.next(), TestStruct.Builder
          .setBoolVal(true)
          .setStringVal1("Hello")
          .setStringVal2("Yo")
          .setDoubleVal(4d)
          .build());

      Assertions.assertEquals(iter.next(), TestStruct.Builder
          .setBoolVal(false)
          .setStringVal1("And he said, \"What's up, Dude\"!")
          .setStringVal2("radical")
          .setDoubleVal(-34.3)
          .build());

      Assertions.assertFalse(iter.hasNext());
    }
  }

  static Stream<Arguments> testRowSchemas() {
    return Stream.of(
        arguments(RowSchemas.forString(2, true), "Hi!"),
        arguments(RowSchemas.forBoolean(0, true), true),
        arguments(RowSchemas.forDouble(3, true), 6d),
        arguments(RowSchemas.forFloat(3, true), 6f),
        arguments(RowSchemas.forInteger(3, true), 6),
        arguments(RowSchemas.forLong(3, true), 6L),
        arguments(RowSchemas.forString("C", true), "Hi!"),
        arguments(RowSchemas.forBoolean("A", true), true),
        arguments(RowSchemas.forDouble("D", true), 6d),
        arguments(RowSchemas.forFloat("D", true), 6f),
        arguments(RowSchemas.forInteger("D", true), 6),
        arguments(RowSchemas.forLong("D", true), 6L)
    );
  }

  @ParameterizedTest
  @MethodSource
  public void testRowSchemas(RowSchema schema, Object expected) {
    try (DSVReader data = reader().withSchema(schema); ObjectIterator iter = data.iterator()) {
      Assertions.assertEquals(expected, iter.next());
    }
  }

  @Test
  public void testRowStringsSchema() {
    try (DSVReader<String[]> data = reader().withSchema(RowSchemas.forStrings());
        ObjectIterator<String[]> iter = data.iterator()) {
      Assertions.assertEquals("Hi!", iter.next()[2]);
    }
  }
}
