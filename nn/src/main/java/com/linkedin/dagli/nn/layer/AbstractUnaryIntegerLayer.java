package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.producer.Producer;


/**
 * Abstract class for layers accepting an integer input.
 *
 * @param <S> the type of the ultimate derived class descending from this base class
 */
abstract class AbstractUnaryIntegerLayer<R, S extends AbstractUnaryIntegerLayer<R, S>>
    extends AbstractTransformerLayer<Long, R, S> {
  private static final long serialVersionUID = 1;

  /**
   * Returns a copy of this layer that will obtain its input from the (long) integer provided by the specified input.
   *
   * The integer will be obtaine from the {@link Number} instances via {@link Number#longValue()}; this may involve
   * truncation or rounding.
   *
   * @param input a {@link Producer} providing the (long) integer serving as the input to this layer
   * @return a copy of this layer that will accept the provided input
   */
  public S withInputFromNumber(Producer<? extends Number> input) {
    return withInput(new NNIntegerInputLayer().withInput(input));
  }

  /**
   * Returns a copy of this layer that will use accept as its input the index of an arbitrary input.  Indices are
   * determined using {@link com.linkedin.dagli.object.Index}.
   *
   * @param input a producer providing the objects to be indexed
   * @return a copy of this layer that will use the specified indexed input
   */
  public S withInputFromIndexedValue(Producer<?> input) {
    return withInput(new NNIntegerInputLayer().withInputFromIndexedValue(input));
  }

  @Override
  public S withInput(NNLayer<Long, ? extends NonTerminalLayer> inputLayer) {
    return super.withInput(inputLayer);
  }
}
