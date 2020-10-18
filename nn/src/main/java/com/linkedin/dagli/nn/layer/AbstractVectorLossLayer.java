package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.producer.Producer;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/**
 * Base class for terminal layers that have a vector input.
 *
 * @param <R> the type of client-visible output this layer produces during inference
 * @param <S> the type of the derived class
 */
abstract class AbstractVectorLossLayer<R, S extends AbstractVectorLossLayer<R, S>>
    extends AbstractTransformerLayer<DenseVector, R, S> implements LossLayer {
  private static final long serialVersionUID = 1;

  // This producer provides our supervision (a vector corresponding to the "right" layer outputs for a given example)
  Producer<? extends Vector> _supervisionProvider = null;

  @Override
  void validate() {
    Objects.requireNonNull(_supervisionProvider,
        "Labels have not been specified for the regression layer " + getName());

    super.validate();
  }

  @Override
  public InternalAPI internalAPI() {
    return new InternalAPI();
  }

  /**
   * Methods provided exclusively for use by Dagli.
   *
   * Client code should not use these methods as they are subject to change at any time.
   */
  public class InternalAPI extends AbstractTransformerLayer<DenseVector, R, S>.InternalAPI {
    /**
     * @return the producer providing the supervision vector for this layer.
     */
    public Producer<? extends Vector> getLabelVectorProducer() {
      return _supervisionProvider;
    }

    @Override
    public List<? extends Producer<?>> getExampleInputProducers() {
      return Collections.singletonList(_supervisionProvider);
    }
  }
}
