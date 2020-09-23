package com.linkedin.dagli.producer;

import com.linkedin.dagli.producer.internal.RootProducerInternalAPI;

/**
 * The base interface for root (non-child) producer interfaces.
 *
 * @param <R> the type of result produced by this producer.
 */
public interface RootProducer<R> extends Producer<R> {
  @Override
  RootProducerInternalAPI<R, ? extends RootProducer<R>> internalAPI();
}
