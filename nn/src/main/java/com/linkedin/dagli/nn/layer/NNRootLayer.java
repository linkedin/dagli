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
}
