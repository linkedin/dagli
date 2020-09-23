package com.linkedin.dagli.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * This annotation asserts that all or none of the methods should be defined on a given class.  If only some of the
 * methods are defined, an error will be generated during compilation (assuming that Dagli's annotation processor is
 * being applied).
 */
@Inherited
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE})
public @interface DefinesAllOrNoneOfTheseMethods {
  String[] value();
}
