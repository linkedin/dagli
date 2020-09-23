package com.linkedin.dagli.annotation.equality;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fields marked by this annotation will be ignored when determining whether two objects are equal via
 * "{@link ValueEquality}".  If a class is marked with this annotation, all fields in the class (but not in its super or
 * subclass) will be ignored.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface IgnoredByValueEquality { }
