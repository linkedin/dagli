package com.linkedin.dagli.annotation.struct;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * This is the pluralization class for the repeatable {@link HasStructField} annotation.
 * This is required by the @Repeatable annotation; it's not intended for actual use.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface HasStructFields {
  /**
   * @return the array of {@link HasStructField}s present on the class
   */
  HasStructField[] value();
}
