package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.visitor.VisitedBy;
import com.linkedin.dagli.util.invariant.Arguments;


/**
 * Pools a sequence of vectors from the input layer via p-norm pooling to create a single vector output (each element in
 * the output is the p-norm of all the corresponding elements in the input vectors).
 *
 * The size of the output vector is the same size as each of the vectors in the input sequence.
 */
@VisitedBy("NNLayerVisitor")
public class NNPNormPoolingLayer extends AbstractVectorSequencePoolingLayer<NNPNormPoolingLayer> {
  private static final long serialVersionUID = 1;

  private int _p = 2;
  private double _dropoutProbability = Double.NaN;

  /**
   * Returns a copy of this instance that will use the specified dropout rate.
   *
   * Each time an example is processed by the network, each element (number) in the input values passed to this layer
   * will have this (independent) probability of being "dropped" (set to 0), which can help mitigate overfitting.
   *
   * The default value, NaN, means "use the neural network's global dropout rate" (which itself defaults to 0).
   * Otherwise, values may range from 0 (no dropout) to 1 (drop everything, which is definitely not a good idea).
   *
   * @param probability a [0, 1] dropout probability, or NaN to use the global network rate
   * @return a copy of this instance that will use the specified dropout rate.
   */
  public NNPNormPoolingLayer withDropoutProbability(double probability) {
    Arguments.check(Double.isNaN(probability) || (probability >= 0 && probability <= 1), "Invalid probability");
    return clone(c -> c._dropoutProbability = probability);
  }

  /**
   * @return the [0, 1] dropout probability for this layer, or NaN if this layer should use the neural network's global
   *         dropout rate
   */
  public double getDropoutProbability() {
    return _dropoutProbability;
  }


  /**
   * @return the p-value that determines which p-norm is used for pooling
   */
  public int getP() {
    return _p;
  }

  /**
   * Returns a copy of this layer that will pool via a p-norm with the specified p value.  For example, the p-norm where
   * p == 2 is the Euclidean norm and p == 1 is the Manhattan norm.
   *
   * By default, p is 2 (Euclidean norm).
   *
   * @param p the p value to use; must be >= 1
   * @return a copy of this layer that will pool via a p-norm with the specified p value
   */
  public NNPNormPoolingLayer withP(int p) {
    return clone(c -> c._p = p);
  }

  @Override
  public <R> R accept(NNLayerVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
