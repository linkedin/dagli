package com.linkedin.dagli.annotation.producer.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * This annotation is used internally by Dagli to annotate AbstractProducer (the root of concrete impleemntations of
 * DAG nodes), allowing it (and all its descendents) to be processed by Dagli's annotation processor and receive various
 * correctness checks (such as checking the correct use of equality semantics annotations like
 * {@link com.linkedin.dagli.annotation.equality.ValueEquality}).  It should not be applied to other classes.
 */
@Inherited
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE})
public @interface IsAbstractProducer { }
