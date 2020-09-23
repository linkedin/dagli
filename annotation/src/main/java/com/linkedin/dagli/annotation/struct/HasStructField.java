package com.linkedin.dagli.annotation.struct;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * This annotation is automatically generated for classes generated from @Struct definitions.  You should not use
 * it directly, and, in particular, it has no effect on the classes annotated with @Struct that serve to define the
 * struct; use normal field declarations instead--see {@link Struct} for details.
 *
 * {@link HasStructField} provides metadata about the fields in a Struct; this is a more robust, easier and faster
 * alternative to using reflection to identify fields directly.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Repeatable(HasStructFields.class)
public @interface HasStructField {
  /**
   * @return whether or not the field must be specified when the object is created (an optional field that is not
   *         specified will take a default value.)
   */
  boolean optional();

  /**
   * @return the name of the field in camelCase
   */
  String name();

  /**
   * @return The type of the field.  For primitives, this will be the primitive class, <b>not</b> the boxed type.
   *         E.g. a double field will have the class "double.class".
   */
  Class<?> type();
}
