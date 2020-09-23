package com.linkedin.dagli.nn.loss;

import com.linkedin.dagli.annotation.Versioned;
import com.linkedin.dagli.annotation.struct.Optional;
import java.io.Serializable;


/**
 * The base class for all loss functions that evaluate the loss for a layer producing a vector of numbers.  Loss
 * functions are represented with objects rather than an enumeration to allow them to be parameterized.
 *
 * Not all NN implementations will necessarily support all instantiations of all loss functions.  A run-time exception
 * (rather than a silent logic error) will result if an unsupported loss function is requested.
 *
 * Note that Dagli typically uses the naming scheme "Abstract..." for abstract base classes.  Here, because the class
 * is public, intended for typing public method parameters and return values, and not derivable outside its package,
 * a more "user friendly" name is used that reflects the fact that clients are not intended to derive from it.
 */
@Versioned
public abstract class LossFunction implements Serializable {
  private static final long serialVersionUID = 1;

  /**
   * There can be multiple loss-incurring nodes in the network; the corresponding losses are multiplied by a weight and
   * summed together to get the total loss of the network.
   */
  @Optional // we have derived classes that are @Structs
  double _weight = 1.0;

  /**
   * There can be multiple loss-incurring nodes in the network; the corresponding losses are multiplied by a weight and
   * summed together to get the total loss of the network.
   *
   * @return the weight of this loss relative to other loss functions in the network.
   */
  public double getWeight() {
    return _weight;
  }

  /**
   * Package-private constructor to ensure that derivatives of this class are defined within our package.
   */
  LossFunction() { }

  public abstract <R> R accept(LossFunctionVisitor<R> visitor);
}
