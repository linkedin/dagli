package com.linkedin.dagli.fasttext.anonymized;

import org.junit.jupiter.api.Test;


public class MatrixTest {
  @Test
  public void testToString() {
    Matrix m = new Matrix(2, 2);

    // make sure toString doesn't throw
    m.toString();
  }
}
