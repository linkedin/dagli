package com.linkedin.dagli.annotation.equality;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Fields marked by this annotation will be compared as deep arrays when determining "{@link ValueEquality}".  If this
 * annotation is not used, arrays are normally compared by reference, consistent with the behavior of
 * {@link java.util.Objects#equals(Object, Object)}.
 *
 * Dagli's annotation processor will generate a compiler error if this annotation is applied to something that is
 * obviously not an array.  Use of this annotation on a field typed that <em>could</em> contain an array, but doesn't,
 * will not affect correctness, although it will increase the computational cost of checking equality.  Cases where a
 * field sometimes contains an array and sometimes doesn't will also be handled correctly.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface DeepArrayValueEquality { }
