package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.visitor.VisitedBy;
import com.linkedin.dagli.object.Convert;
import com.linkedin.dagli.object.Index;
import com.linkedin.dagli.object.Max;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.DynamicInputs;
import com.linkedin.dagli.util.collection.Iterables;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Placeholder for an integer input from the encapsulating DAG.
 *
 * Clients cannot create placeholders directly; instead, they are implicitly created when passing
 * {@link com.linkedin.dagli.producer.Producer} inputs to {@link NNChildLayer}s.
 */
@VisitedBy("NNLayerVisitor")
public class NNIntegerInputLayer extends AbstractPlaceholderLayer<Number, Long, NNIntegerInputLayer> {
  private static final long serialVersionUID = 1;

  private Producer<Long> _maxIntegerProvider = null;

  NNIntegerInputLayer() { }

  @Override
  List<? extends Producer<?>> getDynamicConfigurationInputProducers() {
    return Iterables.append(super.getDynamicConfigurationInputProducers(), _maxIntegerProvider);
  }

  @Override
  void validate() {
    super.validate();
    Objects.requireNonNull(_maxIntegerProvider, "No max-value producer set");
  }

  @Override
  public NNIntegerInputLayer withInput(Producer<? extends Number> input) {
    NNIntegerInputLayer clone = super.withInput(input);
    clone._maxIntegerProvider = new Max<Long>().withInput(Convert.Number.toLong(input));
    return clone;
  }

  /**
   * Returns a copy of this layer that will use accept as its input the index of an arbitrary input.  Indices are
   * determined using {@link com.linkedin.dagli.object.Index}.
   *
   * @param input a producer providing the objects to be indexed
   * @return a copy of this layer that will use the specified indexed input
   */
  NNIntegerInputLayer withInputFromIndexedValue(Producer<?> input) {
    return withInput(new Index<>().withInput(input));
  }

  @Override
  public <R> R accept(NNLayerVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  IntegerInputLayerConfig getDynamicConfig(Map<NNLayer<?, ?>, DynamicLayerConfig> ancestorConfigs,
      DynamicInputs dynamicInputs, DynamicInputs.ConstantInputs constantInputs) {
    return IntegerInputLayerConfig.Builder
        .setOutputShape(new long[]{1})
        .setMaxIntegerValue(constantInputs.get(_maxIntegerProvider))
        .build(); // scalar output
  }
}
