package com.linkedin.dagli.objectio.avro;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class AvroWriterTest {
  @Test
  public void testEmptyGenericWriter() throws IOException {
    Path tempFile = Files.createTempFile("AvroWriterTest", ".avro");
    AvroWriter<GenericRecord> genericWriter = new AvroWriter<>(GenericRecord.class, tempFile);
    genericWriter.close();
    try (AvroReader<GenericRecord> reader = genericWriter.createReader()) {
      Assertions.assertEquals(reader.size64(), 0);
    }
  }

  @Test
  public void testEmptyGenericWriter2() throws IOException {
    Path tempFile = Files.createTempFile("AvroWriterTest", ".avro");
    AvroWriter<GenericRecord> genericWriter = new AvroWriter<>(GenericRecord.class, tempFile, 0);
    genericWriter.close();
    try (AvroReader<GenericRecord> reader = genericWriter.createReader()) {
      Assertions.assertEquals(reader.size64(), 0);
    }
  }

  @Test
  public void testGenericWriter() throws IOException {
    TestAvroSchema record = new TestAvroSchema();
    record.created = 1;
    record.edited = 2L;
    record.recursion = null;
    record.userName = "Sarah";

    Path tempFile = Files.createTempFile("AvroWriterTest", ".avro");
    AvroWriter<GenericRecord> genericWriter = new AvroWriter<>(GenericRecord.class, tempFile);
    genericWriter.write(record);
    genericWriter.write(record);
    genericWriter.write(record);
    genericWriter.close();
    try (AvroReader<GenericRecord> reader = genericWriter.createReader()) {
      Assertions.assertEquals(reader.size64(), 3);
      Assertions.assertEquals("Sarah", reader.stream().findFirst().get().get("userName").toString());
    }

    // try appending to the file
    AvroWriter<TestAvroSchema> writer = new AvroWriter<>(TestAvroSchema.class, tempFile); // reopen file
    record.userName = "Sam"; // change name to write
    writer.write(record); // write a record
    writer.close(); // close the file
    try (AvroReader<TestAvroSchema> reader = writer.createReader()) {
      Assertions.assertEquals(4, reader.size64());
      Assertions.assertEquals("Sam", reader.stream().skip(3).findFirst().get().userName.toString());
    }
  }

  @Test
  public void testSpecificWriter() throws IOException {
    TestAvroSchema record = new TestAvroSchema();
    record.created = 1;
    record.edited = 2L;
    record.recursion = null;
    record.userName = "Sarah";

    Path tempFile = Files.createTempFile("AvroWriterTest", ".avro");
    AvroWriter<TestAvroSchema> writer = new AvroWriter<>(TestAvroSchema.class, tempFile, 3);
    writer.write(record);
    writer.write(record);
    writer.write(record);
    Assertions.assertEquals(3, writer.size64());
    writer.close();
    try (AvroReader<TestAvroSchema> reader = writer.createReader()) {
      Assertions.assertEquals(reader.size64(), 3);
      Assertions.assertEquals("Sarah", reader.stream().findFirst().get().userName.toString());
    }

    // attempting to append to a fixed-record-count file is forbidden:
    Assertions.assertThrows(UncheckedIOException.class, () -> new AvroWriter<>(TestAvroSchema.class, tempFile));

    // as it attempting to set the record count of the file when appending
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> new AvroWriter<>(TestAvroSchema.class, tempFile, 42));
  }
}
