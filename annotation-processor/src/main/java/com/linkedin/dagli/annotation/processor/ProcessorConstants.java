package com.linkedin.dagli.annotation.processor;

/**
 * Common constants for use in processing Dagli annotations.
 */
public abstract class ProcessorConstants {
  public static final String PRODUCER_EQUALS_METHOD_NAME = "computeEqualsUnsafe";
  public static final String PRODUCER_HASH_CODE_METHOD_NAME = "computeHashCode";
  public static final String DAGLI_PRODUCER_PACKAGE = "com.linkedin.dagli.producer";

  private ProcessorConstants() { }
}
