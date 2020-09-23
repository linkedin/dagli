package com.linkedin.dagli.annotation.struct;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Indicates that a field inside a @Struct class definition should be considered optional.
 *
 * Optional values do not need to be set explicitly by client code and, if not set, they assume the value of their
 * initializer expression, or their default value if no initializer is provided.
 *
 * {@link OptionalField} and {@link Optional} have identical semantics, but {@link Optional} is preferred and specified
 * directly on the target fields whereas {@link OptionalField} is intended for cases where the class defining the field
 * cannot be modified and is specified on the @Struct definition class (the class decorated with the {@link Struct}
 * annotation).
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD})
public @interface Optional { }
