package com.linkedin.dagli.dl4j;

import com.linkedin.dagli.nn.activation.ActivationFunctionVisitor;
import com.linkedin.dagli.nn.activation.HyperbolicTangent;
import com.linkedin.dagli.nn.activation.Identity;
import com.linkedin.dagli.nn.activation.RectifiedLinear;
import com.linkedin.dagli.nn.activation.Sigmoid;
import com.linkedin.dagli.nn.activation.SoftMax;
import org.nd4j.linalg.activations.IActivation;
import org.nd4j.linalg.activations.impl.ActivationIdentity;
import org.nd4j.linalg.activations.impl.ActivationReLU;
import org.nd4j.linalg.activations.impl.ActivationSigmoid;
import org.nd4j.linalg.activations.impl.ActivationSoftmax;
import org.nd4j.linalg.activations.impl.ActivationTanH;


/**
 * Converts a Dagli {@link com.linkedin.dagli.nn.activation.ActivationFunction} to a DL4J {@link IActivation}
 */
class ActivationFunctionConverterVisitor implements ActivationFunctionVisitor<IActivation> {
  @Override
  public IActivation visit(Sigmoid visitee) {
    return new ActivationSigmoid();
  }

  @Override
  public IActivation visit(RectifiedLinear visitee) {
    return new ActivationReLU();
  }

  @Override
  public IActivation visit(Identity visitee) {
    return new ActivationIdentity();
  }

  @Override
  public IActivation visit(SoftMax visitee) {
    return new ActivationSoftmax();
  }

  @Override
  public IActivation visit(HyperbolicTangent visitee) {
    return new ActivationTanH();
  }
}
