package com.linkedin.dagli.objectio;

import com.linkedin.dagli.objectio.testing.Tester;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;


public class CollectionWrapperTest {
  @Test
  public void test() {
    ArrayList<Object> list = new ArrayList<>();

    CollectionWriter<Object> bcw = new CollectionWriter<>(list);
    Tester.testWriter(bcw);
  }
}
