package com.linkedin.dagli.annotation.processor.producer;

import com.linkedin.dagli.annotation.processor.ProcessorConstants;
import java.util.Arrays;
import java.util.List;


abstract class ProducerConstants {
  private ProducerConstants() { }

  static final List<String> EQUALITY_METHODS =
      Arrays.asList(ProcessorConstants.PRODUCER_HASH_CODE_METHOD_NAME, ProcessorConstants.PRODUCER_EQUALS_METHOD_NAME);

  static final String ABSTRACT_PRODUCER_CLASS_NAME = ProcessorConstants.DAGLI_PRODUCER_PACKAGE + ".AbstractProducer";
}
