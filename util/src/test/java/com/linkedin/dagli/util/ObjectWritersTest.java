package com.linkedin.dagli.util;

import com.linkedin.dagli.objectio.kryo.KryoFileWriter;
import com.linkedin.dagli.objectio.ObjectIterator;
import com.linkedin.dagli.objectio.ObjectReader;
import com.linkedin.dagli.util.kryo.KryoWriters;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class ObjectWritersTest {
  private static final String DATA = "Hello there!";
  private static final String PADDED_DATA = DATA + " "; // Kryo likes to mangle the last byte

  @Test
  public void test() throws IOException {
    testWithOptions(false, false);
    testWithOptions(false, true);
    testWithOptions(true, false);
    testWithOptions(true, true);
  }

  public void testWithOptions(boolean compression, boolean encryption) throws IOException {
    Path path = Files.createTempFile("ObjectWritersTest", "txt");

    KryoFileWriter<String> appendable = KryoWriters.kryoFromPath(path, compression, encryption);
    for (int i = 0; i < 100000; i++) {
      appendable.write(PADDED_DATA);
    }
    appendable.close();

    try (ObjectReader<String> reader = appendable.createReader();
         ObjectIterator<String> iter = reader.iterator()) {
      for (int i = 0; i < 100000; i++) {
        Assertions.assertEquals(iter.next(), PADDED_DATA);
      }
      Assertions.assertFalse(iter.hasNext());
    }

    String text = bytesToString(Files.readAllBytes(path));
    Assertions.assertEquals(!text.contains(DATA), compression || encryption);

    Files.delete(path);
  }

  private static String bytesToString(byte[] bytes) {
    StringBuilder builder = new StringBuilder(bytes.length);
    for (byte b : bytes) {
      builder.append((char) b);
    }
    return builder.toString();
  }
}
