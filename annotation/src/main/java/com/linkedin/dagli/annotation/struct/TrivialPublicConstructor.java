package com.linkedin.dagli.annotation.struct;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * This annotation may be used on {@link Struct}s to force the generation of a no-argument public constructor.  This is
 * only possible if the {@link Struct} has no non-optional fields, and a compiler error will be emitted if this is not
 * the case.
 *
 * This can be convenient if you wish your {@link Struct} to be configured with "in-place builder" {@code with___(...)}
 * methods and aren't overly concerned about efficiency (each {@code with___(...)} calls entails creating a copy of
 * the struct).
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE})
public @interface TrivialPublicConstructor { }
