package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.visitor.VisitedBy;
import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.object.Max;
import com.linkedin.dagli.producer.MissingInput;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.DynamicInputs;
import com.linkedin.dagli.util.collection.Iterables;
import java.util.List;
import java.util.Map;


/**
 * Placeholder for a sequence of integers provided to the model.  The expected type of objects within the sequence is
 * {@link Number}, but note that [@link Float} and {@link Double} values, if provided, will be implicitly truncated to
 * {@link Long}s (the fractional component will be ignored).
 *
 * The dimensions of the output of the layer are:
 * [maximum sequence length, Max(0, maximum non-negative vector index) + 1]
 */
@VisitedBy("NNLayerVisitor")
public class NNVectorSequenceInputLayer
    extends AbstractSequencePlaceholderLayer<Vector, List<DenseVector>, NNVectorSequenceInputLayer> {
  private static final long serialVersionUID = 1;

  private Producer<Long> _maxNonZeroElementIndexProvider = MissingInput.get();

  NNVectorSequenceInputLayer() { }

  @Override
  public <R> R accept(NNLayerVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public NNVectorSequenceInputLayer withInput(Producer<? extends Iterable<? extends Vector>> input) {
    NNVectorSequenceInputLayer res = super.withInput(input);
    res._maxNonZeroElementIndexProvider =
        new Max<Long>().withInput(new MaxNonZeroElementIndexInVectors().withInput(input));
    return res;
  }

  @Override
  List<? extends Producer<?>> getDynamicConfigurationInputProducers() {
    return Iterables.append(super.getDynamicConfigurationInputProducers(), _maxNonZeroElementIndexProvider);
  }

  @Override
  DynamicLayerConfig getDynamicConfig(Map<NNLayer<?, ?>, DynamicLayerConfig> ancestorConfigs,
      DynamicInputs dynamicInputs, DynamicInputs.ConstantInputs constantInputs) {
    return DynamicLayerConfig.Builder
        .setOutputShape(new long[]{constantInputs.get(_sequenceLengthProvider),
            constantInputs.get(_maxNonZeroElementIndexProvider) + 1})
        .build();
  }
}
