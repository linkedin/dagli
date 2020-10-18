package com.linkedin.dagli.producer;

import com.linkedin.dagli.producer.internal.RootProducerInternalAPI;

/**
 * The base class for root (non-child) producers.
 *
 * @param <R> the type of result produced by the associated producer
 * @param <I> the type of the internal API provided by this producer
 * @param <S> the ultimate derived type of this producer
 */
public abstract class AbstractRootProducer<R, I extends RootProducerInternalAPI<R, S>, S extends AbstractRootProducer<R, I, S>>
    extends AbstractProducer<R, I, S>
    implements RootProducer<R> {
  private static final long serialVersionUID = 1;

  /**
   * Creates a new root with a random UUID
   *
   * @param name a human-friendly name for the producer (may be null, in which case the default name will be used)
   */
  public AbstractRootProducer(String name) {
    super(name);
  }

  /**
   * Creates a new root with the specified UUID
   *
   * @param name a human-friendly name for the producer (may be null, in which case the default name will be used)
   * @param uuidMostSignificantBits most significant 64 bits of the UUID
   * @param uuidLeastSignificantBits least significant 64 bits of the UUID
   */
  public AbstractRootProducer(String name, long uuidMostSignificantBits, long uuidLeastSignificantBits) {
    super(name, uuidMostSignificantBits, uuidLeastSignificantBits);
  }

  protected abstract class InternalAPI extends AbstractProducer<R, I, S>.InternalAPI
      implements RootProducerInternalAPI<R, S> { }
}
