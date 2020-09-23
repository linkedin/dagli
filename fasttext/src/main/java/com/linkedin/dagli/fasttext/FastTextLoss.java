package com.linkedin.dagli.fasttext;

public enum FastTextLoss {
  NEGATIVE_SAMPLING("ns"),
  HEIRARCHICAL_SOFTMAX("hs"),
  SOFTMAX("softmax");


  private final String _argumentName;

  /**
   * @return the name that should be used for this loss type when passed to the FastText backend
   */
  public String getArgumentName() {
    return _argumentName;
  }

  FastTextLoss(String argName) {
    _argumentName = argName;
  }
}
