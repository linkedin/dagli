package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.math.vector.DenseVector;
import com.linkedin.dagli.math.vector.Vector;
import com.linkedin.dagli.producer.Producer;
import java.util.List;
import java.util.Objects;


/**
 * Base class for NN transformations that transform a sequence of input vectors in some fashion.
 *
 * @param <S> the type of the class ultimately deriving from this one
 */
abstract class AbstractUnaryVectorSequenceLayer<R, S extends AbstractUnaryVectorSequenceLayer<R, S>>
    extends AbstractTransformerLayer<List<DenseVector>, R, S> {
  private static final long serialVersionUID = 1;

  /**
   * Returns a copy of this layer that accepts a sequence of vectors provided by a list of producers as its input.
   *
   * @param inputs the producers providing the inputs to this layer
   * @return a copy of this layer that will use the specified inputs
   */
  @SafeVarargs
  public final S withInputFromVectors(Producer<? extends Vector>... inputs) {
    return withInput(new NNVectorSequenceInputLayer().withInputs(inputs));
  }

  /**
   * @return a copy of this layer that will transformer the sequence of vectors provided by the specified producer
   * @param input a {@link Producer} that will provide a vector sequence input to this layer
   */
  public S withInputFromVectorSequence(Producer<? extends Iterable<? extends Vector>> input) {
    return withInput(new NNVectorSequenceInputLayer().withInput(input));
  }

  @Override
  void validate() {
    Objects.requireNonNull(_inputLayer, "An input layer has not been specified for the layer " + getIdentifier());

    super.validate();
  }

  @Override
  public S withInput(NNLayer<List<DenseVector>, ? extends NonTerminalLayer> inputLayer) {
    return super.withInput(inputLayer);
  }
}
