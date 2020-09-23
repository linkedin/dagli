package com.linkedin.dagli.avro;

import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class AvroTest {
  @Test
  public void test() throws IOException {
    TestAvroSchema record = new TestAvroSchema();
    record.userName = "Jeff";

    AvroField<CharSequence> field = new AvroField<>().withFieldName("userName").withFieldType(CharSequence.class);
    Assertions.assertEquals("Jeff", field.apply(record).toString());
  }
}
