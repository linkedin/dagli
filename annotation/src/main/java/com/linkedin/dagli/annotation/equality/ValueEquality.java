package com.linkedin.dagli.annotation.equality;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Marks a Dagli node deriving from AbstractProducer, such as a Generator, Transformer or TransformerView, as having
 * "value equality" semantics, which are appropriate for the vast majority of producers.  Value equality is assumed by
 * default (see below), but it is nonetheless recommended that value-equality classes be annotated as such to allow for
 * better detection of errors.  Under value equality, one instance is equal to another instance if and only if:
 * (1) They both have the same class (if one instance has a class that is a subclass of another, they will be
 *     <strong>not</strong> be considered equal)
 * (2) They both have the same inputs (if applicable: Generators do not have inputs)
 * (3) All non-static field values not annotated with {@link IgnoredByValueEquality} have the same values (as determined
 *     by {@link java.util.Objects#equals}).  If the {@link DeepArrayValueEquality} annotation is used on a field, it
 *     will be compared using "deep" equality, in line with {@link java.util.Objects#deepEquals(Object, Object)}.
 *
 * The hashCode() of the instance will also be based on the class name, inputs, and non-static fields not marked as
 * {@link IgnoredByValueEquality}.
 *
 * Unless computeEqualsUnsafe() and computeHashCode() are defined by the derived class or the {@link HandleEquality}
 * annotation is used, Dagli defaults to assuming that a producer derived from AbstractProducer supports
 * (non-commutative) {@link ValueEquality}, equivalent to annotating the class with {@code @ValueEquality(false)}.
 * However, it is recommended that the annotation still be explicitly provided to make the developer's intention clear
 * and allow Dagli's annotation processor to raise an error if the explicitly value-equality class erroneously defines
 * computeEqualsUnsafe() and computeHashCode().
 *
 * It is an error to define {@link ValueEquality} on a class that does not derive from AbstractProducer, and such an
 * annotation will have no effect at run-time.
 *
 * It is also an error if <strong>more than one</strong> of these are true for any class derived from AbstractProducer:
 * (1) computeEqualsUnsafe() and computeHashCode() are defined on this class or any ancestor up to AbstractProducer
 *     (note that it is also an error if one of these is defined but not the other)
 * (2) The {@link HandleEquality} annotation is present
 * (3) The {@link ValueEquality} annotation is present
 *
 * Note that Dagli will use {@link java.lang.reflect.Field#setAccessible(boolean)} to access non-public fields when
 * checking equality.  If you are running in a context with a {@link SecurityManager}, and that security manager forbids
 * this access, a runtime exception will be thrown on the first call to the instance's equals() or hashCode() methods.
 * See {@link java.lang.reflect.Field#setAccessible(boolean)} for more details.
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ElementType.TYPE})
public @interface ValueEquality {
  /**
   * If true, the order of the inputs does not matter when determining equality, so long as every input is present in
   * the same quantity (since a single transformer can accept the same input multiple times) on both instances.
   *
   * This is false by default (i.e. the order of the inputs matters).
   *
   * For root nodes (which have no inputs) the value of this attribute is irrelevant.
   *
   * @return whether or not the inputs to this instance will be treated as commutative
   */
  boolean commutativeInputs() default false;
}
