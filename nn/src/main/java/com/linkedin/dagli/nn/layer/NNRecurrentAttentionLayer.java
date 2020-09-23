package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.visitor.VisitedBy;
import com.linkedin.dagli.nn.activation.ActivationFunction;
import com.linkedin.dagli.nn.activation.HyperbolicTangent;
import com.linkedin.dagli.producer.Producer;


/**
 * Implements dot-product attention where the query at each timestep is the output from the previous timestep, while the
 * keys and values are the vectors of the input vector sequence.
 *
 * This formulation comes from DL4J's RecurrentAttentionLayer; the specific formulation is described therein as:
 * o_i = activation(W * x_i + R * attention(o_{i-1}, x, x) + b)
 * (where o is the sequence of outputs, x is the input sequence, b is a bias vector, and R and W are learned projection
 * matrices).
 *
 * The output is a vector sequence of shape [length of the input sequence, output size].
 *
 * Attention is determined by the dot-product of the (potentially projected) vectors of the input vector: the greater
 * the relative (as determined by softmax) dot-product similarity between the key and query, the greater the weight
 * of the corresponding vector in the input sequence (the "value") in the resulting weighted sum of values constituting
 * the head (also potentially projected.)
 *
 * Projections are accomplished via multiplication with weight matrices.
 *
 * Background reading:
 * (1) <a href="http://jalammar.github.io/illustrated-transformer/">The Illustrated Transformer</a> (an excellent
 *     introduction to self-attention)
 * (2) <a href="https://arxiv.org/abs/1706.03762">Attention is All You Need</a>
 */
@VisitedBy("NNLayerVisitor")
public class NNRecurrentAttentionLayer extends AbstractAttentionLayer<NNRecurrentAttentionLayer> {
  private static final long serialVersionUID = 1;

  private ActivationFunction _activationFunction = new HyperbolicTangent();

  /**
   * @return the activation function used by this layer
   */
  public ActivationFunction getActivationFunction() {
    return _activationFunction;
  }

  /**
   * Returns a copy of this instance that will use the specified activation function to squash its outputs.
   *
   * The default is {@link HyperbolicTangent}.
   *
   * @param activationFunction the activation function to use
   * @return a copy of this instance that will use the specified activation function to squash its outputs
   */
  public NNRecurrentAttentionLayer withActivationFunction(ActivationFunction activationFunction) {
    return clone(c -> c._activationFunction = activationFunction);
  }

  @Override
  public NNRecurrentAttentionLayer withQueryCount(Producer<? extends Number> queryCountProvider) {
    return super.withQueryCount(queryCountProvider);
  }

  @Override
  public NNRecurrentAttentionLayer withQueryCount(long queryCount) {
    return super.withQueryCount(queryCount);
  }

  @Override
  public <T> T accept(NNLayerVisitor<T> visitor) {
    return visitor.visit(this);
  }
}
