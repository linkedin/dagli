package com.linkedin.dagli.transformer.internal;

import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.producer.internal.ChildProducerInternalAPI;
import com.linkedin.dagli.transformer.Transformer;
import java.util.List;

/**
 * Base interface for the internal API of transformers.
 *
 * @param <R> the type of value produced by the transformer
 * @param <S> the ultimate derived type of the transformer
 */
public interface TransformerInternalAPI<R, S extends Transformer<R>> extends ChildProducerInternalAPI<R, S> {

  /**
   * Gets a list of all the inputs to this transformer.
   *
   * @return a list of the transformer's inputs
   */
  @Override
  List<? extends Producer<?>> getInputList();
}
