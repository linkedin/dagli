package com.linkedin.dagli.objectio.avro;

import com.linkedin.dagli.objectio.ObjectIterator;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AvroTest {
  private static final int FILES_AT_SCALE = 5;
  private static final int RECORDS_PER_FILE_AT_SCALE = 1000000;

  private static File getTempFile() throws IOException {
    File file = File.createTempFile("BIAT", ".avro");
    file.deleteOnExit();
    return file;
  }

  private static Path writeAvroFile() throws IOException {
    File tempFile = getTempFile();

    DatumWriter<TestAvroSchema> userDatumWriter = new SpecificDatumWriter<>(TestAvroSchema.class);
    try (DataFileWriter<TestAvroSchema> dataFileWriter = new DataFileWriter<>(userDatumWriter)) {
      dataFileWriter.setMeta("count", 3);
      dataFileWriter.create(TestAvroSchema.SCHEMA$, tempFile);

      TestAvroSchema record = new TestAvroSchema();
      record.userName = "User1";
      record.recursion = new TestAvroSchema();
      record.recursion.userName = "Steve";
      dataFileWriter.append(record);
      record.created = 5;
      dataFileWriter.append(record);
      dataFileWriter.append(record);
    }
    return tempFile.toPath();
  }

  // copies Avro records stored in a specified path to another file using AvroWriter.toAvroFile()
  private static Path copyAvroFile(Path pathToCopy) throws IOException {
    Path targetPath = getTempFile().toPath();

    try (AvroReader<TestAvroSchema> reader = new AvroReader<>(TestAvroSchema.class, pathToCopy)) {
      AvroWriter.toAvroFile(reader, targetPath);
    }

    return targetPath;
  }

  @Test
  public void testGeneric() throws IOException {
    Path path = writeAvroFile();

    try (AvroReader<GenericRecord> reader = new AvroReader<>(GenericRecord.class, path)) {
      Assertions.assertEquals(3, reader.size64());
      try (ObjectIterator<GenericRecord> iter = reader.iterator()) {
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("User1", iter.next().get("userName").toString());
      }
    }
  }

  @Test
  public void test() throws IOException {
    Path path = writeAvroFile();
    testPath(path);
    testPath(copyAvroFile(path));
  }

  public void testPath(Path path) {
    try (AvroReader<TestAvroSchema> reader = new AvroReader<>(TestAvroSchema.class, path)) {
      Assertions.assertEquals(3, reader.size64());
      try (ObjectIterator<TestAvroSchema> iter = reader.iterator()) {
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("User1", iter.next().userName.toString());
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals(5, iter.next().created);
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertNotNull(iter.next());

        Assertions.assertFalse(iter.hasNext());
      }
    }
  }

  @Test
  public void testEmpty() throws IOException {
    AvroReader<TestAvroSchema> emptyReader = new AvroReader<>(TestAvroSchema.class);

    Path path = getTempFile().toPath();
    AvroWriter.toAvroFile(emptyReader, path);

    AvroReader<TestAvroSchema> fileReader = new AvroReader<>(TestAvroSchema.class, path);
    Assertions.assertEquals(0, fileReader.size64());
  }

  private static Path writeAvroAtScale() throws IOException {
    int counter = 0;

    java.nio.file.Path tempDir = Files.createTempDirectory("BIAT");
    tempDir.toFile().deleteOnExit();

    DatumWriter<TestAvroSchema> userDatumWriter = new SpecificDatumWriter<>(TestAvroSchema.class);
    DataFileWriter<TestAvroSchema> dataFileWriter = new DataFileWriter<>(userDatumWriter);
    TestAvroSchema record = new TestAvroSchema();
    record.userName = "User1";
    record.recursion = new TestAvroSchema();
    record.recursion.userName = "Steve";

    for (int fileIndex = 0; fileIndex < FILES_AT_SCALE; fileIndex++) {
      File file = Files.createFile(tempDir.resolve(fileIndex + ".avro")).toFile();
      file.deleteOnExit();
      dataFileWriter.create(TestAvroSchema.SCHEMA$, file);
      for (int recordIndex = 0; recordIndex < RECORDS_PER_FILE_AT_SCALE; recordIndex++) {
        record.created = counter++;
        dataFileWriter.append(record);
      }
      dataFileWriter.close();
    }

    return tempDir;
  }

  @Test
  public void testAtScale() throws IOException {
    Path tempDir = writeAvroAtScale();

    try (AvroReader<TestAvroSchema> reader = new AvroReader<>(TestAvroSchema.class, tempDir)) {
      Assertions.assertEquals(FILES_AT_SCALE * RECORDS_PER_FILE_AT_SCALE, reader.size64());
      int readCounter = 0;
      try (ObjectIterator<TestAvroSchema> iter = reader.iterator()) {
        for (int i = 0; i < reader.size64(); i++) {
          Assertions.assertTrue(iter.hasNext());
          Assertions.assertEquals(readCounter++, iter.next().created);
        }
        Assertions.assertFalse(iter.hasNext());
      }
    }

    // cleanup temp files/directory (I don't trust deleteOnExit where directories are concerned)
    try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(tempDir)) {
      for (Path path : dirStream) {
        Files.delete(path);
      }
    }
    Files.delete(tempDir);
  }
}
