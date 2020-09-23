package com.linkedin.dagli.nn.optimizer;

import com.linkedin.dagli.annotation.Versioned;
import java.io.Serializable;


/**
 * The base class for all optimizers (algorithms for updating model parameters during training to minimize the loss).
 *
 * Not all NN implementations will necessarily support all instantiations of all optimizers.
 *
 * Note that Dagli typically uses the naming scheme "Abstract..." for abstract base classes.  Here, because the class
 * is public, intended for typing public method parameters and return values, and not derivable outside its package,
 * a more "user friendly" name is used that reflects the fact that clients are not intended to derive from it.
 */
@Versioned
public abstract class Optimizer implements Serializable {
  private static final long serialVersionUID = 1;

  /**
   * Package-private constructor to ensure that derivatives of this base class may only be instantiated in this file.
   */
  Optimizer() { }

  /**
   * Accept a visitor to process an {@link Optimizer} instance.
   *
   * @param visitor the visitor to accept
   * @param <R> the type of result the visitor returns
   * @return the result of the visitor processing this instance
   */
  public abstract <R> R accept(OptimizerVisitor<R> visitor);
}
