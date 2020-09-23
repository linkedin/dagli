package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.visitor.VisitedBy;


/**
 * Implements simple dot-product attention, where the keys, queries, and values are all the vectors of the input vector
 * sequence.
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
public class NNSelfAttentionLayer extends AbstractAttentionLayer<NNSelfAttentionLayer> {
  private static final long serialVersionUID = 1;
  @Override
  public <T> T accept(NNLayerVisitor<T> visitor) {
    return visitor.visit(this);
  }
}
