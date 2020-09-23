package com.linkedin.dagli.annotation.visitor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * This is the pluralization class for the repeatable {@link Visitor} annotation.
 * This is required by the {@link java.lang.annotation.Repeatable} annotation; it's not intended for actual use.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.PACKAGE})
public @interface Visitors {
  /**
   * @return the array of {@link Visitor}s present on the class
   */
  Visitor[] value();
}
