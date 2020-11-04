package com.linkedin.dagli.objectio.kryo;

import com.linkedin.dagli.objectio.ObjectIterator;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.objectio.testing.Tester;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BatchedKryoStreamTest {
  @Test
  public void testInMemory() {
    Tester.testWriter(new KryoMemoryWriter<>());
  }

  @Test
  public void test() throws IOException {
    Tester.testWriter(new KryoMemoryWriter<>());

    Path tempFile = Files.createTempFile("BatchedKryoStreamTest", ".tmp");
    Tester.testWriter(new KryoFileWriter<>(tempFile));
    Files.delete(tempFile);

    tempFile = Files.createTempFile("BatchedKryoStreamTest", ".tmp");
    KryoFileWriter.Config config = new KryoFileWriter.Config();
    config.setCacheHorizon(2);
    try (KryoFileWriter<Integer> ba = new KryoFileWriter<>(tempFile, config)) {
      for (int i = 0; i < 50; i++) {
        ba.write(i);
      }
    }

    // append some more records
    KryoFileWriter<Integer> kryoFileAppender = new KryoFileWriter<>(tempFile, config);
    for (int i = 50; i < 100; i++) {
      kryoFileAppender.write(i);
    }
    kryoFileAppender.close();

    // test createReader()
    try (ObjectReader<Integer> kryoFileReader = kryoFileAppender.createReader(1000);
        ObjectIterator<Integer> ki = kryoFileReader.iterator()) {
      for (int i = 0; i < 100; i++) {
        Assertions.assertEquals(i, (int) ki.next());
      }
    }

    // test opening the file directly with KryoFileReader
    try (KryoFileReader<Integer> ba = new KryoFileReader<>(tempFile);
        ObjectIterator<Integer> bi = ba.iterator()) {
      for (int i = 0; i < 100; i++) {
        Assertions.assertEquals(i, (int) bi.next());
      }
    }
    Files.delete(tempFile);

    config = new KryoFileWriter.Config();
    config.setStreamTransformer(new StreamTransformer() {
      @Override
      public OutputStream transform(OutputStream out) throws IOException {
        return new GZIPOutputStream(out, true);
      }

      @Override
      public InputStream transform(InputStream in) throws IOException {
        return new GZIPInputStream(in);
      }
    });
    tempFile = Files.createTempFile("BatchedKryoStreamTest", ".tmp");
    Tester.testWriter(new KryoFileWriter<>(tempFile, config));
    Files.delete(tempFile);
  }
}
