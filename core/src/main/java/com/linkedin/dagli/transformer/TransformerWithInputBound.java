package com.linkedin.dagli.transformer;

/**
 * Marker interface that types a transformer by its input type bound: a type that is a supertype of all the
 * transformer's inputs.  Except for variadic and unary transformers, this type bound is always {@link Object} since
 * there is (at present) no way to statically synthesize a common supertype from a set of type arguments.
 *
 * @param <B> a type that is a supertype of all the transformer's inputs (often {@code Object}, since a more concrete
 *            supertype is often not known).
 * @param <R> the result ype of the transformer
 */
public interface TransformerWithInputBound<B, R> extends Transformer<R> { }
