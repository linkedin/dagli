package com.linkedin.dagli.annotation.struct;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * This is the pluralization class for the repeatable {@link OptionalField} annotation.
 * This is required by the @Repeatable annotation; it's not intended for actual use.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE})
@Inherited
public @interface OptionalFields {
  /**
   * @return the array of {@link OptionalField}s present on the class
   */
  OptionalField[] value();
}
