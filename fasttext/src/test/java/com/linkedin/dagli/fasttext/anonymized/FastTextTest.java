package com.linkedin.dagli.fasttext.anonymized;

import com.linkedin.dagli.fasttext.anonymized.io.BufferedLineReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class FastTextTest {
  @Test
  public void testGettersAndSetters() {
    FastText ft = new FastText();
    Matrix m = new Matrix(1, 1);

    ft.setDict(null);
    Assertions.assertNull(ft.getDict());
    ft.setOutput(m);
    Assertions.assertSame(m, ft.getOutput());
    ft.setInput(m);
    Assertions.assertSame(m, ft.getInput());
    ft.setModel(new Model(m, m, new Args(), 0));
    Assertions.assertNotNull(ft.getModel());
    ft.setArgs(new Args());
    Assertions.assertNotNull(ft.getArgs());
    ft.setLineReaderClass(BufferedLineReader.class);
    Assertions.assertEquals(BufferedLineReader.class, ft.getLineReaderClass());
    ft.setCharsetName("abc");
    Assertions.assertEquals("abc", ft.getCharsetName());
  }
}
