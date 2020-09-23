package com.linkedin.dagli.producer.internal;

import com.linkedin.dagli.producer.RootProducer;

/**
 * The base interface for internal APIs of root (non-child) producers.
 *
 * @param <R> the type of result produced by the associated producer
 * @param <S> the type of the producer
 */
public interface RootProducerInternalAPI<R, S extends RootProducer<R>> extends ProducerInternalAPI<R, S> { }
