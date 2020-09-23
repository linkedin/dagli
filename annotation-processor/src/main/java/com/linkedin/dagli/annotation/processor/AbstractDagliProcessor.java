package com.linkedin.dagli.annotation.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.lang.model.SourceVersion;


/**
 * Base class for Dagli {@link AbstractProcessor} implementations.
 */
public abstract class AbstractDagliProcessor extends AbstractProcessor {
  @Override
  public SourceVersion getSupportedSourceVersion() {
    // Dagli annotation processors are very likely to be safe for all future versions of Java; avoid irritating warning:
    return SourceVersion.latest();
  }
}
