package com.linkedin.dagli.transformer;

import com.linkedin.dagli.producer.AbstractChildProducer;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.internal.TransformerInternalAPI;
import java.util.List;

/**
 * Base class for transformers.  Note that derived transformer implementations should not extend this class directly.
 * Instead, extend the base class corresponding to the type (prepared/preparable) and arity (e.g. variadic, 1, 4...)
 * of the transformer you're creating, e.g. {@link AbstractPreparedTransformer2}.
 *
 * @param <R> the type of value produced by this transformer
 * @param <I> the type of the internal API object used by this transformer
 * @param <S> the ultimate derived type of the transformer extending this class
 */
abstract class AbstractTransformer<R, I extends TransformerInternalAPI<R, S>, S extends AbstractTransformer<R, I, S>>
    extends AbstractChildProducer<R, I, S> implements Transformer<R> {

  private static final long serialVersionUID = 1;

  /**
   * Get the ordered list of inputs for this transformer.  {@link com.linkedin.dagli.producer.MissingInput} values
   * should be used for inputs that have not yet been set.
   *
   * @return the ordered list of inputs
   */
  abstract protected List<? extends Producer<?>> getInputList();

  protected abstract class InternalAPI extends AbstractChildProducer<R, I, S>.InternalAPI
      implements TransformerInternalAPI<R, S> { }
}
