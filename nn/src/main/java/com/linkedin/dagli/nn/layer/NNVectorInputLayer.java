package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.visitor.VisitedBy;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.object.Max;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.DynamicInputs;
import com.linkedin.dagli.util.invariant.Arguments;
import com.linkedin.dagli.vector.MaxNonZeroVectorElementIndex;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Accepts a vector input.  This vector is assumed to be "dense-like": any elements with negative elements are ignored,
 * and the width of the input layer will be large enough to accommodate all non-zero elements (with non-negative
 * indices); very high sparse indices will thus likely cause out-of-memory exceptions.
 *
 * Clients cannot create placeholders directly; instead, they are implicitly created when passing
 * {@link com.linkedin.dagli.producer.Producer} inputs to {@link NNChildLayer}s.
 */
@VisitedBy("NNLayerVisitor")
public class NNVectorInputLayer extends AbstractPlaceholderLayer<Vector, DenseVector, NNVectorInputLayer> {
  private static final long serialVersionUID = 1;

  private long _maxWidth = Long.MAX_VALUE;

  NNVectorInputLayer() { }

  /**
   * Sets the maximum width for this input layer.  Any inputted vector elements with indices equal or greater than this
   * (or less than zero) will be ignored.  This can be used to avoid any risk of an out-of-memory condition if there
   * is a vector with a very high-index element.
   *
   * By default, the maximum width is {@link Long#MAX_VALUE} (effectively no limit).
   *
   * @param maxWidth the maximum width for this input layer
   * @return a copy of this instance that will use the specified maximum width
   */
  public NNVectorInputLayer withMaxWidth(long maxWidth) {
    Arguments.check(maxWidth > 0, "The maximum width must be at least 1");
    return clone(c -> c._maxWidth = maxWidth);
  }

  Producer<Long> getMaxElementIndexProducer() {
    return new Max<Long>().withInput(
        new MaxNonZeroVectorElementIndex().withResultOnZeroVector(Long.MIN_VALUE).withInput(getInputProducer()));
  }

  @Override
  List<? extends Producer<?>> getDynamicConfigurationInputProducers() {
    return Collections.singletonList(getMaxElementIndexProducer());
  }

  @Override
  public <R> R accept(NNLayerVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  DynamicLayerConfig getDynamicConfig(Map<NNLayer<?, ?>, DynamicLayerConfig> ancestorConfigs,
      DynamicInputs dynamicInputs, DynamicInputs.ConstantInputs constantInputs) {
    return DynamicLayerConfig.Builder
        .setOutputShape(
            new long[]{Math.min(_maxWidth, Math.max(1, constantInputs.get(getMaxElementIndexProducer()) + 1))})
        .build();
  }
}
