package com.linkedin.dagli.annotation.struct;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * The Accessibility annotation can be used on {@link Struct}s to indicate what level of accessibility the generated
 * struct class should be.
 *
 * If this annotation is not used, the default accessibility is PUBLIC.
 *
 * Here's an example that defines a struct with package-private accessibility:
 * <pre>
 * {@code @Accessibility(Accessibility.Level.PACKAGE_PRIVATE)}
 * {@code @Struct("Record")}
 * {@code class RecordDef {
 *   String name;
 *   int age;
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE})
public @interface Accessibility {
  /**
   * @return the accessibility level that will be used by the generated struct class.
   */
  Level value();

  enum Level {
    PUBLIC,
    PACKAGE_PRIVATE,
  }
}
