package com.linkedin.dagli.dl4j;

import com.linkedin.dagli.nn.loss.AbsoluteErrorLoss;
import com.linkedin.dagli.nn.loss.BinaryCrossEntropyLoss;
import com.linkedin.dagli.nn.loss.LossFunction;
import com.linkedin.dagli.nn.loss.LossFunctionVisitor;
import com.linkedin.dagli.nn.loss.MultinomialCrossEntropyLoss;
import com.linkedin.dagli.nn.loss.SquaredErrorLoss;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.ILossFunction;
import org.nd4j.linalg.lossfunctions.impl.LossBinaryXENT;
import org.nd4j.linalg.lossfunctions.impl.LossL1;
import org.nd4j.linalg.lossfunctions.impl.LossL2;
import org.nd4j.linalg.lossfunctions.impl.LossMCXENT;


public class LossFunctionConverterVisitor implements LossFunctionVisitor<ILossFunction> {
  private final long _inputWidth;

  /**
   * Creates a new instance that will convert loss functions from Dagli to DL4J objects.
   *
   * @param inputWidth the number of activations being targeted by the loss
   */
  public LossFunctionConverterVisitor(long inputWidth) {
    _inputWidth = inputWidth;
  }

  /**
   * @param lossFunction the loss function whose weight {@link INDArray} should be returned
   * @return the weight INDArray corresponding to a loss function, or null if it is not required (weight == 1)
   */
  private INDArray weights(LossFunction lossFunction) {
    return lossFunction.getWeight() == 1 ? null : Nd4j.valueArrayOf(_inputWidth, lossFunction.getWeight());
  }

  @Override
  public ILossFunction visit(SquaredErrorLoss visitee) {
    return new LossL2(weights(visitee));
  }

  @Override
  public ILossFunction visit(MultinomialCrossEntropyLoss visitee) {
    return new LossMCXENT(weights(visitee));
  }

  @Override
  public ILossFunction visit(BinaryCrossEntropyLoss visitee) {
    return new LossBinaryXENT(weights(visitee));
  }

  @Override
  public ILossFunction visit(AbsoluteErrorLoss visitee) {
    return new LossL1(weights(visitee));
  }
}
