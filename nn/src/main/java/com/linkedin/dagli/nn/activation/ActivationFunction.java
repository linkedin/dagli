package com.linkedin.dagli.nn.activation;

import com.linkedin.dagli.annotation.Versioned;
import java.io.Serializable;


/**
 * Base class for activation functions.  Activation functions are represented by objects rather than enumeration
 * elements to allow for parameterized activation functions (e.g. leaky ReLU).  All supported activation functions are
 * defined as inner classes of {@link ActivationFunction}.
 *
 * It cannot be guaranteed that all neural network implementations will support all activation functions with all
 * parameterizations, but (particularly for common choices, such as hyperbolic tangent) support can generally be
 * assumed.  More generally, a run-time exception will result when constructing the neural network if the specified
 * network is not realizable with the chosen underlying NN implementation (the result will not be a silent logic bug).
 *
 * Note that Dagli typically uses the naming scheme "Abstract..." for abstract base classes.  Here, because the class
 * is public, intended for typing public method parameters and return values, and not derivable outside its package,
 * a more "user friendly" name is used that reflects the fact that clients are not intended to derive from it.
 */
@Versioned
public abstract class ActivationFunction implements Serializable {
  private static final long serialVersionUID = 1;

  /**
   * Package-private constructor to ensure that all derivative classes are defined in this package.
   */
  ActivationFunction() { }

  /**
   * Accept a visitor to process an {@link ActivationFunction} instance.
   *
   * @param visitor the visitor to accept
   * @param <R> the type of result the visitor returns
   * @return the result of the visitor processing this instance
   */
  public abstract <R> R accept(ActivationFunctionVisitor<R> visitor);
}
