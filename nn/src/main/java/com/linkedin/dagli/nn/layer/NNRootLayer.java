package com.linkedin.dagli.nn.layer;

import com.linkedin.dagli.annotation.Versioned;


/**
 * Base class for all root layers (nodes) in the neural network that have no parent layer(s).
 *
 * This class does not follow the usual "Abstract____" naming convention for abstract classes to allow for more readable
 * public APIs and to reflect the fact that it does not allow for clients to derive new implementations.
 *
 * @param <S> the ultimate (most derived) type deriving from this class
 */
@Versioned
public abstract class NNRootLayer<R, S extends NNRootLayer<R, S>> extends NNLayer<R, S> implements NonTerminalLayer {
  private static final long serialVersionUID = 1;

  @Override
  public InternalAPI internalAPI() {
    return new InternalAPI();
  }

  /**
   * Methods provided exclusively for use by Dagli.
   *
   * Client code should not use these methods as they are subject to change at any time.
   */
  public class InternalAPI extends NNLayer<R, S>.InternalAPI {
    InternalAPI() { }
  }
}
