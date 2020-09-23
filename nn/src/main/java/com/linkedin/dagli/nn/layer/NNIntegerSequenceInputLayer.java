package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.visitor.VisitedBy;
import com.linkedin.dagli.function.FunctionResult1;
import com.linkedin.dagli.object.Indices;
import com.linkedin.dagli.object.Max;
import com.linkedin.dagli.producer.Producer;
import com.linkedin.dagli.transformer.DynamicInputs;
import com.linkedin.dagli.util.collection.Iterables;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Placeholder for a sequence of integers provided to the model.  The expected type of objects within the sequence is
 * {@link Number}, but note that [@link Float} and {@link Double} values, if provided, will be implicitly truncated to
 * {@link Long}s (the fractional component will be ignored).
 *
 * Clients cannot create placeholders directly; instead, they are implicitly created when passing
 * {@link com.linkedin.dagli.producer.Producer} inputs to {@link NNChildLayer}s.
 */
@VisitedBy("NNLayerVisitor")
public class NNIntegerSequenceInputLayer
    extends AbstractSequencePlaceholderLayer<Number, LongList, NNIntegerSequenceInputLayer> {
  private static final long serialVersionUID = 1;

  private Producer<Long> _maxIntegerProvider = null;

  NNIntegerSequenceInputLayer() { }

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
  public NNIntegerSequenceInputLayer withInput(Producer<? extends Iterable<? extends Number>> input) {
    NNIntegerSequenceInputLayer clone = super.withInput(input);
    clone._maxIntegerProvider = new Max<Long>().withInput(new MaxLongInIterable().withInput(input));
    return clone;
  }

  @Override
  public <R> R accept(NNLayerVisitor<R> visitor) {
    return visitor.visit(this);
  }

  /**
   * Returns a copy of this layer that will accept a sequence of inputs provided by the given producer.
   *
   * The objects in the sequence are indexed (using {@link com.linkedin.dagli.object.Indices}) to convert them into an
   * integer sequence.
   *
   * @param sequenceInput a producer providing a sequence of objects
   * @return a copy of this layer that will use the specified sequence input
   */
  NNIntegerSequenceInputLayer withInputFromIndexedSequence(Producer<? extends Iterable<?>> sequenceInput) {
    return withInput(new Indices<>().withInput(sequenceInput));
  }

  /**
   * @return a copy of this instance that will use the specified array input
   * @param input a producer providing an array as a sequence input to the neural network
   */
  NNIntegerSequenceInputLayer withInputFromShortArray(Producer<short[]> input) {
    return withInput(new FunctionResult1<short[], ShortArrayList>(ShortArrayList::new).withInput(input));
  }

  /**
   * @return a copy of this instance that will use the specified array input
   * @param input a producer providing an array as a sequence input to the neural network
   */
  NNIntegerSequenceInputLayer withInputFromIntArray(Producer<int[]> input) {
    return withInput(new FunctionResult1<int[], IntArrayList>(IntArrayList::new).withInput(input));
  }

  /**
   * @return a copy of this instance that will use the specified array input
   * @param input a producer providing an array as a sequence input to the neural network
   */
  NNIntegerSequenceInputLayer withInputFromLongArray(Producer<long[]> input) {
    return withInput(new FunctionResult1<long[], LongArrayList>(LongArrayList::new).withInput(input));
  }

  @Override
  IntegerInputLayerConfig getDynamicConfig(Map<NNLayer<?, ?>, DynamicLayerConfig> ancestorConfigs,
      DynamicInputs dynamicInputs, DynamicInputs.ConstantInputs constantInputs) {
    return IntegerInputLayerConfig.Builder
        .setOutputShape(new long[]{constantInputs.get(_sequenceLengthProvider)})
        .setMaxIntegerValue(constantInputs.get(_maxIntegerProvider))
        .build();
  }
}
