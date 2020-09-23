package com.linkedin.dagli.annotation.equality;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Marks a Dagli node deriving from AbstractProducer, such as a Generator, Transformer or TransformerView, as being
 * equals() to another instance only if they both share the same handle (as returned by their getHandle() methods).
 * This is <strong>almost</strong> equivalent to checking that they are the exact same object, except for certain edge
 * cases such as deserializing the same object multiple times (yielding multiple semantically-identical instances with
 * identical handles).
 *
 * Unless computeEqualsUnsafe() and computeHashCode() are defined by the derived class, Dagli defaults to assuming that
 * a producer derived from AbstractProducer supports (non-commutative) {@link ValueEquality}.  {@link HandleEquality}
 * provides an alternative method to computing equality (and hash codes) for those rare cases where value equality is
 * not appropriate (for example, Placeholders).
 *
 * More specifially, a {@link HandleEquality} producer will be equals to another if and only if:
 * (1) They both have the same class (if one instance has a class that is a subclass of another, they will be considered
 *     not equal)
 * (2) They both have the same handle (as returned by getHandle())
 *
 * The hashCode() of the instance will be based on the handle.
 *
 * It is an error to define {@link ValueEquality} on a class that does not derive from AbstractProducer, and such an
 * annotation will have no effect at run-time.
 *
 * It is also an error if <strong>more than one</strong> of these are true for any class derived from AbstractProducer:
 * (1) computeEqualsUnsafe() and computeHashCode() are defined on this class or any ancestor up to AbstractProducer
 *     (note that it is also an error if one of these is defined but not the other)
 * (2) The {@link HandleEquality} annotation is present
 * (3) The {@link ValueEquality} annotation is present
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ElementType.TYPE})
public @interface HandleEquality { }
