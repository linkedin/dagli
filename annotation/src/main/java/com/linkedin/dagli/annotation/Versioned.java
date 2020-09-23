package com.linkedin.dagli.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * {@link Versioned} can be checked by an annotation processor to verify that a serialVersionUID field is present on the
 * class and all descendents to ensure consistent serialization.
 *
 * The value field determines whether or not the annotation is strict; if strict, the annotation processor will emit
 * compiler errors rather than warnings if you do not define serialVersionUID.
 *
 * Annotation an interface with {@link Versioned} will cause the annotation processor to emit a compilation error, as
 * such annotations have no effect.
 */
@Inherited
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE})
public @interface Versioned {
  /**
   * Whether or not Versioned is to be enforced strictly.  If true, violations will result in compiler errors rather
   * than warnings.  The default is false (not strict).
   */
  boolean strict() default false;
}
