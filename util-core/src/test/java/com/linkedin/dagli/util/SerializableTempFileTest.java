package com.linkedin.dagli.util;

import com.linkedin.dagli.util.io.SerializableTempFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class SerializableTempFileTest {
  @Test
  public void test() throws IOException, ClassNotFoundException {
    File file = File.createTempFile("SerializableTempFileTest", "");
    file.deleteOnExit();
    Files.write(file.toPath(), new byte[] {1, 2, 3, 4});

    SerializableTempFile stf = new SerializableTempFile(file);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream out = new ObjectOutputStream(baos);
    out.writeObject(stf);
    out.close();

    file.delete();

    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    ObjectInputStream ois = new ObjectInputStream(bais);
    SerializableTempFile readSTF = (SerializableTempFile) ois.readObject();

    assertArrayEquals(Files.readAllBytes(readSTF.getFile().toPath()), new byte[] {1, 2, 3, 4});
  }
}
