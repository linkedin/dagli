package com.linkedin.dagli.producer;

/**
 * This is a marker interface that enforces mutual exclusion among types of producers, i.e. ensuring that a
 * class cannot be both a preparable and prepared transformer.
 *
 * @param <R> the type of output from this producer
 * @param <T> the type of the producer
 */
public interface ProducerType<R, T extends Producer<R>> extends Producer<R> { }