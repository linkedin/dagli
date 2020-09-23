package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.visitor.VisitedBy;
import com.linkedin.dagli.producer.Producer;


/**
 * Implements dot-product attention where one or more queries are learned parameters of the model while the keys and
 * values are the vectors of the input vector sequence.
 *
 * The output is a vector sequence of shape [number of queries, output size] (note the change in sequence length: it
 * is independent of the length of the input sequence).
 *
 * Attention is determined by the dot-product of the (potentially projected) vectors of the input vector: the greater
 * the relative (as determined by softmax) dot-product similarity between the key and query, the greater the weight
 * of the corresponding vector in the input sequence (the "value") in the resulting weighted sum of values constituting
 * the head (also potentially projected.)
 *
 * Projections are accomplished via multiplication with weight matrices.
 *
 * If applying attention to an ordered sequence (such as a sequence of text token embeddings), you should strongly
 * consider applying positional encoding to the sequence using {@link NNPositionalEncodedLayer} before passing it to
 * the attention layer.
 *
 * Background reading:
 * (1) <a href="http://jalammar.github.io/illustrated-transformer/">The Illustrated Transformer</a> (an excellent
 *     introduction to self-attention)
 * (2) <a href="https://arxiv.org/abs/1706.03762">Attention is All You Need</a>
 */
@VisitedBy("NNLayerVisitor")
public class NNLearnedSelfAttentionLayer extends AbstractAttentionLayer<NNLearnedSelfAttentionLayer> {
  private static final long serialVersionUID = 1;

  @Override
  public NNLearnedSelfAttentionLayer withQueryCount(Producer<? extends Number> queryCountProvider) {
    return super.withQueryCount(queryCountProvider);
  }

  @Override
  public NNLearnedSelfAttentionLayer withQueryCount(long queryCount) {
    return super.withQueryCount(queryCount);
  }

  @Override
  public <T> T accept(NNLayerVisitor<T> visitor) {
    return visitor.visit(this);
  }
}
