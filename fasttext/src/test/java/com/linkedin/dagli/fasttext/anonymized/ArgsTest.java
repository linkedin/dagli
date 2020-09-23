package com.linkedin.dagli.fasttext.anonymized;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class ArgsTest {
  @Test
  public void testToString() {
    // very basic sanity test to make sure toString is generating *something* (it's only used for human inspection)
    Args args = new Args();
    args.bucket = 1337;
    Assertions.assertTrue(args.toString().contains("1337"));
  }

  @Test
  public void testPrintHelp() {
    Args args = new Args();

    // we're verifying that no exception is thrown here (otherwise the method is extremely straightforward):
    args.printHelp();
  }

  @Test
  public void testIO() throws IOException {
    Args args = new Args();
    args.bucket = 1337;

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    args.save(baos);

    Args loaded = new Args();
    loaded.load(new ByteArrayInputStream(baos.toByteArray()));
    Assertions.assertEquals(args.bucket, loaded.bucket);
  }
}
