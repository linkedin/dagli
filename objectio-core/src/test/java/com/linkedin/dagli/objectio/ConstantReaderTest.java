package com.linkedin.dagli.objectio;

import com.linkedin.dagli.objectio.testing.Tester;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;


public class ConstantReaderTest {
  @Test
  public void test() {
    ConstantReader<Object> bc = new ConstantReader<>("z", 100);
    ArrayList<Object> zs = new ArrayList<>(100);
    for (int i = 0; i < 100; i++) {
      zs.add("z");
    }

    Tester.testReader(bc, zs);
  }
}
