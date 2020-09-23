package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.producer.Producer;
import it.unimi.dsi.fastutil.longs.LongList;


/**
 * Base class for operations that transform a sequence of integers into something else.
 *
 * @param <S> the ultimate descendant that derives from this class
 */
abstract class AbstractUnaryIntegerSequenceLayer<R, S extends AbstractUnaryIntegerSequenceLayer<R, S>>
    extends AbstractTransformerLayer<LongList, R, S> {
  private static final long serialVersionUID = 1;

  /**
   * Creates a copy of this node that will transform a sequence of integers provided by the given producer.
   *
   * @param numberSequenceProducer an (ordered) array of producers that will provide the input sequence of integers
   * @return a copy of this node that will use the specified inputs
   */
  public final S withInputFromNumberSequence(
      Producer<? extends Iterable<? extends Number>> numberSequenceProducer) {
    return withInput(new NNIntegerSequenceInputLayer().withInput(numberSequenceProducer));
  }

  /**
   * Creates a copy of this node that will transform a sequence of integer inputs provided by the given (ordered)
   * producers.
   *
   * @param producers an (ordered) array of producers that will provide the input sequence of integers
   * @return a copy of this node that will use the specified inputs
   */
  @SafeVarargs
  public final S withInputFromNumbers(Producer<? extends Number>... producers) {
    return withInput(new NNIntegerSequenceInputLayer().withInputs(producers));
  }

  /**
   * Returns a copy of this layer that will accept an input sequence provided by the given producer.
   *
   * The objects in the sequence are indexed (using {@link com.linkedin.dagli.object.Indices}) to convert them into an
   * integer sequence.
   *
   * @param sequenceInput a producer providing a sequence of objects
   * @return a copy of this layer that will use the specified sequence input
   */
  public S withInputFromSequence(Producer<? extends Iterable<?>> sequenceInput) {
    return withInput(new NNIntegerSequenceInputLayer().withInputFromIndexedSequence(sequenceInput));
  }

  /**
   * @return a copy of this instance that will use the specified array input
   * @param input a producer providing an array as a sequence input to the neural network
   */
  public S withInputFromShortArray(Producer<short[]> input) {
    return withInput(new NNIntegerSequenceInputLayer().withInputFromShortArray(input));
  }

  /**
   * @return a copy of this instance that will use the specified array input
   * @param input a producer providing an array as a sequence input to the neural network
   */
  public S withInputFromIntArray(Producer<int[]> input) {
    return withInput(new NNIntegerSequenceInputLayer().withInputFromIntArray(input));
  }

  /**
   * @return a copy of this instance that will use the specified array input
   * @param input a producer providing an array as a sequence input to the neural network
   */
  public S withInputFromLongArray(Producer<long[]> input) {
    return withInput(new NNIntegerSequenceInputLayer().withInputFromLongArray(input));
  }

  @Override
  public S withInput(NNLayer<LongList, ? extends NonTerminalLayer> inputLayer) {
    return super.withInput(inputLayer);
  }
}
