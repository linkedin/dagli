package com.linkedin.dagli.producer;

import com.linkedin.dagli.producer.internal.ChildProducerInternalAPI;
import java.util.List;


/**
 * The base class for child (non-root) producers.
 *
 * @param <R> the type of result produced by the associated producer
 * @param <I> the type of the internal API provided by this producer
 * @param <S> the ultimate derived type of this producer
 */
public abstract class AbstractChildProducer<R, I extends ChildProducerInternalAPI<R, S>, S extends AbstractChildProducer<R, I, S>>
    extends AbstractProducer<R, I, S>
    implements ChildProducer<R> {
  private static final long serialVersionUID = 1;

  protected abstract class InternalAPI extends AbstractProducer<R, I, S>.InternalAPI
      implements ChildProducerInternalAPI<R, S> { }

  @Override
  public void validate() {
    super.validate();
    List<? extends Producer<?>> parents = this.internalAPI().getInputList();
    if (parents.isEmpty()) {
      throw new IllegalStateException(this.getName() + " is a child producer with no parents");
    }

    for (int i = 0; i < parents.size(); i++) {
      Producer<?> parent = parents.get(i);
      if (parent == null) {
        throw new IllegalStateException(this.getName() + " has null parent producer as input #" + i
            + ".  This probably means you forgot to set an input on this transformer, e.g. using withInput(...).");
      } else if (parent == MissingInput.get()) {
        throw new IllegalStateException(this.getName() + " has a MissingInput parent producer as input #" + i
            + ".  This probably means you forgot to set an input on this transformer, e.g. using withInput(...).");
      }
    }
  }
}
